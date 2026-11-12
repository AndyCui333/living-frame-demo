package com.livingframe.ai.model;

public record MemoryHit(
        Long conversationId,
        String content,
        float score
) {
}
