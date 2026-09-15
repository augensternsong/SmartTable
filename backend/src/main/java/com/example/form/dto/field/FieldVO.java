package com.example.form.dto.field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 栏位视图(含选项). 使用 @SuperBuilder 以便用户填写视图继承扩展.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FieldVO {
    private String id;
    private String templateId;
    private String fieldCode;
    private String fieldName;
    private String fieldType;
    private Integer sortOrder;
    private Integer required;
    private Integer maxLength;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String regexPattern;
    private Integer fillCycleDays;
    private String placeholder;
    private String description;
    private String status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 选择类栏位的选项 */
    private List<OptionVO> options;
}
