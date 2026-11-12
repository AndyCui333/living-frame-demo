package com.livingframe.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.livingframe.common.api.PageResult;
import com.livingframe.system.dto.OperationLogQuery;
import com.livingframe.system.entity.SysOperationLog;
import com.livingframe.system.mapper.SysOperationLogMapper;
import com.livingframe.system.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysOperationLogMapper operationLogMapper;

    public void record(Long userId, String username, String operation, String method,
                       String requestUri, String ip, String status, String errorMessage, long durationMs) {
        try {
            operationLogMapper.insert(new SysOperationLog()
                    .setUserId(userId)
                    .setUsername(username)
                    .setOperation(operation)
                    .setMethod(method)
                    .setRequestUri(requestUri)
                    .setIp(ip)
                    .setStatus(status)
                    .setErrorMessage(StrUtil.maxLength(errorMessage, 500))
                    .setDurationMs(durationMs)
                    .setCreatedAt(LocalDateTime.now()));
        } catch (Exception ex) {
            log.warn("写入操作日志失败: {}", ex.getMessage());
        }
    }

    public PageResult<OperationLogVO> page(OperationLogQuery query) {
        query.normalize();
        LambdaQueryWrapper<SysOperationLog> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(query.getUsername())) {
            wrapper.like(SysOperationLog::getUsername, query.getUsername().trim());
        }
        if (StrUtil.isNotBlank(query.getOperation())) {
            wrapper.like(SysOperationLog::getOperation, query.getOperation().trim());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(SysOperationLog::getStatus, query.getStatus().trim().toUpperCase());
        }
        wrapper.orderByDesc(SysOperationLog::getId);
        Page<SysOperationLog> page = operationLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<OperationLogVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private OperationLogVO toVO(SysOperationLog log) {
        return new OperationLogVO(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getOperation(),
                log.getMethod(),
                log.getRequestUri(),
                log.getIp(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getDurationMs(),
                log.getCreatedAt() == null ? null : FORMATTER.format(log.getCreatedAt()));
    }
}
