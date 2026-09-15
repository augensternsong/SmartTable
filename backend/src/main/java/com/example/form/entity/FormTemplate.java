package com.example.form.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 表单模板.
 */
@Data
@TableName("form_template")
public class FormTemplate implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String templateCode;

    private String templateName;

    private String description;

    /** DRAFT / PUBLISHED / ARCHIVED */
    private String status;

    private Integer version;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
