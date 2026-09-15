package com.example.form.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 token 请求.
 */
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
