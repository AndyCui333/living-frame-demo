package com.livingframe.common.model;

import com.livingframe.common.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 登录用户快照，保存在 sa-token 的 token session 中。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private UserType userType;
    /** 角色编码，例如 SUPER_ADMIN。 */
    @Builder.Default
    private Set<String> roles = new HashSet<>();
    /** 权限编码，例如 system:user:list。 */
    @Builder.Default
    private Set<String> permissions = new HashSet<>();
    /** 是否超级管理员，代码内直接放行。 */
    private boolean superAdmin;
}
