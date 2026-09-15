package com.example.form.controller;

import com.example.form.common.Result;
import com.example.form.dto.field.FieldSaveRequest;
import com.example.form.dto.field.FieldVO;
import com.example.form.service.FormTemplateFieldService;
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
 * 模板栏位管理接口.
 */
@RestController
@RequestMapping("/templates/{templateId}/fields")
@RequiredArgsConstructor
public class FormTemplateFieldController {

    private final FormTemplateFieldService fieldService;

    @GetMapping
    @PreAuthorize("hasAuthority('form:field:update')")
    public Result<List<FieldVO>> list(@PathVariable String templateId) {
        return Result.ok(fieldService.listByTemplate(templateId));
    }

    @GetMapping("/{fieldId}")
    @PreAuthorize("hasAuthority('form:field:update')")
    public Result<FieldVO> get(@PathVariable String templateId, @PathVariable String fieldId) {
        return Result.ok(fieldService.getById(fieldId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('form:field:create')")
    public Result<String> create(@PathVariable String templateId,
                                 @RequestBody @Valid FieldSaveRequest req) {
        return Result.ok(fieldService.save(templateId, req));
    }

    @PutMapping("/{fieldId}")
    @PreAuthorize("hasAuthority('form:field:update')")
    public Result<Void> update(@PathVariable String templateId, @PathVariable String fieldId,
                               @RequestBody @Valid FieldSaveRequest req) {
        req.setId(fieldId);
        fieldService.save(templateId, req);
        return Result.ok();
    }

    /**
     * 停用栏位(软删除).
     */
    @DeleteMapping("/{fieldId}")
    @PreAuthorize("hasAuthority('form:field:delete')")
    public Result<Void> deactivate(@PathVariable String templateId, @PathVariable String fieldId) {
        fieldService.deactivate(templateId, fieldId);
        return Result.ok();
    }

    /**
     * 启用栏位(恢复).
     */
    @PutMapping("/{fieldId}/activate")
    @PreAuthorize("hasAuthority('form:field:update')")
    public Result<Void> activate(@PathVariable String templateId, @PathVariable String fieldId) {
        fieldService.activate(templateId, fieldId);
        return Result.ok();
    }
}
