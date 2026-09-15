package com.example.form.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.form.common.Constants;
import com.example.form.entity.SysRole;
import com.example.form.entity.SysUser;
import com.example.form.entity.SysUserRole;
import com.example.form.mapper.SysRoleMapper;
import com.example.form.mapper.SysUserMapper;
import com.example.form.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动初始化: 确保存在默认超级管理员账号(若已存在则跳过).
 *
 * <p>角色和权限的种子数据由 Flyway SQL 维护, 此处仅负责
 * 含 bcrypt 哈希密码的 admin 账号(无法在 SQL 中预置).
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${form.init.admin-username:admin}")
    private String adminUsername;

    @Value("${form.init.admin-password:admin123}")
    private String adminPassword;

    @Value("${form.init.admin-nickname:超级管理员}")
    private String adminNickname;

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, adminUsername));
        if (count != null && count > 0) {
            log.info("默认管理员账号 {} 已存在, 跳过初始化", adminUsername);
            return;
        }

        SysRole superAdminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, Constants.ROLE_SUPER_ADMIN));
        if (superAdminRole == null) {
            log.error("SUPER_ADMIN 角色未找到, 请检查 Flyway 是否已执行迁移");
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname(adminNickname);
        admin.setStatus(Constants.STATUS_ENABLED);
        userMapper.insert(admin);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(admin.getId());
        userRole.setRoleId(superAdminRole.getId());
        userRoleMapper.insert(userRole);

        log.info("默认管理员账号初始化完成: {} / {} (请尽快修改默认密码)", adminUsername, adminPassword);
    }
}
