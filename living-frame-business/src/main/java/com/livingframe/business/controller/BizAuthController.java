package com.livingframe.business.controller;

import com.livingframe.business.dto.BizLoginRequest;
import com.livingframe.business.service.BizAuthService;
import com.livingframe.business.vo.BizMeResponse;
import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.ChangePasswordRequest;
import com.livingframe.system.vo.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/biz/auth")
@RequiredArgsConstructor
public class BizAuthController {

    private final BizAuthService bizAuthService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody BizLoginRequest request, HttpServletRequest httpRequest) {
        return R.ok(bizAuthService.login(request, httpRequest));
    }

    @GetMapping("/me")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<BizMeResponse> me() {
        return R.ok(bizAuthService.me());
    }

    @PostMapping("/logout")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    public R<Void> logout() {
        bizAuthService.logout();
        return R.ok();
    }

    @PutMapping("/password")
    @PreAuthorize("@ss.hasRole('BIZ_USER')")
    @OperationLog("业务用户修改密码")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        bizAuthService.changePassword(request);
        return R.ok();
    }
}
