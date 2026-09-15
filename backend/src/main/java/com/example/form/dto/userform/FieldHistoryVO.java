package com.example.form.dto.userform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 单个栏位的填写历史记录.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldHistoryVO {
    private String id;
    private String fieldId;
    private String fieldName;
    private String fieldValue;
    private LocalDateTime filledAt;
    private String filledBy;
    /** 填写人用户名(便于展示) */
    private String filledByUsername;
    private String filledByNickname;
}
