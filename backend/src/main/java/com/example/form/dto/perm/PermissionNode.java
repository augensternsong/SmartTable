package com.example.form.dto.perm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限树节点(用于角色分配权限的勾选 UI).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionNode {
    private String id;
    private String parentId;
    private String permCode;
    private String permName;
    private String permType;
    private Integer sortOrder;
    private Integer visible;
    @Builder.Default
    private List<PermissionNode> children = new ArrayList<>();
}
