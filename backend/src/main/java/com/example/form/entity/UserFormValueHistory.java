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
 * 用户填写历史(全量记录).
 */
@Data
@TableName("user_form_value_history")
public class UserFormValueHistory implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private String templateId;

    private String fieldId;

    private String fieldValue;

    private LocalDateTime filledAt;

    private String filledBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
