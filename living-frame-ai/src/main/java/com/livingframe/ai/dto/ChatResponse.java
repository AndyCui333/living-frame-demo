package com.livingframe.ai.dto;

public record ChatResponse(
        String conversationId,
        String reply,
        int memoryCount
) {
}
