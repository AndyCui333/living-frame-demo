package com.livingframe.business.controller;

import com.livingframe.business.service.BizNotificationService;
import com.livingframe.business.vo.BizNotificationVO;
import com.livingframe.common.api.R;
import com.livingframe.common.context.LoginUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/biz/notifications")
@RequiredArgsConstructor
public class BizNotificationController {

    private final BizNotificationService notificationService;

    @GetMapping
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<List<BizNotificationVO>> list(@RequestParam(defaultValue = "all") String filter,
                                           @RequestParam(defaultValue = "all") String status) {
        String effective = "all".equalsIgnoreCase(filter) ? status : filter;
        return R.ok(notificationService.list(currentUserId(), effective));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<Long> unreadCount() {
        return R.ok(notificationService.unreadCount(currentUserId()));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(currentUserId(), id);
        return R.ok();
    }

    @PostMapping("/read-all")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<Void> markAllRead() {
        notificationService.markAllRead(currentUserId());
        return R.ok();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<Void> deleteBatch(@RequestBody List<Long> ids) {
        notificationService.deleteBatch(currentUserId(), ids);
        return R.ok();
    }

    private Long currentUserId() {
        return LoginUserContext.require().getId();
    }
}
