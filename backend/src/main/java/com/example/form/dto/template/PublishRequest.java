package com.example.form.dto.template;

import lombok.Data;

/**
 * 模板发布请求(预留: 可附加发布说明等).
 */
@Data
public class PublishRequest {
    /** 发布说明(可选) */
    private String note;
}
