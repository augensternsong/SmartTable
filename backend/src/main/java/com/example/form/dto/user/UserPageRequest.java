package com.example.form.dto.user;

import lombok.Data;

/**
 * 用户分页查询请求.
 */
@Data
public class UserPageRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String username;
    private String nickname;
    private String phone;
    private Integer status;
    /** 角色ID过滤 */
    private String roleId;
    /** 分组ID过滤 */
    private String groupId;
}
