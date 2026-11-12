package com.livingframe.ai.controller;

import com.livingframe.ai.dto.AgentRequest;
import com.livingframe.ai.dto.AgentResponse;
import com.livingframe.ai.dto.AiConversationDetail;
import com.livingframe.ai.dto.AiConversationView;
import com.livingframe.ai.dto.AiStatusResponse;
import com.livingframe.ai.dto.AiUserConfigRequest;
import com.livingframe.ai.dto.AiUserConfigView;
import com.livingframe.ai.dto.ChatRequest;
import com.livingframe.ai.dto.ChatResponse;
import com.livingframe.ai.service.AiAgentService;
import com.livingframe.ai.service.AiChatService;
import com.livingframe.ai.service.AiConversationService;
import com.livingframe.ai.service.AiUserConfigService;
import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;
    private final AiAgentService aiAgentService;
    private final AiUserConfigService userConfigService;
    private final AiConversationService conversationService;

    @GetMapping("/status")
    @PreAuthorize("@ss.hasAnyPermi('ai:chat','ai:agent')")
    public R<AiStatusResponse> status() {
        return R.ok(userConfigService.status());
    }

    @GetMapping("/configs")
    @PreAuthorize("@ss.hasAnyPermi('ai:chat','ai:agent')")
    public R<List<AiUserConfigView>> configs() {
        return R.ok(userConfigService.listCurrent());
    }

    @PostMapping("/configs")
    @OperationLog("新增个人 AI 模型配置")
    @PreAuthorize("@ss.hasAnyPermi('ai:chat','ai:agent')")
    public R<Long> createConfig(@Valid @RequestBody AiUserConfigRequest request) {
        return R.ok(userConfigService.create(request));
    }

    @PutMapping("/configs/{id}")
    @OperationLog("编辑个人 AI 模型配置")
    @PreAuthorize("@ss.hasAnyPermi('ai:chat','ai:agent')")
    public R<Void> updateConfig(@PathVariable Long id, @Valid @RequestBody AiUserConfigRequest request) {
        userConfigService.update(id, request);
        return R.ok();
    }

    @DeleteMapping("/configs/{id}")
    @OperationLog("删除个人 AI 模型配置")
    @PreAuthorize("@ss.hasAnyPermi('ai:chat','ai:agent')")
    public R<Void> deleteConfig(@PathVariable Long id) {
        userConfigService.delete(id);
        return R.ok();
    }

    @PutMapping("/configs/{id}/default")
    @OperationLog("切换默认 AI 模型")
    @PreAuthorize("@ss.hasAnyPermi('ai:chat','ai:agent')")
    public R<Void> setDefaultConfig(@PathVariable Long id) {
        userConfigService.setDefault(id);
        return R.ok();
    }

    @PostMapping("/chat")
    @PreAuthorize("@ss.hasPermi('ai:chat')")
    public R<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return R.ok(aiChatService.chat(request));
    }

    @GetMapping("/conversations")
    @PreAuthorize("@ss.hasPermi('ai:chat')")
    public R<List<AiConversationView>> conversations() {
        return R.ok(conversationService.listConversations());
    }

    @GetMapping("/conversations/{id}")
    @PreAuthorize("@ss.hasPermi('ai:chat')")
    public R<AiConversationDetail> conversationDetail(@PathVariable Long id) {
        return R.ok(conversationService.detail(id));
    }

    @DeleteMapping("/conversations/{id}")
    @OperationLog("删除 AI 对话")
    @PreAuthorize("@ss.hasPermi('ai:chat')")
    public R<Void> deleteConversation(@PathVariable Long id) {
        conversationService.delete(id);
        return R.ok();
    }

    @PostMapping("/agent")
    @PreAuthorize("@ss.hasPermi('ai:agent')")
    public R<AgentResponse> agent(@Valid @RequestBody AgentRequest request) {
        return R.ok(aiAgentService.run(request));
    }
}
