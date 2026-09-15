package com.example.form.dto.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {
    private String id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    /** 已分配的权限ID列表 */
    private List<String> permissionIds;
}
