package com.livingframe.system.controller;

import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.BlacklistCreateRequest;
import com.livingframe.system.dto.BlacklistQuery;
import com.livingframe.system.dto.StatusRequest;
import com.livingframe.system.service.SystemBlacklistService;
import com.livingframe.system.vo.BlacklistVO;
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

@RestController
@RequestMapping("/api/system/blacklist")
@RequiredArgsConstructor
public class SystemBlacklistController {

    private final SystemBlacklistService blacklistService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('system:blacklist:list')")
    public R<PageResult<BlacklistVO>> page(BlacklistQuery query) {
        return R.ok(blacklistService.page(query));
    }

    @PostMapping
    @OperationLog("新增系统黑名单")
    @PreAuthorize("@ss.hasPermi('system:blacklist:create')")
    public R<Long> create(@Valid @RequestBody BlacklistCreateRequest request) {
        return R.ok(blacklistService.create(request));
    }

    @PutMapping("/{id}/status")
    @OperationLog("修改系统黑名单状态")
    @PreAuthorize("@ss.hasPermi('system:blacklist:update')")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        blacklistService.updateStatus(id, request.status());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog("删除系统黑名单")
    @PreAuthorize("@ss.hasPermi('system:blacklist:delete')")
    public R<Void> delete(@PathVariable Long id) {
        blacklistService.delete(id);
        return R.ok();
    }
}
