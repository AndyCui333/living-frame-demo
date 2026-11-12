package com.livingframe.ai.service;

import cn.hutool.core.util.StrUtil;
import com.livingframe.ai.model.AiResolvedConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 根据当前用户的厂商/模型配置动态创建 Spring AI ChatClient。
 */
@Component
public class AiChatClientFactory {

    public ChatClient create(AiResolvedConfig config, String systemPrompt) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .build();

        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .model(config.model());
        boolean openAi = "OPENAI".equalsIgnoreCase(config.provider());
        boolean qwen = "QWEN".equalsIgnoreCase(config.provider());
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
            } else {
                String effort = "MAX".equals(level) && openAi
                        ? "high"
                        : level.toLowerCase();
                options.reasoningEffort(effort);
            }
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
}
