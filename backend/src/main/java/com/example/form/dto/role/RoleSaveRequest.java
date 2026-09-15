package com.example.form.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色保存请求.
 */
@Data
public class RoleSaveRequest {
    private String id;

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "角色编码必须大写字母开头, 字母数字下划线, 长度2-64")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64")
    private String roleName;

    @Size(max = 255, message = "描述长度不能超过255")
    private String description;

    private Integer status;
}
