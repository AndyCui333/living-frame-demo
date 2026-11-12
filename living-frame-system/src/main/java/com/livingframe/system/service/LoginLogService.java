package com.livingframe.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.livingframe.common.api.PageResult;
import com.livingframe.system.dto.LoginLogQuery;
import com.livingframe.system.entity.SysLoginLog;
import com.livingframe.system.mapper.SysLoginLogMapper;
import com.livingframe.system.vo.LoginLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysLoginLogMapper loginLogMapper;

    public void recordSuccess(String username, String loginType, String ip, String userAgent, Long userId) {
        record(username, loginType, ip, userAgent, "SUCCESS", "登录成功", userId);
    }

    public void recordFailure(String username, String loginType, String ip, String userAgent,
                              String message, Long userId) {
        record(username, loginType, ip, userAgent, "FAIL", message, userId);
    }

    private void record(String username, String loginType, String ip, String userAgent,
                        String status, String message, Long userId) {
        try {
            loginLogMapper.insert(new SysLoginLog()
                    .setUserId(userId)
                    .setUsername(username)
                    .setLoginType(loginType)
                    .setIp(ip)
                    .setUserAgent(userAgent)
                    .setStatus(status)
                    .setMessage(message)
                    .setLoginAt(LocalDateTime.now()));
        } catch (Exception ex) {
            log.warn("写入登录日志失败: {}", ex.getMessage());
        }
    }

    public PageResult<LoginLogVO> page(LoginLogQuery query) {
        query.normalize();
        LambdaQueryWrapper<SysLoginLog> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(query.getUsername())) {
            wrapper.like(SysLoginLog::getUsername, query.getUsername().trim());
        }
        if (StrUtil.isNotBlank(query.getIp())) {
            wrapper.like(SysLoginLog::getIp, query.getIp().trim());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(SysLoginLog::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StrUtil.isNotBlank(query.getLoginType())) {
            wrapper.eq(SysLoginLog::getLoginType, query.getLoginType().trim().toUpperCase());
        }
        wrapper.orderByDesc(SysLoginLog::getId);
        Page<SysLoginLog> page = loginLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<LoginLogVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private LoginLogVO toVO(SysLoginLog log) {
        return new LoginLogVO(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getLoginType(),
                log.getIp(),
                log.getUserAgent(),
                log.getStatus(),
                log.getMessage(),
                log.getLoginAt() == null ? null : FORMATTER.format(log.getLoginAt()));
    }
}
