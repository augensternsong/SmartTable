package com.example.form.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户保存请求(新建/编辑共用, id 为空表示新建).
 */
@Data
public class UserSaveRequest {

    /** 编辑时传入, 新建时为空 */
    private String id;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "用户名仅支持字母数字下划线点连字符")
    private String username;

    @Size(max = 64, message = "昵称长度不能超过64")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @Size(max = 32, message = "手机号长度不能超过32")
    private String phone;

    /** 1启用 0禁用 */
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    /** 新建时必填的初始密码; 编辑时为空表示不改密码 */
    @Size(min = 6, max = 32, message = "密码长度6-32")
    private String password;
}
