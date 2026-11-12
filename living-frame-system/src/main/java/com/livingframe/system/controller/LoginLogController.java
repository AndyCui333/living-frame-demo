package com.livingframe.system.controller;

import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.LoginLogQuery;
import com.livingframe.system.service.LoginLogService;
import com.livingframe.system.vo.LoginLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('system:login-log:list')")
    public R<PageResult<LoginLogVO>> page(LoginLogQuery query) {
        return R.ok(loginLogService.page(query));
    }
}
