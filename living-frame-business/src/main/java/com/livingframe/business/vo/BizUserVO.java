package com.livingframe.business.vo;

public record BizUserVO(
        Long id,
        String username,
        String nickname,
        String phone,
        String email,
        String avatar,
        Integer status,
        String lastLoginAt,
        String createdAt
) {
}
