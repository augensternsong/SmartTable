package com.example.form.security;

import com.example.form.entity.SysUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security 上下文中的用户主体.
 *
 * <p>同时持有用户实体 + 角色编码集合 + 权限编码集合, 供鉴权使用.
 */
@Getter
public class SecurityUser implements UserDetails {

    private final SysUser user;
    /** 角色编码集合, 如 SUPER_ADMIN */
    private final Set<String> roleCodes;
    /** 权限编码集合, 如 form:template:create */
    private final Set<String> permissions;

    public SecurityUser(SysUser user, Set<String> roleCodes, Set<String> permissions) {
        this.user = user;
        this.roleCodes = roleCodes;
        this.permissions = permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 同时把角色和权限都作为 GrantedAuthority, 角色前缀 "ROLE_"
        List<GrantedAuthority> auths = new java.util.ArrayList<>();
        for (String role : roleCodes) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        auths.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
        return auths;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() != null && user.getStatus() == 1;
    }

    public String getUserId() {
        return user.getId();
    }

    public boolean isSuperAdmin() {
        return roleCodes.contains("SUPER_ADMIN");
    }
}
