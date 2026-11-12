package com.livingframe.system.vo;

public record LoginLogVO(
        Long id,
        Long userId,
        String username,
        String loginType,
        String ip,
        String userAgent,
        String status,
        String message,
        String loginAt
) {
}
