package com.livingframe.system.vo;

import com.livingframe.common.enums.UserType;

import java.util.Set;

public record MeResponse(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        String avatar,
        UserType userType,
        Set<String> roles,
        Set<String> permissions,
        boolean superAdmin
) {
}
