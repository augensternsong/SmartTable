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
 * 模板-分组分配.
 */
@Data
@TableName("form_template_group_assign")
public class FormTemplateGroupAssign implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String templateId;

    private String groupId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
