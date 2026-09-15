package com.example.form.security;

import com.example.form.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具: 获取当前登录用户.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录的 SecurityUser, 未登录抛业务异常.
     */
    public static SecurityUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser su)) {
            throw new BusinessException(401, "未登录");
        }
        return su;
    }

    /**
     * 获取当前用户ID.
     */
    public static String currentUserId() {
        return current().getUserId();
    }

    /**
     * 当前用户是否为超级管理员.
     */
    public static boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser su)) {
            return false;
        }
        return su.isSuperAdmin();
    }
}
