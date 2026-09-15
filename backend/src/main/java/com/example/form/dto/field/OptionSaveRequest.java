package com.example.form.dto.field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 选项保存请求.
 */
@Data
public class OptionSaveRequest {
    private String id;

    @NotBlank(message = "选项值不能为空")
    @Size(max = 128, message = "选项值长度不能超过128")
    private String optionValue;

    @NotBlank(message = "选项标签不能为空")
    @Size(max = 128, message = "选项标签长度不能超过128")
    private String optionLabel;

    private Integer sortOrder;
    private Integer status;
}
