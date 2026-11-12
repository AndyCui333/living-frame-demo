package com.livingframe.ai.dto;

import java.time.LocalDateTime;

public record AiMessageView(
        Long id,
        String role,
        String content,
        LocalDateTime createdAt
) {
}
