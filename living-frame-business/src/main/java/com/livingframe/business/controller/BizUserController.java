package com.livingframe.business.controller;

import com.livingframe.business.dto.BizUserCreateRequest;
import com.livingframe.business.dto.BizUserQuery;
import com.livingframe.business.dto.BizUserUpdateRequest;
import com.livingframe.business.service.BizUserService;
import com.livingframe.business.vo.BizUserActivityVO;
import com.livingframe.business.vo.BizUserStatisticsVO;
import com.livingframe.business.vo.BizUserVO;
import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.ResetPasswordRequest;
import com.livingframe.system.dto.StatusRequest;
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
@RequestMapping("/api/business/users")
@RequiredArgsConstructor
public class BizUserController {

    private final BizUserService bizUserService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('business:user:list')")
    public R<PageResult<BizUserVO>> page(BizUserQuery query) {
        return R.ok(bizUserService.page(query));
    }

    @GetMapping("/activity")
    @PreAuthorize("@ss.hasPermi('business:user:list')")
    public R<BizUserActivityVO> activity() {
        return R.ok(bizUserService.activity());
    }

    @GetMapping("/statistics")
    @PreAuthorize("@ss.hasPermi('business:statistics:list')")
    public R<BizUserStatisticsVO> statistics() {
        return R.ok(bizUserService.statistics());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('business:user:list')")
    public R<BizUserVO> get(@PathVariable Long id) {
        return R.ok(bizUserService.get(id));
    }

    @PostMapping
    @OperationLog("新增业务用户")
    @PreAuthorize("@ss.hasPermi('business:user:create')")
    public R<Long> create(@Valid @RequestBody BizUserCreateRequest request) {
        return R.ok(bizUserService.create(request));
    }

    @PutMapping("/{id}")
    @OperationLog("编辑业务用户")
    @PreAuthorize("@ss.hasPermi('business:user:update')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody BizUserUpdateRequest request) {
        bizUserService.update(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog("删除业务用户")
    @PreAuthorize("@ss.hasPermi('business:user:delete')")
    public R<Void> delete(@PathVariable Long id) {
        bizUserService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @OperationLog("修改业务用户状态")
    @PreAuthorize("@ss.hasPermi('business:user:update')")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        bizUserService.updateStatus(id, request.status());
        return R.ok();
    }

    @PutMapping("/{id}/password")
    @OperationLog("重置业务用户密码")
    @PreAuthorize("@ss.hasPermi('business:user:update')")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        bizUserService.resetPassword(id, request.password());
        return R.ok();
    }

    @PostMapping("/{id}/blacklist")
    @OperationLog("门户用户一键拉黑")
    @PreAuthorize("@ss.hasPermi('business:user:update')")
    public R<Void> blacklist(@PathVariable Long id) {
        bizUserService.blacklist(id);
        return R.ok();
    }
}
