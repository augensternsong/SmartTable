package com.example.form.dto.user;

import lombok.Data;

import java.util.List;

/**
 * 给用户分配角色(传角色 id 列表, 全量覆盖).
 */
@Data
public class AssignRolesRequest {
    private List<String> roleIds;
}
