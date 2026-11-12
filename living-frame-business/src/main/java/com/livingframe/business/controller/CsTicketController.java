package com.livingframe.business.controller;

import com.livingframe.business.dto.CsMessageView;
import com.livingframe.business.dto.CsTicketDetail;
import com.livingframe.business.dto.CsTicketMessageRequest;
import com.livingframe.business.dto.CsTicketTransferRequest;
import com.livingframe.business.dto.CsTicketView;
import com.livingframe.business.service.CsTicketService;
import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.R;
import com.livingframe.common.context.LoginUserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cs/tickets")
@RequiredArgsConstructor
public class CsTicketController {

    private final CsTicketService ticketService;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPermi('cs:chat:list','cs:ticket:list')")
    public R<List<CsTicketView>> list(@RequestParam(required = false) String type) {
        return R.ok(ticketService.listAgentTickets(currentUserId(), type));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPermi('cs:chat:list','cs:ticket:list')")
    public R<CsTicketDetail> detail(@PathVariable Long id) {
        return R.ok(ticketService.detailForAgent(currentUserId(), id));
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("@ss.hasPermi('cs:chat:reply')")
    public R<CsMessageView> reply(@PathVariable Long id,
                                  @Valid @RequestBody CsTicketMessageRequest request) {
        return R.ok(ticketService.agentReply(currentUserId(), id, request.content()));
    }

    @PostMapping("/{id}/dispatch")
    @OperationLog("派遣客服工单")
    @PreAuthorize("@ss.hasPermi('cs:ticket:dispatch')")
    public R<Void> dispatch(@PathVariable Long id) {
        ticketService.dispatch(id);
        return R.ok();
    }

    @PostMapping("/{id}/transfer")
    @OperationLog("转接客服工单")
    @PreAuthorize("@ss.hasPermi('cs:ticket:transfer')")
    public R<Void> transfer(@PathVariable Long id, @Valid @RequestBody CsTicketTransferRequest request) {
        ticketService.transfer(currentUserId(), id, request.targetAgentId());
        return R.ok();
    }

    @PostMapping("/{id}/archive")
    @OperationLog("截单归档客服工单")
    @PreAuthorize("@ss.hasPermi('cs:ticket:close')")
    public R<Void> archive(@PathVariable Long id) {
        ticketService.archive(currentUserId(), id);
        return R.ok();
    }

    private Long currentUserId() {
        return LoginUserContext.require().getId();
    }
}
