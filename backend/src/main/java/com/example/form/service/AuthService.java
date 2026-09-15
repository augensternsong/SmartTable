package com.example.form.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.form.config.JwtProperties;
import com.example.form.dto.auth.LoginRequest;
import com.example.form.dto.auth.MenuNode;
import com.example.form.dto.auth.RefreshTokenRequest;
import com.example.form.dto.auth.TokenResponse;
import com.example.form.dto.auth.UserInfo;
import com.example.form.entity.SysPermission;
import com.example.form.mapper.SysPermissionMapper;
import com.example.form.mapper.SysUserMapper;
import com.example.form.security.JwtTokenProvider;
import com.example.form.security.SecurityUser;
import com.example.form.security.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 鉴权服务: 登录 / 刷新 / 登出 / 当前用户信息.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtProperties jwtProperties;

    public TokenResponse login(LoginRequest req) {
        SecurityUser su = (SecurityUser) userDetailsService.loadUserByUsername(req.getUsername());
        if (!passwordEncoder.matches(req.getPassword(), su.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        return buildToken(su);
    }

    public TokenResponse refresh(RefreshTokenRequest req) {
        if (!tokenProvider.isValid(req.getRefreshToken())) {
            throw new BadCredentialsException("refresh token 无效或已过期");
        }
        Claims claims = tokenProvider.parse(req.getRefreshToken());
        if (!JwtTokenProvider.TYPE_REFRESH.equals(claims.get("typ", String.class))) {
            throw new BadCredentialsException("token 类型不正确");
        }
        String userId = claims.getSubject();
        SecurityUser su = (SecurityUser) userDetailsService.loadUserByUserId(userId);
        // 旧的 refresh token 加入黑名单, 防止重复刷新
        tokenProvider.blacklist(req.getRefreshToken());
        return buildToken(su);
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            tokenProvider.blacklist(accessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenProvider.blacklist(refreshToken);
        }
    }

    public UserInfo currentUserInfo(SecurityUser su) {
        // 查询用户可见的权限项, 仅 MENU 类型用于构建菜单
        List<SysPermission> perms = loadUserPermissions(su);
        List<MenuNode> menus = buildMenuTree(perms);

        return UserInfo.builder()
                .id(su.getUserId())
                .username(su.getUser().getUsername())
                .nickname(su.getUser().getNickname())
                .email(su.getUser().getEmail())
                .phone(su.getUser().getPhone())
                .roles(new ArrayList<>(su.getRoleCodes()))
                .permissions(new ArrayList<>(su.getPermissions()))
                .menus(menus)
                .build();
    }

    // ------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------

    private TokenResponse buildToken(SecurityUser su) {
        String access = tokenProvider.generateAccessToken(su.getUserId(), su.getUsername());
        String refresh = tokenProvider.generateRefreshToken(su.getUserId(), su.getUsername());
        UserInfo info = currentUserInfo(su);
        return TokenResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(jwtProperties.getAccessTokenTtl())
                .userInfo(info)
                .build();
    }

    /**
     * 加载当前用户拥有的全部权限项(菜单+按钮), 超管直接全量返回.
     */
    private List<SysPermission> loadUserPermissions(SecurityUser su) {
        if (su.isSuperAdmin()) {
            return permissionMapper.selectList(
                    new LambdaQueryWrapper<SysPermission>()
                            .eq(SysPermission::getVisible, 1)
                            .orderByAsc(SysPermission::getSortOrder));
        }
        if (su.getPermissions().isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getPermCode, su.getPermissions())
                        .eq(SysPermission::getVisible, 1)
                        .orderByAsc(SysPermission::getSortOrder));
    }

    /**
     * 把扁平权限列表组装成菜单树.
     */
    private List<MenuNode> buildMenuTree(List<SysPermission> perms) {
        // 仅菜单类型参与树构建(按钮不展示在菜单中)
        List<SysPermission> menus = perms.stream()
                .filter(p -> "MENU".equals(p.getPermType()))
                .collect(Collectors.toList());

        Map<String, MenuNode> nodeMap = new HashMap<>();
        for (SysPermission p : menus) {
            nodeMap.put(p.getId(), toNode(p));
        }

        List<MenuNode> roots = new ArrayList<>();
        for (SysPermission p : menus) {
            MenuNode node = nodeMap.get(p.getId());
            String parentId = p.getParentId();
            if (parentId == null || parentId.isBlank() || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }

        sortTree(roots);
        return roots;
    }

    private MenuNode toNode(SysPermission p) {
        return MenuNode.builder()
                .id(p.getId())
                .parentId(p.getParentId())
                .permCode(p.getPermCode())
                .name(p.getPermName())
                .type(p.getPermType())
                .path(p.getRoutePath())
                .routeName(p.getRouteName())
                .component(p.getComponent())
                .icon(p.getIcon())
                .sortOrder(p.getSortOrder())
                .visible(p.getVisible())
                .children(new ArrayList<>())
                .build();
    }

    private void sortTree(List<MenuNode> nodes) {
        nodes.sort(Comparator.comparingInt(n -> n.getSortOrder() == null ? 0 : n.getSortOrder()));
        for (MenuNode n : nodes) {
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                sortTree(n.getChildren());
            }
        }
    }
}
