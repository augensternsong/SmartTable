package com.example.form.controller;

import com.example.form.common.Result;
import com.example.form.dto.auth.LoginRequest;
import com.example.form.dto.auth.RefreshTokenRequest;
import com.example.form.dto.auth.TokenResponse;
import com.example.form.dto.auth.UserInfo;
import com.example.form.security.SecurityUser;
import com.example.form.security.SecurityUtils;
import com.example.form.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权接口.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@RequestBody @Valid RefreshTokenRequest req) {
        return Result.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7).trim();
        }
        // refresh token 由前端 body 传入更合适, 这里简化处理仅黑名单 access token
        authService.logout(accessToken, null);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserInfo> me() {
        SecurityUser su = SecurityUtils.current();
        return Result.ok(authService.currentUserInfo(su));
    }
}
