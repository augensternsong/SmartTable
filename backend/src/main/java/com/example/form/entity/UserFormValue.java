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
 * 用户填写当前值(一个用户一个栏位一条).
 */
@Data
@TableName("user_form_value")
public class UserFormValue implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    private String templateId;

    private String fieldId;

    /** 填写值, 多选存JSON数组字符串 */
    private String fieldValue;

    private LocalDateTime filledAt;

    /** 填写人ID(可能代填) */
    private String filledBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
