package com.livingframe.system.controller;

import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.ChangePasswordRequest;
import com.livingframe.system.dto.LoginRequest;
import com.livingframe.system.service.AuthService;
import com.livingframe.system.service.CaptchaService;
import com.livingframe.system.vo.CaptchaResponse;
import com.livingframe.system.vo.LoginResponse;
import com.livingframe.system.vo.MeResponse;
import com.livingframe.system.vo.PermissionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public R<CaptchaResponse> captcha() {
        return R.ok(captchaService.generate());
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return R.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @GetMapping("/me")
    public R<MeResponse> me() {
        return R.ok(authService.me());
    }

    @GetMapping("/menus")
    public R<List<PermissionVO>> menus() {
        return R.ok(authService.menus());
    }

    @PutMapping("/password")
    @OperationLog("修改个人密码")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return R.ok();
    }
}
