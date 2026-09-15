package com.example.form.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.form.dto.perm.PermissionNode;
import com.example.form.entity.SysPermission;
import com.example.form.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限服务: 只读(权限与后端 @PreAuthorize 注解绑定, 不开放 CRUD).
 */
@Service
@RequiredArgsConstructor
public class SysPermissionService {

    private final SysPermissionMapper permissionMapper;

    /**
     * 返回完整权限树.
     */
    public List<PermissionNode> tree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortOrder));
        Map<String, PermissionNode> nodeMap = new HashMap<>();
        for (SysPermission p : all) {
            nodeMap.put(p.getId(), toNode(p));
        }
        List<PermissionNode> roots = new ArrayList<>();
        for (SysPermission p : all) {
            PermissionNode node = nodeMap.get(p.getId());
            String pid = p.getParentId();
            if (pid == null || pid.isBlank() || !nodeMap.containsKey(pid)) {
                roots.add(node);
            } else {
                nodeMap.get(pid).getChildren().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    private PermissionNode toNode(SysPermission p) {
        return PermissionNode.builder()
                .id(p.getId())
                .parentId(p.getParentId())
                .permCode(p.getPermCode())
                .permName(p.getPermName())
                .permType(p.getPermType())
                .sortOrder(p.getSortOrder())
                .visible(p.getVisible())
                .children(new ArrayList<>())
                .build();
    }

    private void sortTree(List<PermissionNode> nodes) {
        nodes.sort(Comparator.comparingInt(n -> n.getSortOrder() == null ? 0 : n.getSortOrder()));
        for (PermissionNode n : nodes) {
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                sortTree(n.getChildren());
            }
        }
    }
}
