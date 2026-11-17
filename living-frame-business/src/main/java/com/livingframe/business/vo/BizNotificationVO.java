package com.livingframe.business.vo;

public record BizNotificationVO(
        Long id,
        String type,
        String title,
        String content,
        String targetType,
        Long targetId,
        boolean read,
        String createdAt
) {
}
