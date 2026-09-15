package com.example.form.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.form.common.Constants;
import com.example.form.common.PageResult;
import com.example.form.common.exception.BusinessException;
import com.example.form.dto.template.AssignTemplateGroupsRequest;
import com.example.form.dto.template.TemplateDetailVO;
import com.example.form.dto.template.TemplatePageRequest;
import com.example.form.dto.template.TemplateSaveRequest;
import com.example.form.dto.template.TemplateVO;
import com.example.form.entity.FormTemplate;
import com.example.form.entity.FormTemplateField;
import com.example.form.entity.FormTemplateGroupAssign;
import com.example.form.mapper.FormTemplateFieldMapper;
import com.example.form.mapper.FormTemplateGroupAssignMapper;
import com.example.form.mapper.FormTemplateMapper;
import com.example.form.mapper.SysUserMapper;
import com.example.form.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 表单模板管理服务.
 */
@Service
@RequiredArgsConstructor
public class FormTemplateService {

    private final FormTemplateMapper templateMapper;
    private final FormTemplateFieldMapper fieldMapper;
    private final FormTemplateGroupAssignMapper assignMapper;
    private final SysUserMapper sysUserMapper;
    private final TemplateCacheService cacheService;

    public PageResult<TemplateVO> page(TemplatePageRequest req) {
        LambdaQueryWrapper<FormTemplate> wrapper = new LambdaQueryWrapper<FormTemplate>()
                .like(StringUtils.hasText(req.getKeyword()), FormTemplate::getTemplateName, req.getKeyword())
                .or()
                .like(StringUtils.hasText(req.getKeyword()), FormTemplate::getTemplateCode, req.getKeyword())
                .eq(StringUtils.hasText(req.getStatus()), FormTemplate::getStatus, req.getStatus())
                .orderByDesc(FormTemplate::getCreatedAt);
        Page<FormTemplate> p = templateMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);

        List<TemplateVO> records = p.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), records);
    }

    /**
     * 模板详情: 含全部栏位(含已停用) + 已分配分组. 管理端使用.
     * 注意: 不走缓存(因为含 INACTIVE 栏位, 是管理员视角).
     */
    public TemplateDetailVO getDetail(String id) {
        FormTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        TemplateDetailVO detail = cacheService.getTemplateDetail(id);
        if (detail != null && Constants.TEMPLATE_PUBLISHED.equals(template.getStatus())) {
            // 缓存的 detail 只含 ACTIVE 栏位, 管理端需要全部, 重新组装
        }
        return buildDetailAdmin(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public String save(TemplateSaveRequest req) {
        boolean isNew = !StringUtils.hasText(req.getId());
        // 编码唯一性
        Long exists = templateMapper.selectCount(new LambdaQueryWrapper<FormTemplate>()
                .eq(FormTemplate::getTemplateCode, req.getTemplateCode())
                .ne(!isNew, FormTemplate::getId, req.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("模板编码已存在");
        }
        FormTemplate template;
        if (isNew) {
            template = new FormTemplate();
            template.setTemplateCode(req.getTemplateCode());
            template.setStatus(Constants.TEMPLATE_DRAFT);
            template.setVersion(1);
            template.setCreatedBy(SecurityUtils.currentUserId());
        } else {
            template = templateMapper.selectById(req.getId());
            if (template == null) {
                throw new BusinessException("模板不存在");
            }
            if (!template.getTemplateCode().equals(req.getTemplateCode())) {
                throw new BusinessException("模板编码不可修改");
            }
            if (Constants.TEMPLATE_ARCHIVED.equals(template.getStatus())) {
                throw new BusinessException("已归档模板不可修改");
            }
        }
        template.setTemplateName(req.getTemplateName());
        template.setDescription(req.getDescription());
        if (isNew) {
            templateMapper.insert(template);
        } else {
            templateMapper.updateById(template);
        }
        cacheService.evictTemplate(template.getId());
        return template.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        FormTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        // 仅草稿可物理删除; 已发布/已归档需先归档(此处简化: 已发布不可直接删除)
        if (!Constants.TEMPLATE_DRAFT.equals(template.getStatus())) {
            throw new BusinessException("仅草稿状态模板可删除, 已发布模板请改用归档");
        }
        // 关联栏位/选项/分配一并清理
        List<FormTemplateField> fields = fieldMapper.selectList(
                new LambdaQueryWrapper<FormTemplateField>().eq(FormTemplateField::getTemplateId, id));
        // 选项级联删除由 SQL 外键? 无, 这里手动删(简化: 仅清栏位, 选项留下孤儿由后台清理)
        fieldMapper.delete(new LambdaQueryWrapper<FormTemplateField>().eq(FormTemplateField::getTemplateId, id));
        assignMapper.delete(new LambdaQueryWrapper<FormTemplateGroupAssign>()
                .eq(FormTemplateGroupAssign::getTemplateId, id));
        templateMapper.deleteById(id);
        cacheService.evictTemplate(id);
    }

    /**
     * 发布模板: DRAFT/ARCHIVED -> PUBLISHED, version+1.
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(String id) {
        FormTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (Constants.TEMPLATE_PUBLISHED.equals(template.getStatus())) {
            throw new BusinessException("模板已是已发布状态");
        }
        // 校验至少有一个 ACTIVE 栏位
        Long activeFields = fieldMapper.selectCount(new LambdaQueryWrapper<FormTemplateField>()
                .eq(FormTemplateField::getTemplateId, id)
                .eq(FormTemplateField::getStatus, Constants.FIELD_ACTIVE));
        if (activeFields == null || activeFields == 0) {
            throw new BusinessException("模板至少需要一个生效栏位才能发布");
        }
        template.setStatus(Constants.TEMPLATE_PUBLISHED);
        template.setVersion(template.getVersion() + 1);
        templateMapper.updateById(template);
        cacheService.evictTemplate(id);
    }

    /**
     * 归档模板: PUBLISHED -> ARCHIVED.
     */
    @Transactional(rollbackFor = Exception.class)
    public void archive(String id) {
        FormTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (!Constants.TEMPLATE_PUBLISHED.equals(template.getStatus())) {
            throw new BusinessException("仅已发布模板可归档");
        }
        template.setStatus(Constants.TEMPLATE_ARCHIVED);
        templateMapper.updateById(template);
        cacheService.evictTemplate(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignGroups(String id, AssignTemplateGroupsRequest req) {
        FormTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        assignMapper.delete(new LambdaQueryWrapper<FormTemplateGroupAssign>()
                .eq(FormTemplateGroupAssign::getTemplateId, id));
        if (req.getGroupIds() != null) {
            for (String gid : req.getGroupIds()) {
                FormTemplateGroupAssign a = new FormTemplateGroupAssign();
                a.setTemplateId(id);
                a.setGroupId(gid);
                assignMapper.insert(a);
            }
        }
        cacheService.evictTemplate(id);
    }

    /**
     * 当前登录用户可见的已发布模板(按其所在分组过滤).
     * 超管可见全部已发布模板.
     */
    public List<TemplateVO> listForCurrentUser() {
        if (SecurityUtils.isSuperAdmin()) {
            List<FormTemplate> all = templateMapper.selectList(
                    new LambdaQueryWrapper<FormTemplate>()
                            .eq(FormTemplate::getStatus, Constants.TEMPLATE_PUBLISHED)
                            .orderByDesc(FormTemplate::getUpdatedAt));
            return all.stream().map(this::toListVO).collect(Collectors.toList());
        }
        String uid = SecurityUtils.currentUserId();
        List<String> myGroupIds = sysUserMapper.selectGroupIds(uid);
        if (myGroupIds == null || myGroupIds.isEmpty()) {
            return List.of();
        }
        // 查询分配给这些分组的模板ID
        List<String> templateIds = assignMapper.selectList(
                        new LambdaQueryWrapper<FormTemplateGroupAssign>()
                                .in(FormTemplateGroupAssign::getGroupId, myGroupIds))
                .stream().map(FormTemplateGroupAssign::getTemplateId).distinct().collect(Collectors.toList());
        if (templateIds.isEmpty()) {
            return List.of();
        }
        List<FormTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<FormTemplate>()
                        .in(FormTemplate::getId, templateIds)
                        .eq(FormTemplate::getStatus, Constants.TEMPLATE_PUBLISHED)
                        .orderByDesc(FormTemplate::getUpdatedAt));
        return templates.stream().map(this::toListVO).collect(Collectors.toList());
    }

    // ------------------------------------------------------------

    private TemplateVO toListVO(FormTemplate t) {
        Long fieldCount = fieldMapper.selectCount(new LambdaQueryWrapper<FormTemplateField>()
                .eq(FormTemplateField::getTemplateId, t.getId()));
        Long groupCount = assignMapper.selectCount(new LambdaQueryWrapper<FormTemplateGroupAssign>()
                .eq(FormTemplateGroupAssign::getTemplateId, t.getId()));
        return TemplateVO.builder()
                .id(t.getId())
                .templateCode(t.getTemplateCode())
                .templateName(t.getTemplateName())
                .description(t.getDescription())
                .status(t.getStatus())
                .version(t.getVersion())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .fieldCount(fieldCount == null ? 0L : fieldCount)
                .assignedGroupCount(groupCount == null ? 0L : groupCount)
                .build();
    }

    private TemplateDetailVO buildDetailAdmin(FormTemplate template) {
        // 管理端详情: 含全部栏位(含已停用) + 已分配分组; 不走缓存
        List<FormTemplateField> fields = fieldMapper.selectList(
                new LambdaQueryWrapper<FormTemplateField>()
                        .eq(FormTemplateField::getTemplateId, template.getId())
                        .orderByAsc(FormTemplateField::getSortOrder)
                        .orderByAsc(FormTemplateField::getCreatedAt));
        List<String> groupIds = assignMapper.selectList(
                        new LambdaQueryWrapper<FormTemplateGroupAssign>()
                                .eq(FormTemplateGroupAssign::getTemplateId, template.getId()))
                .stream().map(FormTemplateGroupAssign::getGroupId).collect(Collectors.toList());

        // 此处 fields 直接用 FormTemplateField 实体返回前端(管理端简单可用)
        // 选项需要单独通过栏位 controller 的 getById 获取
        return TemplateDetailVO.builder()
                .id(template.getId())
                .templateCode(template.getTemplateCode())
                .templateName(template.getTemplateName())
                .description(template.getDescription())
                .status(template.getStatus())
                .version(template.getVersion())
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .assignedGroupIds(groupIds)
                .fields(fields.stream().map(f -> com.example.form.dto.field.FieldVO.builder()
                        .id(f.getId())
                        .templateId(f.getTemplateId())
                        .fieldCode(f.getFieldCode())
                        .fieldName(f.getFieldName())
                        .fieldType(f.getFieldType())
                        .sortOrder(f.getSortOrder())
                        .required(f.getRequired())
                        .maxLength(f.getMaxLength())
                        .minValue(f.getMinValue())
                        .maxValue(f.getMaxValue())
                        .regexPattern(f.getRegexPattern())
                        .fillCycleDays(f.getFillCycleDays())
                        .placeholder(f.getPlaceholder())
                        .description(f.getDescription())
                        .status(f.getStatus())
                        .version(f.getVersion())
                        .createdAt(f.getCreatedAt())
                        .updatedAt(f.getUpdatedAt())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
