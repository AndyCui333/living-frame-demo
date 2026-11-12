package com.livingframe.ai.service;

import com.livingframe.ai.config.AiMemoryProperties;
import com.livingframe.ai.model.MemoryHit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Qdrant 长期记忆：按用户隔离向量，按会话保存对话轮次。
 * Qdrant 不可用时只降级为 MySQL + Redis，不影响正常对话。
 */
@Slf4j
@Service
public class AiQdrantMemoryService {

    private final AiMemoryProperties properties;
    private final AiEmbeddingService embeddingService;
    private final RestClient restClient;

    private volatile boolean collectionReady = false;

    public AiQdrantMemoryService(
            AiMemoryProperties properties,
            AiEmbeddingService embeddingService) {
        this.properties = properties;
        this.embeddingService = embeddingService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getQdrantUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public List<MemoryHit> search(Long userId, Long conversationId, String query) {
        if (!properties.isEnabled() || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            ensureCollection();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("vector", embeddingService.embed(query));
            body.put("limit", properties.getTopK());
            body.put("with_payload", true);
            body.put("score_threshold", properties.getScoreThreshold());
            Map<String, Object> filter = new LinkedHashMap<>();
            filter.put("must", List.of(Map.of(
                    "key", "user_id",
                    "match", Map.of("value", String.valueOf(userId)))));
            if (conversationId != null) {
                filter.put("must_not", List.of(Map.of(
                        "key", "conversation_id",
                        "match", Map.of("value", String.valueOf(conversationId)))));
            }
            body.put("filter", filter);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/collections/{collection}/points/search", properties.getCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !(response.get("result") instanceof List<?> rows)) {
                return List.of();
            }
            List<MemoryHit> hits = new ArrayList<>();
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> item)) {
                    continue;
                }
                Object payloadValue = item.get("payload");
                if (!(payloadValue instanceof Map<?, ?> payload)) {
                    continue;
                }
                Object content = payload.get("content");
                Object cid = payload.get("conversation_id");
                Object score = item.get("score");
                if (content != null && score instanceof Number number) {
                    hits.add(new MemoryHit(
                            cid == null ? null : Long.valueOf(String.valueOf(cid)),
                            String.valueOf(content),
                            number.floatValue()));
                }
            }
            return hits;
        } catch (Exception ex) {
            log.warn("Qdrant 记忆检索失败，本次对话降级为 MySQL/Redis 上下文: {}", ex.getMessage());
            return List.of();
        }
    }

    public void indexTurn(Long userId, Long conversationId, String userMessage, String assistantReply) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            ensureCollection();
            String content = limit("用户: " + userMessage + "\n助手: " + assistantReply, 4000);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("user_id", String.valueOf(userId));
            payload.put("conversation_id", String.valueOf(conversationId));
            payload.put("role", "turn");
            payload.put("content", content);
            payload.put("created_at", Instant.now().toString());

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("id", UUID.randomUUID().toString());
            point.put("vector", embeddingService.embed(content));
            point.put("payload", payload);

            restClient.put()
                    .uri("/collections/{collection}/points?wait=true", properties.getCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("points", List.of(point)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Qdrant 记忆写入失败，不影响本次对话: {}", ex.getMessage());
        }
    }

    public void deleteConversation(Long conversationId) {
        if (!properties.isEnabled() || conversationId == null) {
            return;
        }
        try {
            ensureCollection();
            Map<String, Object> filter = Map.of("must", List.of(Map.of(
                    "key", "conversation_id",
                    "match", Map.of("value", String.valueOf(conversationId)))));
            restClient.post()
                    .uri("/collections/{collection}/points/delete?wait=true", properties.getCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("filter", filter))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Qdrant 会话记忆删除失败: {}", ex.getMessage());
        }
    }

    private synchronized void ensureCollection() {
        if (collectionReady) {
            return;
        }
        try {
            restClient.get()
                    .uri("/collections/{collection}", properties.getCollection())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound notFound) {
            Map<String, Object> vectors = Map.of(
                    "size", properties.getVectorSize(),
                    "distance", "Cosine");
            restClient.put()
                    .uri("/collections/{collection}", properties.getCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("vectors", vectors))
                    .retrieve()
                    .toBodilessEntity();
            log.info("已创建 Qdrant 记忆集合: {}", properties.getCollection());
        }
        collectionReady = true;
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
