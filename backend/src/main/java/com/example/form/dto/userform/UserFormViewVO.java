package com.example.form.dto.userform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户填写表单视图(模板 + 栏位 + 每个栏位的当前值/超期标记).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFormViewVO {
    private String templateId;
    private String templateCode;
    private String templateName;
    private String description;
    private Integer version;
    private LocalDateTime updatedAt;
    /** 栏位列表(按 sortOrder, 含当前值与超期标记) */
    private List<UserFieldVO> fields;
    /** 待填写栏位数 */
    private Integer pendingCount;
    /** 已超期栏位数 */
    private Integer expiredCount;
}
