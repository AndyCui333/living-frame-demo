package com.livingframe.ai.service;

import cn.hutool.core.util.StrUtil;
import com.livingframe.ai.model.AiResolvedConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据当前用户的厂商/模型配置动态创建 Spring AI ChatClient。
 */
@Slf4j
@Component
public class AiChatClientFactory {

    public ChatClient create(AiResolvedConfig config, String systemPrompt) {
        String baseUrl = normalizeBaseUrl(config.baseUrl());
        log.info("创建 AI 客户端: provider={}, model={}, baseUrl={}", config.provider(), config.model(), baseUrl);
        RestClient.Builder restClientBuilder = RestClient.builder()
                .messageConverters(converters -> {
                    // 百灵等部分 OpenAI 兼容网关的 JSON 响应会错误地标记为 text/plain。
                    // 注册一个同时接受 application/json 和 text/plain 的 Jackson 转换器。
                    MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
                    jsonConverter.setSupportedMediaTypes(List.of(
                            MediaType.APPLICATION_JSON,
                            MediaType.APPLICATION_JSON_UTF8,
                            MediaType.TEXT_PLAIN));
                    converters.add(jsonConverter);
                });
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(config.apiKey())
                .restClientBuilder(restClientBuilder)
                .build();

        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .model(config.model());
        boolean openAi = "OPENAI".equalsIgnoreCase(config.provider());
        boolean qwen = "QWEN".equalsIgnoreCase(config.provider());
        boolean antLing = (config.baseUrl() != null && config.baseUrl().contains("ant-ling.com"))
                || (config.model() != null && config.model().toUpperCase().startsWith("LING-"));
        if (openAi) {
            options.maxCompletionTokens(config.maxTokens());
        } else {
            options.maxTokens(config.maxTokens());
        }

        // OpenAI 推理模型通常不允许自定义 temperature，其余厂商正常传递
        if (!openAi && config.temperature() != null) {
            options.temperature(config.temperature().doubleValue());
        }

        String level = StrUtil.blankToDefault(config.reasoningLevel(), "NONE").toUpperCase();
        if (!"NONE".equals(level)) {
            if (qwen) {
                Map<String, Object> extraBody = new HashMap<>();
                extraBody.put("enable_thinking", true);
                extraBody.put("thinking_budget", thinkingBudget(level, config.maxTokens()));
                options.extraBody(extraBody);
            } else if (openAi) {
                String effort = "MAX".equals(level) ? "high" : level.toLowerCase();
                options.reasoningEffort(effort);
            }
        }

        if (antLing) {
            // 百灵官方 OpenAI 兼容示例使用 top_k 和 thinking 对象；
            // 这些字段不属于 OpenAI 标准参数，需要通过 extraBody 透传。
            Map<String, Object> extraBody = new HashMap<>();
            extraBody.put("top_k", 20);
            extraBody.put("thinking", Map.of("type", "NONE".equals(level) ? "disabled" : "enabled"));
            options.extraBody(extraBody);
            options.topP(0.95);
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options.build())
                .toolCallingManager(ToolCallingManager.builder().build())
                .build();

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (StrUtil.isNotBlank(systemPrompt)) {
            builder.defaultSystem(systemPrompt);
        }
        return builder.build();
    }

    private int thinkingBudget(String level, Integer maxTokens) {
        int budget = switch (level) {
            case "LOW" -> 1024;
            case "MEDIUM" -> 2048;
            case "HIGH" -> 4096;
            case "MAX" -> 8192;
            default -> 2048;
        };
        return Math.min(budget, maxTokens == null ? budget : maxTokens);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        String lower = normalized.toLowerCase();
        if (lower.endsWith("/v1/chat/completions")) {
            normalized = normalized.substring(0, normalized.length() - "/v1/chat/completions".length());
        } else if (lower.endsWith("/chat/completions")) {
            normalized = normalized.substring(0, normalized.length() - "/chat/completions".length());
        } else if (lower.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized.replaceAll("/+$", "");
    }
}
