package com.example.form.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前登录用户信息(供前端初始化菜单和按钮权限).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    /** 角色编码列表 */
    private List<String> roles;
    /** 权限编码列表(按钮/API粒度) */
    private List<String> permissions;
    /** 可见菜单树 */
    private List<MenuNode> menus;
}
