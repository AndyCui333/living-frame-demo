package com.livingframe.ai.service;

import cn.hutool.core.util.StrUtil;
import com.livingframe.ai.config.AiMemoryProperties;
import com.livingframe.ai.dto.AiMessageView;
import com.livingframe.ai.dto.ChatRequest;
import com.livingframe.ai.dto.ChatResponse;
import com.livingframe.ai.model.AiResolvedConfig;
import com.livingframe.ai.model.MemoryHit;
import com.livingframe.common.api.ResultCode;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final String SYSTEM_PROMPT = """
            你是 living-frame 后台系统的智能助手，使用简体中文回答。
            回答要简洁、准确；涉及系统统计信息时优先调用工具，不要编造数据。
            你会收到“长期记忆”和“最近对话”。长期记忆来自用户过去的对话，只在相关时参考；
            如果记忆与当前问题无关，不要主动提及，也不要编造记忆中没有的信息。
            """;

    private final AiChatClientFactory chatClientFactory;
    private final AiUserConfigService userConfigService;
    private final AiConversationService conversationService;
    private final AiQdrantMemoryService memoryService;
    private final AiMemoryProperties memoryProperties;

    public ChatResponse chat(ChatRequest request) {
        Long userId = LoginUserContext.require().getId();
        AiResolvedConfig config = userConfigService.resolveCurrent();
        Long conversationId = conversationService.resolveConversation(
                request.conversationId(), userId, request.message());
        List<AiMessageView> history = conversationService.recentMessages(
                conversationId, memoryProperties.getHistoryLimit());
        List<MemoryHit> memories = memoryService.search(
                userId, conversationId, request.message());

        conversationService.append(
                conversationId, userId, "user", request.message(), config.model());

        String prompt = buildPrompt(memories, history, request.message());
        try {
            ChatClient chatClient = chatClientFactory.create(config, SYSTEM_PROMPT);
            String reply = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            if (StrUtil.isBlank(reply)) {
                reply = "模型本次没有返回内容，请重试。";
            }
            conversationService.append(
                    conversationId, userId, "assistant", reply, config.model());
            memoryService.indexTurn(userId, conversationId, request.message(), reply);
            return new ChatResponse(String.valueOf(conversationId), reply, memories.size());
        } catch (Exception ex) {
            if (isModelNotFound(ex)) {
                log.warn("AI 模型不存在: provider={}, model={}", config.provider(), config.model());
                throw new BusinessException(400,
                        "模型 " + config.model() + " 不存在或当前账号无权访问，请到「AI 设置」选择可用模型");
            }
            log.error("调用 AI 对话失败: provider={}, model={}", config.provider(), config.model(), ex);
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "AI 服务暂时不可用，请检查模型配置或稍后重试");
        }
    }

    private String buildPrompt(List<MemoryHit> memories, List<AiMessageView> history, String current) {
        StringBuilder prompt = new StringBuilder();
        if (!memories.isEmpty()) {
            prompt.append("【长期记忆】\n");
            for (MemoryHit memory : memories) {
                String content = StrUtil.maxLength(
                        memory.content().replaceAll("\\s+", " ").trim(), 500);
                prompt.append("- ").append(content).append('\n');
            }
            prompt.append('\n');
        }
        if (!history.isEmpty()) {
            prompt.append("【最近对话】\n");
            int budget = 5000;
            for (AiMessageView message : history) {
                String role = "assistant".equals(message.role()) ? "助手" : "用户";
                String content = StrUtil.maxLength(
                        message.content().replaceAll("\\s+", " ").trim(), 800);
                String line = role + ": " + content + "\n";
                if (budget - line.length() < 0) {
                    break;
                }
                prompt.append(line);
                budget -= line.length();
            }
            prompt.append('\n');
        }
        prompt.append("【当前问题】\n").append(current);
        return prompt.toString();
    }

    private boolean isModelNotFound(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("Model not exist")
                    || message.contains("model_not_found")
                    || message.contains("does not exist"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
