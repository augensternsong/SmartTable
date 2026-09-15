package com.example.form.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.form.common.Constants;
import com.example.form.common.FieldType;
import com.example.form.common.exception.BusinessException;
import com.example.form.dto.field.FieldSaveRequest;
import com.example.form.dto.field.FieldVO;
import com.example.form.dto.field.OptionSaveRequest;
import com.example.form.dto.field.OptionVO;
import com.example.form.entity.FormFieldOption;
import com.example.form.entity.FormTemplate;
import com.example.form.entity.FormTemplateField;
import com.example.form.mapper.FormFieldOptionMapper;
import com.example.form.mapper.FormTemplateFieldMapper;
import com.example.form.mapper.FormTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板栏位 + 选项管理服务.
 *
 * <p>软版本策略:
 * <ul>
 *   <li>新增栏位: status=ACTIVE, version=当前模板版本</li>
 *   <li>修改栏位: 原地更新(name/rules/options); 用户数据按 field_id 绑定, 不受影响</li>
 *   <li>停用栏位: status=INACTIVE, 不物理删除; 用户已有填写值仍保留但不展示</li>
 *   <li>启用栏位: status=ACTIVE</li>
 *   <li>field_code 全生命周期稳定, 用户数据按此绑定</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FormTemplateFieldService {

    private final FormTemplateFieldMapper fieldMapper;
    private final FormFieldOptionMapper optionMapper;
    private final FormTemplateMapper templateMapper;
    private final TemplateCacheService cacheService;

    public List<FieldVO> listByTemplate(String templateId) {
        List<FormTemplateField> fields = fieldMapper.selectList(
                new LambdaQueryWrapper<FormTemplateField>()
                        .eq(FormTemplateField::getTemplateId, templateId)
                        .orderByAsc(FormTemplateField::getSortOrder)
                        .orderByAsc(FormTemplateField::getCreatedAt));
        return fields.stream().map(this::toVO).collect(Collectors.toList());
    }

    public FieldVO getById(String id) {
        FormTemplateField field = fieldMapper.selectById(id);
        if (field == null) {
            throw new BusinessException("栏位不存在");
        }
        return toVO(field);
    }

    @Transactional(rollbackFor = Exception.class)
    public String save(String templateId, FieldSaveRequest req) {
        FormTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        // 仅草稿/已发布模板可编辑栏位(归档不可编辑)
        if (Constants.TEMPLATE_ARCHIVED.equals(template.getStatus())) {
            throw new BusinessException("已归档模板不可修改栏位");
        }

        // 校验类型枚举
        FieldType type;
        try {
            type = FieldType.valueOf(req.getFieldType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的栏位类型: " + req.getFieldType());
        }

        // 选择类栏位必须有选项
        if (type.isSelectType() && CollectionUtils.isEmpty(req.getOptions())) {
            throw new BusinessException("选择类栏位必须配置至少一个选项");
        }
        if (!type.isSelectType() && !CollectionUtils.isEmpty(req.getOptions())) {
            throw new BusinessException("非选择类栏位不应配置选项");
        }

        boolean isNew = !StringUtils.hasText(req.getId());
        // 校验栏位编码在模板内唯一
        Long codeExists = fieldMapper.selectCount(new LambdaQueryWrapper<FormTemplateField>()
                .eq(FormTemplateField::getTemplateId, templateId)
                .eq(FormTemplateField::getFieldCode, req.getFieldCode())
                .ne(!isNew, FormTemplateField::getId, req.getId()));
        if (codeExists != null && codeExists > 0) {
            throw new BusinessException("栏位编码在模板内已存在");
        }

        FormTemplateField field;
        if (isNew) {
            field = new FormTemplateField();
            field.setTemplateId(templateId);
            field.setFieldCode(req.getFieldCode());
            field.setStatus(Constants.FIELD_ACTIVE);
            field.setVersion(template.getVersion());
        } else {
            field = fieldMapper.selectById(req.getId());
            if (field == null) {
                throw new BusinessException("栏位不存在");
            }
            if (!field.getTemplateId().equals(templateId)) {
                throw new BusinessException("栏位不属于该模板");
            }
            // 编码不可修改(用户数据按 field_code 绑定)
            if (!field.getFieldCode().equals(req.getFieldCode())) {
                throw new BusinessException("栏位编码不可修改");
            }
        }
        field.setFieldName(req.getFieldName());
        field.setFieldType(req.getFieldType());
        field.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        field.setRequired(req.getRequired() == null ? 0 : req.getRequired());
        field.setMaxLength(req.getMaxLength());
        field.setMinValue(req.getMinValue());
        field.setMaxValue(req.getMaxValue());
        field.setRegexPattern(req.getRegexPattern());
        field.setFillCycleDays(req.getFillCycleDays());
        field.setPlaceholder(req.getPlaceholder());
        field.setDescription(req.getDescription());

        if (isNew) {
            fieldMapper.insert(field);
        } else {
            fieldMapper.updateById(field);
        }

        // 选项同步(选择类栏位)
        if (type.isSelectType()) {
            syncOptions(field.getId(), req.getOptions());
        }

        cacheService.evictTemplate(templateId);
        return field.getId();
    }

    /**
     * 停用栏位(软删除).
     */
    @Transactional(rollbackFor = Exception.class)
    public void deactivate(String templateId, String fieldId) {
        FormTemplateField field = fieldMapper.selectById(fieldId);
        if (field == null || !field.getTemplateId().equals(templateId)) {
            throw new BusinessException("栏位不存在");
        }
        field.setStatus(Constants.FIELD_INACTIVE);
        fieldMapper.updateById(field);
        cacheService.evictTemplate(templateId);
    }

    /**
     * 启用栏位(从软删除恢复).
     */
    @Transactional(rollbackFor = Exception.class)
    public void activate(String templateId, String fieldId) {
        FormTemplateField field = fieldMapper.selectById(fieldId);
        if (field == null || !field.getTemplateId().equals(templateId)) {
            throw new BusinessException("栏位不存在");
        }
        field.setStatus(Constants.FIELD_ACTIVE);
        fieldMapper.updateById(field);
        cacheService.evictTemplate(templateId);
    }

    // ------------------------------------------------------------
    // options
    // ------------------------------------------------------------

    /**
     * 全量覆盖选项: 删除前端未带的旧选项, 新增/更新前端带的选项.
     */
    private void syncOptions(String fieldId, List<OptionSaveRequest> options) {
        List<FormFieldOption> existing = optionMapper.selectList(
                new LambdaQueryWrapper<FormFieldOption>().eq(FormFieldOption::getFieldId, fieldId));
        // 已存在 -> 按 id 索引, 用于增量更新
        var existingById = existing.stream()
                .collect(Collectors.toMap(FormFieldOption::getId, o -> o));

        for (OptionSaveRequest o : options) {
            if (StringUtils.hasText(o.getId()) && existingById.containsKey(o.getId())) {
                FormFieldOption opt = existingById.get(o.getId());
                opt.setOptionValue(o.getOptionValue());
                opt.setOptionLabel(o.getOptionLabel());
                opt.setSortOrder(o.getSortOrder() == null ? 0 : o.getSortOrder());
                opt.setStatus(o.getStatus() == null ? 1 : o.getStatus());
                optionMapper.updateById(opt);
                existingById.remove(o.getId());
            } else {
                FormFieldOption opt = new FormFieldOption();
                opt.setFieldId(fieldId);
                opt.setOptionValue(o.getOptionValue());
                opt.setOptionLabel(o.getOptionLabel());
                opt.setSortOrder(o.getSortOrder() == null ? 0 : o.getSortOrder());
                opt.setStatus(o.getStatus() == null ? 1 : o.getStatus());
                optionMapper.insert(opt);
            }
        }
        // 剩余的 existingById 是前端删除的 -> 物理删除
        for (FormFieldOption opt : existingById.values()) {
            optionMapper.deleteById(opt.getId());
        }
    }

    private FieldVO toVO(FormTemplateField field) {
        List<OptionVO> options = new ArrayList<>();
        if (FieldType.valueOf(field.getFieldType()).isSelectType()) {
            List<FormFieldOption> opts = optionMapper.selectList(
                    new LambdaQueryWrapper<FormFieldOption>()
                            .eq(FormFieldOption::getFieldId, field.getId())
                            .orderByAsc(FormFieldOption::getSortOrder));
            options = opts.stream().map(this::toOptionVO).collect(Collectors.toList());
        }
        return FieldVO.builder()
                .id(field.getId())
                .templateId(field.getTemplateId())
                .fieldCode(field.getFieldCode())
                .fieldName(field.getFieldName())
                .fieldType(field.getFieldType())
                .sortOrder(field.getSortOrder())
                .required(field.getRequired())
                .maxLength(field.getMaxLength())
                .minValue(field.getMinValue())
                .maxValue(field.getMaxValue())
                .regexPattern(field.getRegexPattern())
                .fillCycleDays(field.getFillCycleDays())
                .placeholder(field.getPlaceholder())
                .description(field.getDescription())
                .status(field.getStatus())
                .version(field.getVersion())
                .createdAt(field.getCreatedAt())
                .updatedAt(field.getUpdatedAt())
                .options(options)
                .build();
    }

    private OptionVO toOptionVO(FormFieldOption opt) {
        return OptionVO.builder()
                .id(opt.getId())
                .fieldId(opt.getFieldId())
                .optionValue(opt.getOptionValue())
                .optionLabel(opt.getOptionLabel())
                .sortOrder(opt.getSortOrder())
                .status(opt.getStatus())
                .build();
    }
}
