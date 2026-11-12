package com.livingframe.system.controller;

import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.OperationLogQuery;
import com.livingframe.system.service.OperationLogService;
import com.livingframe.system.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('system:operation-log:list')")
    public R<PageResult<OperationLogVO>> page(OperationLogQuery query) {
        return R.ok(operationLogService.page(query));
    }
}
