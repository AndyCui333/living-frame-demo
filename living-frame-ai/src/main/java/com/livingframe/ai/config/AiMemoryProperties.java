package com.livingframe.ai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.ai.memory")
public class AiMemoryProperties {

    /**
     * 是否启用 Qdrant 长期记忆。
     */
    private boolean enabled;

    /**
     * Qdrant HTTP REST 地址，必须由当前环境显式配置。
     */
    @NotBlank
    private String qdrantUrl;

    @NotBlank
    private String collection;

    @Min(1)
    private int vectorSize;

    @Min(1)
    private int topK;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private float scoreThreshold;

    @Min(1)
    private int historyLimit;

    @Min(1)
    private long redisTtlHours;
}
