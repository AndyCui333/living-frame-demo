package com.livingframe.business.controller;

import com.livingframe.business.dto.CsAgentView;
import com.livingframe.business.dto.CsOnlineInbox;
import com.livingframe.business.dto.CsRankingView;
import com.livingframe.business.service.CsAgentService;
import com.livingframe.business.service.CsTicketService;
import com.livingframe.common.api.R;
import com.livingframe.common.context.LoginUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cs/agents")
@RequiredArgsConstructor
public class CsAgentController {

    private final CsAgentService agentService;
    private final CsTicketService ticketService;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPermi('cs:chat:list','cs:ticket:list')")
    public R<List<CsAgentView>> agents(@RequestParam(required = false) String serviceType) {
        return R.ok(agentService.listAgents(serviceType));
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("@ss.hasAnyPermi('cs:chat:list','cs:ticket:list')")
    public R<CsOnlineInbox> heartbeat() {
        Long userId = LoginUserContext.require().getId();
        agentService.heartbeat(userId);
        return R.ok(ticketService.onlineInbox(userId));
    }

    @PostMapping("/offline")
    @PreAuthorize("@ss.hasAnyPermi('cs:chat:list','cs:ticket:list')")
    public R<Void> offline() {
        agentService.offline(LoginUserContext.require().getId());
        return R.ok();
    }

    @GetMapping("/ranking")
    @PreAuthorize("@ss.hasAnyPermi('cs:chat:list','cs:ticket:list')")
    public R<List<CsRankingView>> ranking() {
        return R.ok(ticketService.ranking());
    }
}
