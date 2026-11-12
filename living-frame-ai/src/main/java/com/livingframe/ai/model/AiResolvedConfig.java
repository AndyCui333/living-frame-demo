package com.livingframe.ai.model;

import java.math.BigDecimal;

/**
 * 本次请求实际使用的模型配置（包含前端本地传入的 API Key，不持久化）。
 */
public record AiResolvedConfig(
        String provider,
        String providerName,
        String model,
        String reasoningLevel,
        BigDecimal temperature,
        Integer maxTokens,
        String baseUrl,
        String apiKey,
        boolean configured
) {
}
