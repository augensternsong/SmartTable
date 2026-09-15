package com.example.form.dto.template;

import lombok.Data;

import java.util.List;

/**
 * 给模板分配用户分组(全量覆盖).
 */
@Data
public class AssignTemplateGroupsRequest {
    private List<String> groupIds;
}
