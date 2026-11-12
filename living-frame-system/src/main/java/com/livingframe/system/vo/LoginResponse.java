package com.livingframe.system.vo;

import com.livingframe.common.enums.UserType;

public record LoginResponse(
        String token,
        String tokenName,
        long expiresIn,
        Long userId,
        String username,
        String nickname,
        UserType userType
) {
}
