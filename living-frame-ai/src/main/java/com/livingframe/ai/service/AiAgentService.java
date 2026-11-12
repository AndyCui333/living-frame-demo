package com.livingframe.ai.service;

import com.livingframe.ai.dto.AgentRequest;
import com.livingframe.ai.dto.AgentResponse;
import com.livingframe.ai.model.AiResolvedConfig;
import com.livingframe.ai.tool.SystemTools;
import com.livingframe.common.api.ResultCode;
import com.livingframe.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentService {

    private static final String AGENT_PROMPT = """
            你是 living-frame 的运维助手。你可以调用工具查询系统统计信息。
            需要数据时必须调用工具，不要凭空猜测；最终用简体中文给出简洁结论。
            """;

    private final AiChatClientFactory chatClientFactory;
    private final SystemTools systemTools;
    private final AiUserConfigService userConfigService;

    public AgentResponse run(AgentRequest request) {
        AiResolvedConfig config = userConfigService.resolveCurrent();
        try {
            ChatClient chatClient = chatClientFactory.create(config, AGENT_PROMPT);
            String reply = chatClient.prompt()
                    .user(request.task())
                    .tools(systemTools)
                    .call()
                    .content();
            return new AgentResponse(reply);
        } catch (Exception ex) {
            if (isModelNotFound(ex)) {
                log.warn("AI Agent 模型不存在: provider={}, model={}", config.provider(), config.model());
                throw new BusinessException(400,
                        "模型 " + config.model() + " 不存在或当前账号无权访问，请到「AI 设置」选择可用模型");
            }
            log.error("执行 AI Agent 失败: provider={}, model={}", config.provider(), config.model(), ex);
            throw new BusinessException(ResultCode.AI_UNAVAILABLE, "Agent 暂时不可用，请检查模型配置或稍后重试");
        }
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
