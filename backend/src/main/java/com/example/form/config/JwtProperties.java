package com.example.form.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 相关配置.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "form.jwt")
public class JwtProperties {
    /** 签名密钥(Base64) */
    private String secret;
    /** access token 有效期(秒) */
    private long accessTokenTtl = 7200L;
    /** refresh token 有效期(秒) */
    private long refreshTokenTtl = 604800L;
    /** header 名 */
    private String header = "Authorization";
    /** token 前缀 */
    private String prefix = "Bearer ";
}
