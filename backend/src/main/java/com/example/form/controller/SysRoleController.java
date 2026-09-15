package com.example.form.controller;

import com.example.form.common.PageResult;
import com.example.form.common.Result;
import com.example.form.dto.role.AssignPermissionsRequest;
import com.example.form.dto.role.RoleSaveRequest;
import com.example.form.dto.role.RoleVO;
import com.example.form.service.SysRoleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口.
 */
@RestController
@RequestMapping("/sys/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('sys:role:update')")
    public Result<PageResult<RoleVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "20") Integer size,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(roleService.page(page, size, keyword));
    }

    @GetMapping("/all")
    public Result<List<RoleVO>> all() {
        return Result.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:role:update')")
    public Result<RoleVO> get(@PathVariable String id) {
        return Result.ok(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:role:create')")
    public Result<String> create(@RequestBody @Valid RoleSaveRequest req) {
        return Result.ok(roleService.save(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:role:update')")
    public Result<Void> update(@PathVariable String id, @RequestBody @Valid RoleSaveRequest req) {
        req.setId(id);
        roleService.save(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:role:delete')")
    public Result<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('sys:role:assign-perm')")
    public Result<Void> assignPermissions(@PathVariable String id,
                                          @RequestBody AssignPermissionsRequest req) {
        roleService.assignPermissions(id, req);
        return Result.ok();
    }
}
