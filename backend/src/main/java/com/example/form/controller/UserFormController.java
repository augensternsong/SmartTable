package com.example.form.controller;

import com.example.form.common.Result;
import com.example.form.dto.userform.FieldHistoryVO;
import com.example.form.dto.userform.FormSubmitRequest;
import com.example.form.dto.userform.ReminderVO;
import com.example.form.dto.userform.UserFormViewVO;
import com.example.form.service.UserFormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户表单填写接口(普通用户使用).
 */
@RestController
@RequestMapping("/forms")
@RequiredArgsConstructor
public class UserFormController {

    private final UserFormService userFormService;

    /**
     * 加载某模板的填写视图(含当前值与超期标记).
     */
    @GetMapping("/templates/{templateId}")
    public Result<UserFormViewVO> getForm(@PathVariable String templateId) {
        return Result.ok(userFormService.getFormForUser(templateId));
    }

    /**
     * 提交填写(全量或部分栏位均可).
     */
    @PostMapping("/templates/{templateId}/submit")
    public Result<Void> submit(@PathVariable String templateId,
                               @RequestBody @Valid com.example.form.dto.userform.FormSubmitRequest req) {
        userFormService.submit(templateId, req);
        return Result.ok();
    }

    /**
     * 某栏位的填写历史(倒序).
     */
    @GetMapping("/fields/{fieldId}/history")
    public Result<List<FieldHistoryVO>> history(@PathVariable String fieldId) {
        return Result.ok(userFormService.getFieldHistory(fieldId));
    }

    /**
     * 当前用户的待办提醒(超期 + 未填写).
     */
    @GetMapping("/reminders")
    public Result<List<ReminderVO>> reminders() {
        return Result.ok(userFormService.getMyReminders());
    }
}
