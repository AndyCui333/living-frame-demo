package com.livingframe.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AiUserConfigRequest(
        @NotBlank(message = "厂商类型不能为空")
        @Pattern(regexp = "^(DEEPSEEK|QWEN|OPENAI|CUSTOM)$", message = "厂商类型不合法")
        String provider,

        @NotBlank(message = "厂商名称不能为空")
        @Size(max = 64, message = "厂商名称过长")
        String providerName,

        @NotBlank(message = "Base URL 不能为空")
        @Size(max = 512, message = "Base URL 过长")
        @Pattern(regexp = "^https?://.+", message = "Base URL 必须以 http:// 或 https:// 开头")
        String baseUrl,

        @NotBlank(message = "模型不能为空")
        @Size(max = 128, message = "模型标识过长")
        @Pattern(regexp = "^[A-Za-z0-9._:-]+$", message = "模型标识只能包含字母、数字、点、下划线、冒号或短横线")
        String model,

        @Size(max = 512, message = "API Key 过长")
        String apiKey,

        Boolean clearApiKey,

        Boolean defaultConfig,

        @Size(max = 16, message = "推理等级过长")
        String reasoningLevel,

        @DecimalMin(value = "0.0", message = "温度不能小于 0")
        @DecimalMax(value = "2.0", message = "温度不能大于 2")
        BigDecimal temperature,

        @Min(value = 256, message = "最大 token 不能小于 256")
        @Max(value = 32768, message = "最大 token 不能超过 32768")
        Integer maxTokens
) {
}
