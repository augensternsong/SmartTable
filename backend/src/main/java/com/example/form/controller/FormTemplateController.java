package com.example.form.controller;

import com.example.form.common.PageResult;
import com.example.form.common.Result;
import com.example.form.dto.template.AssignTemplateGroupsRequest;
import com.example.form.dto.template.TemplateDetailVO;
import com.example.form.dto.template.TemplatePageRequest;
import com.example.form.dto.template.TemplateSaveRequest;
import com.example.form.dto.template.TemplateVO;
import com.example.form.service.FormTemplateService;
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

import java.util.List;

/**
 * 模板管理接口.
 */
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class FormTemplateController {

    private final FormTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAuthority('form:template:create') or hasAuthority('form:template:update')")
    public Result<PageResult<TemplateVO>> page(TemplatePageRequest req) {
        return Result.ok(templateService.page(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('form:template:create') or hasAuthority('form:template:update')")
    public Result<TemplateDetailVO> detail(@PathVariable String id) {
        return Result.ok(templateService.getDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('form:template:create')")
    public Result<String> create(@RequestBody @Valid TemplateSaveRequest req) {
        return Result.ok(templateService.save(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('form:template:update')")
    public Result<Void> update(@PathVariable String id, @RequestBody @Valid TemplateSaveRequest req) {
        req.setId(id);
        templateService.save(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('form:template:delete')")
    public Result<Void> delete(@PathVariable String id) {
        templateService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('form:template:publish')")
    public Result<Void> publish(@PathVariable String id) {
        templateService.publish(id);
        return Result.ok();
    }

    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('form:template:publish')")
    public Result<Void> archive(@PathVariable String id) {
        templateService.archive(id);
        return Result.ok();
    }

    @PutMapping("/{id}/groups")
    @PreAuthorize("hasAuthority('form:template:update')")
    public Result<Void> assignGroups(@PathVariable String id,
                                      @RequestBody AssignTemplateGroupsRequest req) {
        templateService.assignGroups(id, req);
        return Result.ok();
    }

    /**
     * 当前用户可见的已发布模板(按其所在分组过滤).
     */
    @GetMapping("/mine")
    public Result<List<TemplateVO>> mine() {
        return Result.ok(templateService.listForCurrentUser());
    }
}
