package com.livingframe.business.controller;

import com.livingframe.business.dto.CsMessageView;
import com.livingframe.business.dto.CsTicketDetail;
import com.livingframe.business.dto.CsTicketFeedbackRequest;
import com.livingframe.business.dto.CsTicketMessageRequest;
import com.livingframe.business.dto.CsTicketSaveRequest;
import com.livingframe.business.dto.CsTicketView;
import com.livingframe.business.service.CsTicketService;
import com.livingframe.common.api.R;
import com.livingframe.common.context.LoginUserContext;
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
@RequestMapping("/api/biz/cs/tickets")
@RequiredArgsConstructor
public class BizCsTicketController {

    private final CsTicketService ticketService;

    @GetMapping
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<List<CsTicketView>> list() {
        return R.ok(ticketService.listPortalTickets(currentUserId()));
    }

    @PostMapping("/drafts")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<Long> createDraft(@Valid @RequestBody CsTicketSaveRequest request) {
        return R.ok(ticketService.createDraft(currentUserId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<Void> updateDraft(@PathVariable Long id, @Valid @RequestBody CsTicketSaveRequest request) {
        ticketService.updateDraft(currentUserId(), id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<Void> deleteDraft(@PathVariable Long id) {
        ticketService.deleteDraft(currentUserId(), id);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<CsTicketDetail> submit(@PathVariable Long id) {
        return R.ok(ticketService.submit(currentUserId(), id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<CsTicketDetail> detail(@PathVariable Long id) {
        return R.ok(ticketService.detailForPortal(currentUserId(), id));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<CsMessageView> message(@PathVariable Long id,
                                    @Valid @RequestBody CsTicketMessageRequest request) {
        return R.ok(ticketService.portalMessage(currentUserId(), id, request.content()));
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<Void> feedback(@PathVariable Long id,
                            @Valid @RequestBody CsTicketFeedbackRequest request) {
        ticketService.feedback(currentUserId(), id, request.rating(), request.feedback());
        return R.ok();
    }

    private Long currentUserId() {
        return LoginUserContext.require().getId();
    }
}
