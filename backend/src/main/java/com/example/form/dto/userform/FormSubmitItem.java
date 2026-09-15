package com.example.form.dto.userform;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 单个栏位填写项.
 */
@Data
public class FormSubmitItem {
    @NotBlank(message = "fieldId 不能为空")
    private String fieldId;

    /** 填写值; 多选时传 JSON 数组字符串如 ["a","b"]; 空字符串/视作清空 */
    private String value;
}
