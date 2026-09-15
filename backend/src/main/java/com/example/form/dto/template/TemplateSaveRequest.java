package com.example.form.dto.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 模板保存请求.
 */
@Data
public class TemplateSaveRequest {
    private String id;

    @NotBlank(message = "模板编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]{1,63}$",
            message = "模板编码字母开头, 字母数字下划线连字符, 长度2-64")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称长度不能超过128")
    private String templateName;

    @Size(max = 512, message = "描述长度不能超过512")
    private String description;
}
