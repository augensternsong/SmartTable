package com.example.form.dto.group;

import lombok.Data;

import java.util.List;

/**
 * 给分组分配用户(全量覆盖).
 */
@Data
public class AssignUsersRequest {
    private List<String> userIds;
}
