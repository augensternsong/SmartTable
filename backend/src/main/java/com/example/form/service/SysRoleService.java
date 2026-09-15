package com.example.form.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.form.common.Constants;
import com.example.form.common.PageResult;
import com.example.form.common.exception.BusinessException;
import com.example.form.dto.role.AssignPermissionsRequest;
import com.example.form.dto.role.RoleSaveRequest;
import com.example.form.dto.role.RoleVO;
import com.example.form.entity.SysRole;
import com.example.form.entity.SysRolePermission;
import com.example.form.entity.SysUserRole;
import com.example.form.mapper.SysRoleMapper;
import com.example.form.mapper.SysRolePermissionMapper;
import com.example.form.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务.
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    public PageResult<RoleVO> page(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(keyword), SysRole::getRoleName, keyword)
                .or()
                .like(StringUtils.hasText(keyword), SysRole::getRoleCode, keyword)
                .orderByAsc(SysRole::getCreatedAt);
        Page<SysRole> p = roleMapper.selectPage(new Page<>(page, size), wrapper);
        List<RoleVO> records = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), records);
    }

    public List<RoleVO> listAll() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(SysRole::getCreatedAt));
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    public RoleVO getById(String id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return toVO(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public String save(RoleSaveRequest req) {
        boolean isNew = !StringUtils.hasText(req.getId());
        // 编码唯一性校验
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, req.getRoleCode())
                .ne(!isNew, SysRole::getId, req.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("角色编码已存在");
        }
        SysRole role;
        if (isNew) {
            role = new SysRole();
            role.setRoleCode(req.getRoleCode());
            role.setStatus(Constants.STATUS_ENABLED);
        } else {
            role = roleMapper.selectById(req.getId());
            if (role == null) {
                throw new BusinessException("角色不存在");
            }
            // 内置角色不可改编码
            if (isBuiltinRole(role.getRoleCode()) && !role.getRoleCode().equals(req.getRoleCode())) {
                throw new BusinessException("内置角色编码不可修改");
            }
        }
        role.setRoleName(req.getRoleName());
        role.setDescription(req.getDescription());
        if (req.getStatus() != null) {
            role.setStatus(req.getStatus());
        }
        if (isNew) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (isBuiltinRole(role.getRoleCode())) {
            throw new BusinessException("内置角色不可删除");
        }
        Long used = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (used != null && used > 0) {
            throw new BusinessException("该角色仍有用户关联, 请先解除关联");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(String roleId, AssignPermissionsRequest req) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        if (req.getPermissionIds() != null) {
            for (String pid : req.getPermissionIds()) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    private boolean isBuiltinRole(String code) {
        return Constants.ROLE_SUPER_ADMIN.equals(code)
                || Constants.ROLE_ADMIN.equals(code)
                || Constants.ROLE_USER.equals(code);
    }

    private RoleVO toVO(SysRole role) {
        List<String> permIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, role.getId()))
                .stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        return RoleVO.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .status(role.getStatus())
                .createdAt(role.getCreatedAt())
                .permissionIds(permIds)
                .build();
    }
}
