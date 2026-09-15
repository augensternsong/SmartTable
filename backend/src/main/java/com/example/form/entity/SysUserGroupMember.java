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
 * 用户-分组关联.
 */
@Data
@TableName("sys_user_group_member")
public class SysUserGroupMember implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private String groupId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
