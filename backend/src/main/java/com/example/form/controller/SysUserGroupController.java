package com.example.form.controller;

import com.example.form.common.PageResult;
import com.example.form.common.Result;
import com.example.form.dto.group.AssignUsersRequest;
import com.example.form.dto.group.GroupSaveRequest;
import com.example.form.dto.group.GroupVO;
import com.example.form.service.SysUserGroupService;
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
 * 用户分组接口.
 */
@RestController
@RequestMapping("/sys/groups")
@RequiredArgsConstructor
public class SysUserGroupController {

    private final SysUserGroupService groupService;

    @GetMapping
    @PreAuthorize("hasAuthority('sys:group:update')")
    public Result<PageResult<GroupVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "20") Integer size,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(groupService.page(page, size, keyword));
    }

    @GetMapping("/all")
    public Result<List<GroupVO>> all() {
        return Result.ok(groupService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:group:update')")
    public Result<GroupVO> get(@PathVariable String id) {
        return Result.ok(groupService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:group:create')")
    public Result<String> create(@RequestBody @Valid GroupSaveRequest req) {
        return Result.ok(groupService.save(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:group:update')")
    public Result<Void> update(@PathVariable String id, @RequestBody @Valid GroupSaveRequest req) {
        req.setId(id);
        groupService.save(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:group:delete')")
    public Result<Void> delete(@PathVariable String id) {
        groupService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/users")
    @PreAuthorize("hasAuthority('sys:group:assign-user')")
    public Result<Void> assignUsers(@PathVariable String id, @RequestBody AssignUsersRequest req) {
        groupService.assignUsers(id, req);
        return Result.ok();
    }
}
