package com.example.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.form.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper.
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询用户拥有的角色编码集合.
     */
    @Select("""
            SELECT r.role_code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 1
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectRoleCodes(@Param("userId") String userId);

    /**
     * 查询用户拥有的权限编码集合(通过角色聚合).
     */
    @Select("""
            SELECT DISTINCT p.perm_code
            FROM sys_user_role ur
            JOIN sys_role_permission rp ON rp.role_id = ur.role_id
            JOIN sys_permission p ON p.id = rp.permission_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectPermissionCodes(@Param("userId") String userId);

    /**
     * 查询用户已分配的用户分组ID集合.
     */
    @Select("""
            SELECT group_id
            FROM sys_user_group_member
            WHERE user_id = #{userId}
            """)
    List<String> selectGroupIds(@Param("userId") String userId);
}
