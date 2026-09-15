package com.example.form.dto.role;

import lombok.Data;

import java.util.List;

/**
 * 给角色分配权限(全量覆盖).
 */
@Data
public class AssignPermissionsRequest {
    private List<String> permissionIds;
}
