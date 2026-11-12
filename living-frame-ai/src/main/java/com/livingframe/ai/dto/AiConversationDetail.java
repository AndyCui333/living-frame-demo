package com.livingframe.ai.dto;

import java.util.List;

public record AiConversationDetail(
        Long id,
        String title,
        List<AiMessageView> messages
) {
}
