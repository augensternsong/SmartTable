package com.example.form.controller;

import com.example.form.common.PageResult;
import com.example.form.common.Result;
import com.example.form.dto.user.AssignGroupsRequest;
import com.example.form.dto.user.AssignRolesRequest;
import com.example.form.dto.user.ChangePasswordRequest;
import com.example.form.dto.user.ResetPasswordRequest;
import com.example.form.dto.user.UserPageRequest;
import com.example.form.dto.user.UserSaveRequest;
import com.example.form.dto.user.UserVO;
import com.example.form.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户管理接口.
 */
@RestController
@RequestMapping("/sys/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('sys:user:update')")
    public Result<PageResult<UserVO>> page(UserPageRequest req) {
        return Result.ok(userService.page(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public Result<UserVO> get(@PathVariable String id) {
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:user:create')")
    public Result<String> create(@RequestBody @Valid UserSaveRequest req) {
        return Result.ok(userService.save(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public Result<Void> update(@PathVariable String id, @RequestBody @Valid UserSaveRequest req) {
        req.setId(id);
        userService.save(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user:delete')")
    public Result<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public Result<Void> assignRoles(@PathVariable String id, @RequestBody AssignRolesRequest req) {
        userService.assignRoles(id, req);
        return Result.ok();
    }

    @PutMapping("/{id}/groups")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public Result<Void> assignGroups(@PathVariable String id, @RequestBody AssignGroupsRequest req) {
        userService.assignGroups(id, req);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('sys:user:reset-pwd')")
    public Result<Void> resetPassword(@PathVariable String id, @RequestBody @Valid ResetPasswordRequest req) {
        userService.resetPassword(id, req);
        return Result.ok();
    }

    /** 用户自助改密, 不走按钮权限 */
    @PutMapping("/me/password")
    public Result<Void> changeMyPassword(@RequestBody @Valid ChangePasswordRequest req) {
        userService.changeMyPassword(req);
        return Result.ok();
    }

    /** 返回当前用户不可分配的角色ID集合(用于前端禁用选项) */
    @GetMapping("/editable-roles")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public Result<Map<String, Object>> editableRoles() {
        return Result.ok(Map.of("disabledRoleIds", userService.myEditableRoleIds()));
    }
}
