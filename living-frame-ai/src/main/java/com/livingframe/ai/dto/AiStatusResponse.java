package com.livingframe.ai.dto;

public record AiStatusResponse(
        boolean enabled,
        String provider,
        String providerName,
        String model,
        String modelName,
        String reasoningLevel
) {
}
