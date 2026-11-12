package com.livingframe.system.vo;

import java.util.List;

public record UserVO(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        String avatar,
        Integer status,
        String lastLoginAt,
        String createdAt,
        List<RoleBriefVO> roles
) {
}
