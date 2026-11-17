package com.livingframe.business.controller;

import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.LoginLogQuery;
import com.livingframe.system.dto.OperationLogQuery;
import com.livingframe.system.service.LoginLogService;
import com.livingframe.system.service.OperationLogService;
import com.livingframe.system.vo.LoginLogVO;
import com.livingframe.system.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户端登录日志与操作日志，独立于后台系统日志。
 */
@RestController
@RequestMapping("/api/business/logs")
@RequiredArgsConstructor
public class BizLogController {

    private final LoginLogService loginLogService;
    private final OperationLogService operationLogService;

    @GetMapping("/login")
    @PreAuthorize("@ss.hasPermi('business:login-log:portal')")
    public R<PageResult<LoginLogVO>> loginLogs(LoginLogQuery query) {
        query.setLoginType("BUSINESS");
        return R.ok(loginLogService.page(query));
    }

    @GetMapping("/operation")
    @PreAuthorize("@ss.hasPermi('business:operation-log:portal')")
    public R<PageResult<OperationLogVO>> operationLogs(OperationLogQuery query) {
        query.setUserType("BUSINESS");
        return R.ok(operationLogService.page(query));
    }
}
