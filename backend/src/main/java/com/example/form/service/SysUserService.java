package com.example.form.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.form.common.Constants;
import com.example.form.common.PageResult;
import com.example.form.common.exception.BusinessException;
import com.example.form.dto.user.AssignGroupsRequest;
import com.example.form.dto.user.AssignRolesRequest;
import com.example.form.dto.user.ChangePasswordRequest;
import com.example.form.dto.user.ResetPasswordRequest;
import com.example.form.dto.user.UserPageRequest;
import com.example.form.dto.user.UserSaveRequest;
import com.example.form.dto.user.UserVO;
import com.example.form.entity.SysRole;
import com.example.form.entity.SysUser;
import com.example.form.entity.SysUserGroupMember;
import com.example.form.entity.SysUserRole;
import com.example.form.mapper.SysRoleMapper;
import com.example.form.mapper.SysUserGroupMemberMapper;
import com.example.form.mapper.SysUserMapper;
import com.example.form.mapper.SysUserRoleMapper;
import com.example.form.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理服务.
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserGroupMemberMapper groupMemberMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(UserPageRequest req) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(req.getUsername()), SysUser::getUsername, req.getUsername())
                .like(StringUtils.hasText(req.getNickname()), SysUser::getNickname, req.getNickname())
                .like(StringUtils.hasText(req.getPhone()), SysUser::getPhone, req.getPhone())
                .eq(req.getStatus() != null, SysUser::getStatus, req.getStatus())
                .orderByDesc(SysUser::getCreatedAt);

        // 角色过滤: 先查匹配该角色的用户ID集合
        if (StringUtils.hasText(req.getRoleId())) {
            List<String> userIds = userRoleMapper.selectList(
                            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, req.getRoleId()))
                    .stream().map(SysUserRole::getUserId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }
            wrapper.in(SysUser::getId, userIds);
        }
        // 分组过滤
        if (StringUtils.hasText(req.getGroupId())) {
            List<String> userIds = groupMemberMapper.selectList(
                            new LambdaQueryWrapper<SysUserGroupMember>().eq(SysUserGroupMember::getGroupId, req.getGroupId()))
                    .stream().map(SysUserGroupMember::getUserId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }
            wrapper.in(SysUser::getId, userIds);
        }

        Page<SysUser> page = new Page<>(req.getPage(), req.getSize());
        Page<SysUser> result = userMapper.selectPage(page, wrapper);
        List<UserVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), records);
    }

    public UserVO getById(String id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public String save(UserSaveRequest req) {
        boolean isNew = !StringUtils.hasText(req.getId());
        SysUser user;
        if (isNew) {
            // 用户名唯一性校验
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, req.getUsername()));
            if (exists != null && exists > 0) {
                throw new BusinessException("用户名已存在");
            }
            if (!StringUtils.hasText(req.getPassword())) {
                throw new BusinessException("新建用户必须设置初始密码");
            }
            user = new SysUser();
            user.setUsername(req.getUsername());
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        } else {
            user = userMapper.selectById(req.getId());
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            // 不允许修改用户名(避免引用混乱)
        }
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRemark(req.getRemark());
        if (req.getStatus() != null) {
            // 不允许禁用最后一个超管
            if (req.getStatus() == Constants.STATUS_DISABLED
                    && isSuperAdmin(user.getId())) {
                Long superAdminActive = countActiveSuperAdmins();
                if (superAdminActive <= 1) {
                    throw new BusinessException("不能禁用最后一个超级管理员账号");
                }
            }
            user.setStatus(req.getStatus());
        }
        if (isNew) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        if (id.equals(SecurityUtils.currentUserId())) {
            throw new BusinessException("不能删除自己");
        }
        if (isSuperAdmin(id)) {
            throw new BusinessException("超级管理员账号不可删除");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        groupMemberMapper.delete(new LambdaQueryWrapper<SysUserGroupMember>().eq(SysUserGroupMember::getUserId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(String userId, AssignRolesRequest req) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (!CollectionUtils.isEmpty(req.getRoleIds())) {
            // 校验角色存在
            List<SysRole> roles = roleMapper.selectBatchIds(req.getRoleIds());
            if (roles.size() != req.getRoleIds().size()) {
                throw new BusinessException("存在无效的角色ID");
            }
            for (String roleId : req.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignGroups(String userId, AssignGroupsRequest req) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        groupMemberMapper.delete(new LambdaQueryWrapper<SysUserGroupMember>().eq(SysUserGroupMember::getUserId, userId));
        if (!CollectionUtils.isEmpty(req.getGroupIds())) {
            for (String groupId : req.getGroupIds()) {
                SysUserGroupMember m = new SysUserGroupMember();
                m.setUserId(userId);
                m.setGroupId(groupId);
                groupMemberMapper.insert(m);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String userId, ResetPasswordRequest req) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeMyPassword(ChangePasswordRequest req) {
        String uid = SecurityUtils.currentUserId();
        SysUser user = userMapper.selectById(uid);
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    // ------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------

    private boolean isSuperAdmin(String userId) {
        List<String> roleCodes = userMapper.selectRoleCodes(userId);
        return roleCodes.contains(Constants.ROLE_SUPER_ADMIN);
    }

    private Long countActiveSuperAdmins() {
        // 查询拥有 SUPER_ADMIN 角色且启用的用户
        List<String> superUserIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>()
                                .inSql(SysUserRole::getRoleId,
                                        "SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN'"))
                .stream().map(SysUserRole::getUserId).distinct().collect(Collectors.toList());
        if (superUserIds.isEmpty()) {
            return 0L;
        }
        return userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, superUserIds)
                .eq(SysUser::getStatus, Constants.STATUS_ENABLED));
    }

    private UserVO toVO(SysUser user) {
        List<String> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<String> roleCodes = new ArrayList<>();
        List<String> groupIds = userMapper.selectGroupIds(user.getId());

        if (!roleIds.isEmpty()) {
            roleCodes = roleMapper.selectBatchIds(roleIds).stream()
                    .map(SysRole::getRoleCode).collect(Collectors.toList());
        }

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .remark(user.getRemark())
                .createdAt(user.getCreatedAt())
                .roleIds(roleIds)
                .roleCodes(roleCodes)
                .groupIds(groupIds)
                .build();
    }

    /** 排除角色超管不可选的角色(用于前端"分配角色"下拉禁用 SUPER_ADMIN 选项) */
    public Set<String> myEditableRoleIds() {
        // 当前实现: 非超管不能分配超管; 超管可分配全部
        if (SecurityUtils.isSuperAdmin()) {
            return Set.of();
        }
        SysRole superAdmin = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, Constants.ROLE_SUPER_ADMIN));
        return superAdmin == null ? Set.of() : Set.of(superAdmin.getId());
    }
}
