package com.livingframe.ai.dto;

import java.math.BigDecimal;

/**
 * 前端只返回是否已配置 Key，绝不返回 Key 明文或密文。
 */
public record AiUserConfigView(
        Long id,
        String provider,
        String providerName,
        String baseUrl,
        String model,
        boolean hasApiKey,
        boolean isDefault,
        String reasoningLevel,
        BigDecimal temperature,
        Integer maxTokens
) {
}
