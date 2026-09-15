package com.example.form.dto.template;

import lombok.Data;

@Data
public class TemplatePageRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private String status;
}
