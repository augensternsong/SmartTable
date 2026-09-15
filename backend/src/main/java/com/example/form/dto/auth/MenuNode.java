package com.example.form.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点(供前端路由/菜单渲染).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuNode {
    private String id;
    private String parentId;
    private String permCode;
    private String name;
    private String type;
    private String path;
    private String routeName;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer visible;
    @Builder.Default
    private List<MenuNode> children = new ArrayList<>();
}
