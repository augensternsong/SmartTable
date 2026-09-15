package com.example.form.controller;

import com.example.form.common.Result;
import com.example.form.dto.perm.PermissionNode;
import com.example.form.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限接口(只读).
 */
@RestController
@RequestMapping("/sys/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;

    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('sys:role:assign-perm','sys:perm:update')")
    public Result<List<PermissionNode>> tree() {
        return Result.ok(permissionService.tree());
    }
}
