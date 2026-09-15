package com.example.form.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图(列表/详情).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    /** 角色编码列表 */
    private List<String> roleCodes;
    /** 角色 id 列表 */
    private List<String> roleIds;
    /** 分组 id 列表 */
    private List<String> groupIds;
}
