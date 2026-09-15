package com.example.form.dto.user;

import lombok.Data;

import java.util.List;

/**
 * 给用户分配分组(传分组 id 列表, 全量覆盖).
 */
@Data
public class AssignGroupsRequest {
    private List<String> groupIds;
}
