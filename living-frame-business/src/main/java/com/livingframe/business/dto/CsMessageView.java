package com.livingframe.business.dto;

import java.time.LocalDateTime;

public record CsMessageView(
        Long id,
        String senderType,
        String senderName,
        String content,
        LocalDateTime createdAt
) {
}
