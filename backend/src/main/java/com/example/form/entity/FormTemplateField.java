package com.example.form.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模板栏位(软版本: status标志, 不物理删除).
 */
@Data
@TableName("form_template_field")
public class FormTemplateField implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String templateId;

    /** 栏位编码, 软版本下保持稳定, 用户数据按此绑定 */
    private String fieldCode;

    private String fieldName;

    /** TEXT/TEXTAREA/NUMBER/DATE/DATETIME/SELECT_SINGLE/SELECT_MULTI */
    private String fieldType;

    private Integer sortOrder;

    private Integer required;

    private Integer maxLength;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    private String regexPattern;

    /** 填写周期天数, NULL表示无周期 */
    private Integer fillCycleDays;

    private String placeholder;

    private String description;

    /** ACTIVE / INACTIVE */
    private String status;

    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
