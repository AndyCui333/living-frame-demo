package com.livingframe.business.controller;

import com.livingframe.business.dto.CsAgentView;
import com.livingframe.business.service.CsAgentService;
import com.livingframe.common.api.R;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cs/agents")
@RequiredArgsConstructor
public class CsAgentController {

    private final CsAgentService agentService;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPermi('cs:chat:list','cs:ticket:list')")
    public R<List<CsAgentView>> agents(@RequestParam(required = false) String serviceType) {
        return R.ok(agentService.listAgents(serviceType));
    }
}
