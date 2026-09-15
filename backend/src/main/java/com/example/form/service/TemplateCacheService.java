package com.example.form.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.form.common.Constants;
import com.example.form.dto.field.FieldVO;
import com.example.form.dto.template.TemplateDetailVO;
import com.example.form.entity.FormFieldOption;
import com.example.form.entity.FormTemplate;
import com.example.form.entity.FormTemplateField;
import com.example.form.entity.FormTemplateGroupAssign;
import com.example.form.mapper.FormFieldOptionMapper;
import com.example.form.mapper.FormTemplateFieldMapper;
import com.example.form.mapper.FormTemplateGroupAssignMapper;
import com.example.form.mapper.FormTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板结构缓存服务.
 *
 * <p>缓存"已发布模板"的结构(模板 + 生效栏位 + 选项 + 已分配分组),
 * 用户填写流程直接读缓存, 避免每次 join 多张表. 任何模板/栏位/选项/分组
 * 变更都通过 {@link #evictTemplate(String)} 失效缓存.
 *
 * <p>未发布模板(DRAFT/ARCHIVED)不缓存, 直接走 DB(管理员访问频率低).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateCacheService {

    private static final Duration TTL = Duration.ofHours(2);

    private final FormTemplateMapper templateMapper;
    private final FormTemplateFieldMapper fieldMapper;
    private final FormFieldOptionMapper optionMapper;
    private final FormTemplateGroupAssignMapper assignMapper;
    private final RedisTemplate<String, Object> redis;

    /**
     * 取模板完整结构(详情/用户填写都用此), 已发布走缓存, 其他状态走 DB.
     */
    @SuppressWarnings("unchecked")
    public TemplateDetailVO getTemplateDetail(String templateId) {
        FormTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            return null;
        }
        // 仅 PUBLISHED 走缓存
        if (Constants.TEMPLATE_PUBLISHED.equals(template.getStatus())) {
            String key = cacheKey(templateId);
            try {
                Object cached = redis.opsForValue().get(key);
                if (cached instanceof TemplateDetailVO) {
                    return (TemplateDetailVO) cached;
                }
            } catch (Exception e) {
                log.warn("读取模板缓存失败, 回源 DB: {}", e.getMessage());
            }
            TemplateDetailVO detail = buildDetailFromDb(template);
            try {
                redis.opsForValue().set(key, detail, TTL);
            } catch (Exception e) {
                log.warn("写入模板缓存失败: {}", e.getMessage());
            }
            return detail;
        }
        // 非已发布模板不走缓存, 但只返回 ACTIVE 栏位
        return buildDetailFromDb(template);
    }

    /**
     * 仅查已发布模板的有效栏位列表(用户填写视图用, 极简版).
     */
    @SuppressWarnings("unchecked")
    public List<FieldVO> getPublishedFields(String templateId) {
        TemplateDetailVO detail = getTemplateDetail(templateId);
        if (detail == null) {
            return Collections.emptyList();
        }
        return detail.getFields() == null ? Collections.emptyList() : detail.getFields();
    }

    /**
     * 失效指定模板缓存. 任何模板/栏位/选项/分组变更后调用.
     */
    public void evictTemplate(String templateId) {
        if (!StringUtils.hasText(templateId)) {
            return;
        }
        try {
            redis.delete(cacheKey(templateId));
        } catch (Exception e) {
            log.warn("删除模板缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 失效全部模板缓存(用于批量重置等场景).
     */
    public void evictAll() {
        try {
            redis.delete(redis.keys(Constants.CACHE_TEMPLATE + "*"));
        } catch (Exception e) {
            log.warn("批量删除模板缓存失败: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------

    private String cacheKey(String templateId) {
        return Constants.CACHE_TEMPLATE + templateId;
    }

    private TemplateDetailVO buildDetailFromDb(FormTemplate template) {
        List<FormTemplateField> fields = fieldMapper.selectList(
                new LambdaQueryWrapper<FormTemplateField>()
                        .eq(FormTemplateField::getTemplateId, template.getId())
                        .eq(FormTemplateField::getStatus, Constants.FIELD_ACTIVE)
                        .orderByAsc(FormTemplateField::getSortOrder)
                        .orderByAsc(FormTemplateField::getCreatedAt));

        List<FieldVO> fieldVOs = fields.stream().map(this::toFieldVO).collect(Collectors.toList());

        List<String> groupIds = assignMapper.selectList(
                        new LambdaQueryWrapper<FormTemplateGroupAssign>()
                                .eq(FormTemplateGroupAssign::getTemplateId, template.getId()))
                .stream().map(FormTemplateGroupAssign::getGroupId).collect(Collectors.toList());

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
                .fields(fieldVOs)
                .build();
    }

    private FieldVO toFieldVO(FormTemplateField f) {
        List<com.example.form.dto.field.OptionVO> optVOs = new ArrayList<>();
        if (f.getFieldType() != null && (
                "SELECT_SINGLE".equals(f.getFieldType()) || "SELECT_MULTI".equals(f.getFieldType()))) {
            List<FormFieldOption> opts = optionMapper.selectList(
                    new LambdaQueryWrapper<FormFieldOption>()
                            .eq(FormFieldOption::getFieldId, f.getId())
                            .eq(FormFieldOption::getStatus, Constants.STATUS_ENABLED)
                            .orderByAsc(FormFieldOption::getSortOrder));
            optVOs = opts.stream().map(o -> com.example.form.dto.field.OptionVO.builder()
                            .id(o.getId())
                            .fieldId(o.getFieldId())
                            .optionValue(o.getOptionValue())
                            .optionLabel(o.getOptionLabel())
                            .sortOrder(o.getSortOrder())
                            .status(o.getStatus())
                            .build())
                    .collect(Collectors.toList());
        }
        return com.example.form.dto.field.FieldVO.builder()
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
                .options(optVOs)
                .build();
    }
}
