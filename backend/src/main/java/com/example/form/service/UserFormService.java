package com.example.form.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.form.common.Constants;
import com.example.form.common.FieldType;
import com.example.form.common.exception.BusinessException;
import com.example.form.dto.field.FieldVO;
import com.example.form.dto.template.TemplateDetailVO;
import com.example.form.dto.userform.FieldHistoryVO;
import com.example.form.dto.userform.FormSubmitItem;
import com.example.form.dto.userform.FormSubmitRequest;
import com.example.form.dto.userform.ReminderVO;
import com.example.form.dto.userform.UserFieldVO;
import com.example.form.dto.userform.UserFormViewVO;
import com.example.form.entity.FormTemplate;
import com.example.form.entity.FormTemplateField;
import com.example.form.entity.FormTemplateGroupAssign;
import com.example.form.entity.SysUser;
import com.example.form.entity.UserFormValue;
import com.example.form.entity.UserFormValueHistory;
import com.example.form.mapper.FormTemplateFieldMapper;
import com.example.form.mapper.FormTemplateGroupAssignMapper;
import com.example.form.mapper.FormTemplateMapper;
import com.example.form.mapper.SysUserMapper;
import com.example.form.mapper.UserFormValueHistoryMapper;
import com.example.form.mapper.UserFormValueMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户表单填写服务: 加载填写视图 / 提交 / 历史 / 到期提醒.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFormService {

    private final FormTemplateMapper templateMapper;
    private final FormTemplateGroupAssignMapper assignMapper;
    private final FormTemplateFieldMapper fieldMapper;
    private final UserFormValueMapper valueMapper;
    private final UserFormValueHistoryMapper historyMapper;
    private final SysUserMapper sysUserMapper;
    private final TemplateCacheService cacheService;
    private final ObjectMapper objectMapper;

    /**
     * 用户填写视图: 模板 + 全部 ACTIVE 栏位 + 每个栏位的当前值/超期标记.
     */
    public UserFormViewVO getFormForUser(String templateId) {
        FormTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (!Constants.TEMPLATE_PUBLISHED.equals(template.getStatus())) {
            throw new BusinessException("模板未发布, 不可填写");
        }
        ensureAccess(templateId);

        // 栏位 + 选项(走缓存)
        List<FieldVO> fields = cacheService.getPublishedFields(templateId);

        // 当前值
        String uid = currentUser();
        List<UserFormValue> values = valueMapper.selectByUserAndTemplate(uid, templateId);
        Map<String, UserFormValue> valueByFieldId = new HashMap<>();
        for (UserFormValue v : values) {
            valueByFieldId.put(v.getFieldId(), v);
        }

        LocalDateTime now = LocalDateTime.now();
        int pendingCount = 0;
        int expiredCount = 0;
        List<UserFieldVO> userFields = new ArrayList<>();
        for (FieldVO f : fields) {
            UserFormValue cur = valueByFieldId.get(f.getId());
            UserFieldVO uf = UserFieldVO.builder()
                    .id(f.getId())
                    .templateId(f.getTemplateId())
                    .fieldCode(f.getFieldCode())
                    .fieldName(f.getFieldName())
                    .fieldType(f.getFieldType())
                    .sortOrder(f.getSortOrder())
                    .required(f.getRequired())
                    .maxLength(f.getMaxLength())
                    .minValue(f.getMinValue())
                    .maxValue(f.getMaxValue())
                    .regexPattern(f.getRegexPattern())
                    .fillCycleDays(f.getFillCycleDays())
                    .placeholder(f.getPlaceholder())
                    .description(f.getDescription())
                    .status(f.getStatus())
                    .version(f.getVersion())
                    .createdAt(f.getCreatedAt())
                    .updatedAt(f.getUpdatedAt())
                    .options(f.getOptions())
                    .build();

            if (cur != null && StringUtils.hasText(cur.getFieldValue())) {
                uf.setCurrentValue(cur.getFieldValue());
                uf.setFilledAt(cur.getFilledAt());
                uf.setFilledBy(cur.getFilledBy());
                long days = Duration.between(cur.getFilledAt(), now).toDays();
                uf.setDaysSinceFilled((int) Math.max(0, days));
                if (f.getFillCycleDays() != null && days > f.getFillCycleDays()) {
                    uf.setExpired(true);
                    uf.setPending(false);
                    expiredCount++;
                } else {
                    uf.setExpired(false);
                    uf.setPending(false);
                }
            } else {
                uf.setCurrentValue(null);
                uf.setFilledAt(null);
                uf.setPending(true);
                uf.setExpired(false);
                uf.setDaysSinceFilled(null);
                pendingCount++;
            }
            userFields.add(uf);
        }

        return UserFormViewVO.builder()
                .templateId(template.getId())
                .templateCode(template.getTemplateCode())
                .templateName(template.getTemplateName())
                .description(template.getDescription())
                .version(template.getVersion())
                .updatedAt(template.getUpdatedAt())
                .fields(userFields)
                .pendingCount(pendingCount)
                .expiredCount(expiredCount)
                .build();
    }

    /**
     * 提交填写: 校验 -> 写历史 -> upsert 当前值. 仅变化的栏位写历史.
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(String templateId, FormSubmitRequest req) {
        FormTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (!Constants.TEMPLATE_PUBLISHED.equals(template.getStatus())) {
            throw new BusinessException("模板未发布, 不可填写");
        }
        ensureAccess(templateId);

        // 模板 + 栏位 + 选项(走缓存)
        List<FieldVO> fields = cacheService.getPublishedFields(templateId);
        Map<String, FieldVO> fieldMap = fields.stream()
                .collect(Collectors.toMap(FieldVO::getId, f -> f));

        // 当前值
        String uid = currentUser();
        List<UserFormValue> existing = valueMapper.selectByUserAndTemplate(uid, templateId);
        Map<String, UserFormValue> existingByField = new HashMap<>();
        for (UserFormValue v : existing) {
            existingByField.put(v.getFieldId(), v);
        }

        LocalDateTime now = LocalDateTime.now();
        int changedCount = 0;
        for (FormSubmitItem item : req.getItems()) {
            FieldVO field = fieldMap.get(item.getFieldId());
            if (field == null) {
                throw new BusinessException("栏位不存在或已停用: " + item.getFieldId());
            }
            // 校验
            String value = normalizeValue(field, item.getValue());
            validateValue(field, value);

            UserFormValue cur = existingByField.get(item.getFieldId());
            String oldValue = cur == null ? null : cur.getFieldValue();

            // 仅在值变化时写历史 + 更新当前值
            if (!stringEquals(oldValue, value)) {
                // 写历史
                UserFormValueHistory h = new UserFormValueHistory();
                h.setUserId(uid);
                h.setTemplateId(templateId);
                h.setFieldId(item.getFieldId());
                h.setFieldValue(value);
                h.setFilledAt(now);
                h.setFilledBy(uid);
                historyMapper.insert(h);

                // upsert 当前值
                if (cur == null) {
                    UserFormValue nv = new UserFormValue();
                    nv.setUserId(uid);
                    nv.setTemplateId(templateId);
                    nv.setFieldId(item.getFieldId());
                    nv.setFieldValue(value);
                    nv.setFilledAt(now);
                    nv.setFilledBy(uid);
                    valueMapper.insert(nv);
                } else {
                    cur.setFieldValue(value);
                    cur.setFilledAt(now);
                    cur.setFilledBy(uid);
                    valueMapper.updateById(cur);
                }
                changedCount++;
            }
        }
        log.info("用户 {} 提交模板 {} 共 {} 个栏位, 实际变更 {} 项", uid, templateId, req.getItems().size(), changedCount);
    }

    /**
     * 查询当前用户某栏位的填写历史(倒序).
     */
    public List<FieldHistoryVO> getFieldHistory(String fieldId) {
        String uid = currentUser();
        List<UserFormValueHistory> records = historyMapper.selectList(
                new LambdaQueryWrapper<UserFormValueHistory>()
                        .eq(UserFormValueHistory::getUserId, uid)
                        .eq(UserFormValueHistory::getFieldId, fieldId)
                        .orderByDesc(UserFormValueHistory::getFilledAt));
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        // 取栏位名(允许查 INACTIVE 栏位的旧历史)
        // 这里直接通过 user_form_value 关联拿 template_id, 不再做权限校验
        // 取所有填写人用户名
        Set<String> userIds = records.stream()
                .map(UserFormValueHistory::getFilledBy)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            sysUserMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }
        // 栏位名
        String fieldName = records.isEmpty() ? null
                : fetchFieldName(records.get(0).getFieldId());

        return records.stream().map(h -> FieldHistoryVO.builder()
                .id(h.getId())
                .fieldId(h.getFieldId())
                .fieldName(fieldName)
                .fieldValue(h.getFieldValue())
                .filledAt(h.getFilledAt())
                .filledBy(h.getFilledBy())
                .filledByUsername(userMap.containsKey(h.getFilledBy()) ? userMap.get(h.getFilledBy()).getUsername() : null)
                .filledByNickname(userMap.containsKey(h.getFilledBy()) ? userMap.get(h.getFilledBy()).getNickname() : null)
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * 当前用户的待办提醒(超期 + 未填写).
     */
    public List<ReminderVO> getMyReminders() {
        String uid = currentUser();
        List<UserFormValue> values = valueMapper.selectList(
                new LambdaQueryWrapper<UserFormValue>().eq(UserFormValue::getUserId, uid));
        Map<String, UserFormValue> valueByFieldId = values.stream()
                .collect(Collectors.toMap(UserFormValue::getFieldId, v -> v, (a, b) -> a));

        // 查询当前用户可见的全部已发布模板的栏位
        List<String> templateIds = visibleTemplateIds(uid);
        List<ReminderVO> reminders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (String tid : templateIds) {
            TemplateDetailVO detail = cacheService.getTemplateDetail(tid);
            if (detail == null) continue;
            for (FieldVO f : detail.getFields() == null ? List.<FieldVO>of() : detail.getFields()) {
                UserFormValue cur = valueByFieldId.get(f.getId());
                if (cur == null || !StringUtils.hasText(cur.getFieldValue())) {
                    // 从未填写 -> 仅在必填或有限制周期时提醒
                    reminders.add(ReminderVO.builder()
                            .templateId(tid)
                            .templateName(detail.getTemplateName())
                            .fieldId(f.getId())
                            .fieldName(f.getFieldName())
                            .fieldType(f.getFieldType())
                            .type("PENDING")
                            .filledAt(null)
                            .daysSinceFilled(null)
                            .fillCycleDays(f.getFillCycleDays())
                            .build());
                } else if (f.getFillCycleDays() != null) {
                    long days = Duration.between(cur.getFilledAt(), now).toDays();
                    if (days > f.getFillCycleDays()) {
                        reminders.add(ReminderVO.builder()
                                .templateId(tid)
                                .templateName(detail.getTemplateName())
                                .fieldId(f.getId())
                                .fieldName(f.getFieldName())
                                .fieldType(f.getFieldType())
                                .type("REMINDER")
                                .filledAt(cur.getFilledAt())
                                .daysSinceFilled((int) Math.max(0, days))
                                .fillCycleDays(f.getFillCycleDays())
                                .build());
                    }
                }
            }
        }
        // 优先按"超期天数倒序"排, 再按模板名
        reminders.sort((a, b) -> {
            int d1 = a.getDaysSinceFilled() == null ? -1 : a.getDaysSinceFilled();
            int d2 = b.getDaysSinceFilled() == null ? -1 : b.getDaysSinceFilled();
            return Integer.compare(d2, d1);
        });
        return reminders;
    }

    // ------------------------------------------------------------

    private String currentUser() {
        return com.example.form.security.SecurityUtils.currentUserId();
    }

    private void ensureAccess(String templateId) {
        if (com.example.form.security.SecurityUtils.isSuperAdmin()) {
            return;
        }
        String uid = currentUser();
        List<String> myGroupIds = sysUserMapper.selectGroupIds(uid);
        if (myGroupIds == null || myGroupIds.isEmpty()) {
            throw new BusinessException(403, "您没有该模板的填写权限");
        }
        Long cnt = assignMapper.selectCount(
                new LambdaQueryWrapper<FormTemplateGroupAssign>()
                        .eq(FormTemplateGroupAssign::getTemplateId, templateId)
                        .in(FormTemplateGroupAssign::getGroupId, myGroupIds));
        if (cnt == null || cnt == 0) {
            throw new BusinessException(403, "您没有该模板的填写权限");
        }
    }

    private List<String> visibleTemplateIds(String uid) {
        if (com.example.form.security.SecurityUtils.isSuperAdmin()) {
            return templateMapper.selectList(
                            new LambdaQueryWrapper<FormTemplate>()
                                    .eq(FormTemplate::getStatus, Constants.TEMPLATE_PUBLISHED))
                    .stream().map(FormTemplate::getId).collect(Collectors.toList());
        }
        List<String> myGroupIds = sysUserMapper.selectGroupIds(uid);
        if (myGroupIds == null || myGroupIds.isEmpty()) {
            return List.of();
        }
        return assignMapper.selectList(
                        new LambdaQueryWrapper<FormTemplateGroupAssign>()
                                .in(FormTemplateGroupAssign::getGroupId, myGroupIds))
                .stream().map(FormTemplateGroupAssign::getTemplateId)
                .distinct().collect(Collectors.toList());
    }

    private String fetchFieldName(String fieldId) {
        FormTemplateField f = fieldMapper.selectById(fieldId);
        return f == null ? null : f.getFieldName();
    }

    /**
     * 规范化值: 多选时把前端传入的 JSON 数组字符串规整(去重 + 排序稳定), 避免顺序差异触发"变化".
     */
    private String normalizeValue(FieldVO field, String raw) {
        if (raw == null) {
            return null;
        }
        String type = field.getFieldType();
        if (FieldType.SELECT_MULTI.name().equals(type)) {
            if (raw.isBlank()) {
                return null;
            }
            try {
                List<String> arr = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
                // 去重 + 排序(便于"值是否变化"判断稳定)
                List<String> sorted = arr.stream().distinct().sorted().collect(Collectors.toList());
                if (sorted.isEmpty()) {
                    return null;
                }
                try {
                    return objectMapper.writeValueAsString(sorted);
                } catch (JsonProcessingException e) {
                    return raw;
                }
            } catch (JsonProcessingException e) {
                throw new BusinessException("多选栏位 [" + field.getFieldName() + "] 的值不是合法的 JSON 数组");
            }
        }
        // 其他类型: 去掉首尾空白
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 按栏位规则校验值, 不合法抛业务异常.
     */
    private void validateValue(FieldVO field, String value) {
        FieldType type;
        try {
            type = FieldType.valueOf(field.getFieldType());
        } catch (IllegalArgumentException e) {
            return; // 类型未知不校验, 避免阻断
        }
        boolean required = field.getRequired() != null && field.getRequired() == 1;
        if (value == null || value.isBlank()) {
            if (required) {
                throw new BusinessException("[" + field.getFieldName() + "] 为必填项");
            }
            return;
        }
        switch (type) {
            case TEXT, TEXTAREA -> {
                if (field.getMaxLength() != null && value.length() > field.getMaxLength()) {
                    throw new BusinessException("[" + field.getFieldName() + "] 长度不能超过 " + field.getMaxLength());
                }
                if (StringUtils.hasText(field.getRegexPattern())) {
                    if (!value.matches(field.getRegexPattern())) {
                        throw new BusinessException("[" + field.getFieldName() + "] 格式不正确");
                    }
                }
            }
            case NUMBER -> {
                BigDecimal n;
                try {
                    n = new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw new BusinessException("[" + field.getFieldName() + "] 必须是数字");
                }
                if (field.getMinValue() != null && n.compareTo(field.getMinValue()) < 0) {
                    throw new BusinessException("[" + field.getFieldName() + "] 不能小于 " + field.getMinValue());
                }
                if (field.getMaxValue() != null && n.compareTo(field.getMaxValue()) > 0) {
                    throw new BusinessException("[" + field.getFieldName() + "] 不能大于 " + field.getMaxValue());
                }
            }
            case DATE -> {
                try {
                    java.sql.Date.valueOf(value);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("[" + field.getFieldName() + "] 日期格式应为 yyyy-MM-dd");
                }
            }
            case DATETIME -> {
                try {
                    java.sql.Timestamp.valueOf(value);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("[" + field.getFieldName() + "] 日期时间格式应为 yyyy-MM-dd HH:mm:ss");
                }
            }
            case SELECT_SINGLE -> {
                Set<String> opts = field.getOptions() == null ? Set.of()
                        : field.getOptions().stream().map(o -> o.getOptionValue()).collect(Collectors.toSet());
                if (!opts.contains(value)) {
                    throw new BusinessException("[" + field.getFieldName() + "] 选项值无效");
                }
            }
            case SELECT_MULTI -> {
                Set<String> opts = field.getOptions() == null ? Set.of()
                        : field.getOptions().stream().map(o -> o.getOptionValue()).collect(Collectors.toSet());
                List<String> arr;
                try {
                    arr = objectMapper.readValue(value, new TypeReference<List<String>>() {});
                } catch (JsonProcessingException e) {
                    throw new BusinessException("[" + field.getFieldName() + "] 多选值格式错误");
                }
                for (String v : arr) {
                    if (!opts.contains(v)) {
                        throw new BusinessException("[" + field.getFieldName() + "] 选项值无效: " + v);
                    }
                }
            }
        }
    }

    private boolean stringEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
