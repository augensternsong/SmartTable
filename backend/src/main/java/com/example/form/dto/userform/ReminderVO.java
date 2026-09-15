package com.example.form.dto.userform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待办提醒项(超期或未填写的栏位).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderVO {
    private String templateId;
    private String templateName;
    private String fieldId;
    private String fieldName;
    private String fieldType;
    /** REMINDER 超期 / PENDING 未填写 */
    private String type;
    private LocalDateTime filledAt;
    private Integer daysSinceFilled;
    private Integer fillCycleDays;
}
