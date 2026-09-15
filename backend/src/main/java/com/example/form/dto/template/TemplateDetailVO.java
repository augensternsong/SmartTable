package com.example.form.dto.template;

import com.example.form.dto.field.FieldVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板详情(含栏位列表 + 已分配分组ID).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDetailVO {
    private String id;
    private String templateCode;
    private String templateName;
    private String description;
    private String status;
    private Integer version;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 已分配的用户分组ID */
    private List<String> assignedGroupIds;
    /** 栏位列表(按 sortOrder 排序, 含 ACTIVE 与 INACTIVE) */
    private List<FieldVO> fields;
}
