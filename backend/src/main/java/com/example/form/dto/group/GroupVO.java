package com.example.form.dto.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户分组视图.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupVO {
    private String id;
    private String groupCode;
    private String groupName;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    /** 已分配用户数 */
    private Long memberCount;
    /** 已分配用户ID列表(详情时返回) */
    private List<String> userIds;
}
