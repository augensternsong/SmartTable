package com.example.form.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.form.entity.SysUser;
import com.example.form.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spring Security 用户加载服务.
 *
 * <p>每次请求从 DB 加载用户 + 角色编码 + 权限编码, 并组装为 {@link SecurityUser}.
 * 低并发场景下 DB 查询足够快; 高并发时可在此处加 @Cacheable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已被禁用");
        }
        Set<String> roleCodes = new HashSet<>(sysUserMapper.selectRoleCodes(user.getId()));
        Set<String> permissions = new HashSet<>(sysUserMapper.selectPermissionCodes(user.getId()));
        return new SecurityUser(user, roleCodes, permissions);
    }

    /**
     * 按用户ID加载, 用于 JWT 过滤器中根据 token 内的 uid 还原主体.
     */
    public UserDetails loadUserByUserId(String userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已被禁用");
        }
        Set<String> roleCodes = new HashSet<>(sysUserMapper.selectRoleCodes(user.getId()));
        Set<String> permissions = new HashSet<>(sysUserMapper.selectPermissionCodes(user.getId()));
        return new SecurityUser(user, roleCodes, permissions);
    }
}
