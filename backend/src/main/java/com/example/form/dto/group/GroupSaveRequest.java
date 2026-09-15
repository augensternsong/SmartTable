package com.example.form.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户分组保存请求.
 */
@Data
public class GroupSaveRequest {
    private String id;

    @NotBlank(message = "分组编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]{1,63}$", message = "分组编码字母开头, 字母数字下划线连字符, 长度2-64")
    private String groupCode;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 64, message = "分组名称长度不能超过64")
    private String groupName;

    @Size(max = 255, message = "描述长度不能超过255")
    private String description;

    private Integer status;
}
