package com.example.form.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.form.common.Constants;
import com.example.form.common.PageResult;
import com.example.form.common.exception.BusinessException;
import com.example.form.dto.group.AssignUsersRequest;
import com.example.form.dto.group.GroupSaveRequest;
import com.example.form.dto.group.GroupVO;
import com.example.form.entity.SysUserGroup;
import com.example.form.entity.SysUserGroupMember;
import com.example.form.mapper.SysUserGroupMapper;
import com.example.form.mapper.SysUserGroupMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户分组管理服务.
 */
@Service
@RequiredArgsConstructor
public class SysUserGroupService {

    private final SysUserGroupMapper groupMapper;
    private final SysUserGroupMemberMapper memberMapper;

    public PageResult<GroupVO> page(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<SysUserGroup> wrapper = new LambdaQueryWrapper<SysUserGroup>()
                .like(StringUtils.hasText(keyword), SysUserGroup::getGroupName, keyword)
                .or()
                .like(StringUtils.hasText(keyword), SysUserGroup::getGroupCode, keyword)
                .orderByDesc(SysUserGroup::getCreatedAt);
        Page<SysUserGroup> p = groupMapper.selectPage(new Page<>(page, size), wrapper);
        List<GroupVO> records = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), records);
    }

    public List<GroupVO> listAll() {
        List<SysUserGroup> groups = groupMapper.selectList(new LambdaQueryWrapper<SysUserGroup>()
                .eq(SysUserGroup::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(SysUserGroup::getCreatedAt));
        return groups.stream().map(this::toVO).collect(Collectors.toList());
    }

    public GroupVO getById(String id) {
        SysUserGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("分组不存在");
        }
        GroupVO vo = toVO(group);
        vo.setUserIds(memberMapper.selectList(
                        new LambdaQueryWrapper<SysUserGroupMember>().eq(SysUserGroupMember::getGroupId, id))
                .stream().map(SysUserGroupMember::getUserId).collect(Collectors.toList()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public String save(GroupSaveRequest req) {
        boolean isNew = !StringUtils.hasText(req.getId());
        Long exists = groupMapper.selectCount(new LambdaQueryWrapper<SysUserGroup>()
                .eq(SysUserGroup::getGroupCode, req.getGroupCode())
                .ne(!isNew, SysUserGroup::getId, req.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("分组编码已存在");
        }
        SysUserGroup group;
        if (isNew) {
            group = new SysUserGroup();
            group.setGroupCode(req.getGroupCode());
            group.setStatus(Constants.STATUS_ENABLED);
        } else {
            group = groupMapper.selectById(req.getId());
            if (group == null) {
                throw new BusinessException("分组不存在");
            }
        }
        group.setGroupName(req.getGroupName());
        group.setDescription(req.getDescription());
        if (req.getStatus() != null) {
            group.setStatus(req.getStatus());
        }
        if (isNew) {
            groupMapper.insert(group);
        } else {
            groupMapper.updateById(group);
        }
        return group.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        SysUserGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("分组不存在");
        }
        // 解除与用户/模板的关联(分组本身可删除)
        memberMapper.delete(new LambdaQueryWrapper<SysUserGroupMember>().eq(SysUserGroupMember::getGroupId, id));
        groupMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignUsers(String groupId, AssignUsersRequest req) {
        SysUserGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("分组不存在");
        }
        memberMapper.delete(new LambdaQueryWrapper<SysUserGroupMember>()
                .eq(SysUserGroupMember::getGroupId, groupId));
        if (req.getUserIds() != null) {
            for (String uid : req.getUserIds()) {
                SysUserGroupMember m = new SysUserGroupMember();
                m.setGroupId(groupId);
                m.setUserId(uid);
                memberMapper.insert(m);
            }
        }
    }

    private GroupVO toVO(SysUserGroup group) {
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<SysUserGroupMember>()
                .eq(SysUserGroupMember::getGroupId, group.getId()));
        return GroupVO.builder()
                .id(group.getId())
                .groupCode(group.getGroupCode())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .status(group.getStatus())
                .createdAt(group.getCreatedAt())
                .memberCount(count == null ? 0L : count)
                .build();
    }
}
