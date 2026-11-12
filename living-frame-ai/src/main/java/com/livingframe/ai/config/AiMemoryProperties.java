package com.livingframe.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai.memory")
public class AiMemoryProperties {

    private boolean enabled = true;
    /**
     * Qdrant 的 HTTP REST 端口（默认 6333；当前环境的 HTTP 端口为 6033）。
     */
    private String qdrantUrl = "http://localhost:6333";
    private String collection = "living-frame-ai-memory";
    private int vectorSize = 384;
    private int topK = 5;
    private float scoreThreshold = 0.18f;
    private int historyLimit = 10;
    private long redisTtlHours = 6;
}
