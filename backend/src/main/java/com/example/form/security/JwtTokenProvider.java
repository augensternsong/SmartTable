package com.example.form.security;

import com.example.form.common.Constants;
import com.example.form.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具: 生成 / 解析 / 校验 / 黑名单(走 Redis).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final StringRedisTemplate redis;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props, StringRedisTemplate redis) {
        this.props = props;
        this.redis = redis;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** access token 类型 */
    public static final String TYPE_ACCESS = "access";
    /** refresh token 类型 */
    public static final String TYPE_REFRESH = "refresh";

    /**
     * 生成 token.
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param type     类型 access / refresh
     */
    public String generate(String userId, String username, String type) {
        long ttlSec = TYPE_REFRESH.equals(type) ? props.getRefreshTokenTtl() : props.getAccessTokenTtl();
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlSec * 1000L);

        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("usr", username);
        claims.put("typ", type);

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String generateAccessToken(String userId, String username) {
        return generate(userId, username, TYPE_ACCESS);
    }

    public String generateRefreshToken(String userId, String username) {
        return generate(userId, username, TYPE_REFRESH);
    }

    /**
     * 解析 token, 校验签名与过期时间.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 token 是否有效(签名 + 未过期 + 未被吊销).
     */
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            parse(token);
            return !isBlacklisted(token);
        } catch (JwtException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 把 token 加入黑名单(在 token 自身过期之前不可用). 用于登出.
     */
    public void blacklist(String token) {
        try {
            Claims claims = parse(token);
            long expMs = claims.getExpiration().getTime();
            long ttlSec = (expMs - System.currentTimeMillis()) / 1000;
            if (ttlSec > 0) {
                redis.opsForValue().set(blacklistKey(token), "1", Duration.ofSeconds(ttlSec));
            }
        } catch (JwtException e) {
            // 解析失败说明 token 已失效, 无需加入黑名单
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redis.hasKey(blacklistKey(token)));
    }

    private String blacklistKey(String token) {
        return Constants.JWT_BLACKLIST + token;
    }

    public JwtProperties getProps() {
        return props;
    }
}
