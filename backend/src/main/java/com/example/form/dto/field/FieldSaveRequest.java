package com.example.form.dto.field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 栏位保存请求. 选择类栏位(SELECT_SINGLE/SELECT_MULTI) 必须带 options.
 */
@Data
public class FieldSaveRequest {
    private String id;

    @NotBlank(message = "栏位编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{1,63}$",
            message = "栏位编码字母开头, 字母数字下划线, 长度2-64")
    private String fieldCode;

    @NotBlank(message = "栏位名称不能为空")
    @Size(max = 128, message = "栏位名称长度不能超过128")
    private String fieldName;

    @NotBlank(message = "栏位类型不能为空")
    private String fieldType;

    private Integer sortOrder;

    private Integer required;

    @Positive(message = "最大长度必须为正数")
    private Integer maxLength;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    @Size(max = 512, message = "正则表达式长度不能超过512")
    private String regexPattern;

    @Positive(message = "填写周期必须为正数")
    private Integer fillCycleDays;

    @Size(max = 255, message = "placeholder长度不能超过255")
    private String placeholder;

    @Size(max = 512, message = "描述长度不能超过512")
    private String description;

    /** 选择类栏位的选项列表(其余类型忽略) */
    private List<OptionSaveRequest> options;
}
