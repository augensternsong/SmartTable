package com.example.form.dto.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模板列表项.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVO {
    private String id;
    private String templateCode;
    private String templateName;
    private String description;
    private String status;
    private Integer version;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 栏位数量(含停用) */
    private Long fieldCount;
    /** 已分配分组数 */
    private Long assignedGroupCount;
}
