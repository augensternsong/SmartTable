package com.example.form.dto.userform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 表单提交请求.
 */
@Data
public class FormSubmitRequest {
    @NotEmpty(message = "提交内容不能为空")
    @Valid
    private List<FormSubmitItem> items;
}
