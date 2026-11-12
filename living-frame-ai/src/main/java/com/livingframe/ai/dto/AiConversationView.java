package com.livingframe.ai.dto;

import java.time.LocalDateTime;

public record AiConversationView(
        Long id,
        String title,
        int messageCount,
        LocalDateTime lastMessageAt
) {
}
