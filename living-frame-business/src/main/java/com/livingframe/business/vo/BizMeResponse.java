package com.livingframe.business.vo;

import com.livingframe.common.enums.UserType;

public record BizMeResponse(
        Long id,
        String username,
        String nickname,
        String phone,
        String email,
        String avatar,
        UserType userType
) {
}
