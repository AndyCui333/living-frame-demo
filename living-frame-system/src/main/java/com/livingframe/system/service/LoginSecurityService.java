package com.livingframe.system.service;

import com.livingframe.common.constant.Constants;
import com.livingframe.common.enums.UserType;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.system.enums.LoginFailureAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录风控：失败计数、5 分钟锁定、10 次自动加入永久系统黑名单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityService {

    private final StringRedisTemplate stringRedisTemplate;
    private final LoginLogService loginLogService;
    private final SystemBlacklistService blacklistService;

    public void assertNotBlocked(String username, UserType type, String ip, String userAgent) {
        if (blacklistService.isBlocked("IP", ip) || blacklistService.isBlocked("USERNAME", username)) {
            String message = "当前 IP 或账号已被加入系统黑名单";
            loginLogService.recordFailure(username, type.name(), ip, userAgent, message, null);
            throw new BusinessException(403, message);
        }
    }

    public void assertNotLocked(String username, UserType type, String ip, String userAgent) {
        String key = lockKey(username, type);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            String message = "登录失败次数过多，请 5 分钟后再试";
            loginLogService.recordFailure(username, type.name(), ip, userAgent, message, null);
            throw new BusinessException(423, message);
        }
    }

    public LoginFailureAction recordFailure(String username, UserType type, String ip, String userAgent,
                                            String message, Long userId) {
        String failKey = failKey(username, type);
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
        if (failCount != null && failCount == 1L) {
            stringRedisTemplate.expire(failKey, Duration.ofSeconds(Constants.LOGIN_FAIL_TTL_SECONDS));
        }

        Long ipFailCount = null;
        if (ip != null && !ip.isBlank()) {
            String ipKey = ipFailKey(ip);
            ipFailCount = stringRedisTemplate.opsForValue().increment(ipKey);
            if (ipFailCount != null && ipFailCount == 1L) {
                stringRedisTemplate.expire(ipKey, Duration.ofSeconds(Constants.LOGIN_IP_FAIL_TTL_SECONDS));
            }
        }

        boolean blacklisted = (failCount != null && failCount >= Constants.LOGIN_BLACKLIST_THRESHOLD)
                || (ipFailCount != null && ipFailCount >= Constants.LOGIN_BLACKLIST_THRESHOLD);
        if (blacklisted) {
            blacklistService.autoBlockIp(ip, "来源 IP 连续登录失败达到 10 次，系统自动加入永久系统黑名单");
            stringRedisTemplate.delete(lockKey(username, type));
            String blockedMessage = "登录失败次数过多，当前 IP 已加入系统黑名单";
            loginLogService.recordFailure(username, type.name(), ip, userAgent, blockedMessage, userId);
            return LoginFailureAction.BLACKLISTED;
        }

        if (failCount != null && failCount >= Constants.MAX_LOGIN_FAIL_COUNT) {
            stringRedisTemplate.opsForValue().set(
                    lockKey(username, type), "1", Duration.ofSeconds(Constants.LOGIN_LOCK_SECONDS));
            String lockedMessage = "登录失败次数过多，请 5 分钟后再试";
            loginLogService.recordFailure(username, type.name(), ip, userAgent, lockedMessage, userId);
            return LoginFailureAction.LOCKED;
        }

        loginLogService.recordFailure(username, type.name(), ip, userAgent, message, userId);
        return LoginFailureAction.NORMAL;
    }

    public void recordSuccess(String username, UserType type, String ip, String userAgent, Long userId) {
        stringRedisTemplate.delete(failKey(username, type));
        stringRedisTemplate.delete(lockKey(username, type));
        if (ip != null && !ip.isBlank()) {
            stringRedisTemplate.delete(ipFailKey(ip));
        }
        loginLogService.recordSuccess(username, type.name(), ip, userAgent, userId);
    }

    public void logFailureOnly(String username, UserType type, String ip, String userAgent,
                               String message, Long userId) {
        loginLogService.recordFailure(username, type.name(), ip, userAgent, message, userId);
    }

    private String failKey(String username, UserType type) {
        return Constants.LOGIN_FAIL_KEY_PREFIX + type.name().toLowerCase() + ":" + username;
    }

    private String lockKey(String username, UserType type) {
        return Constants.LOGIN_LOCK_KEY_PREFIX + type.name().toLowerCase() + ":" + username;
    }

    private String ipFailKey(String ip) {
        return Constants.LOGIN_IP_FAIL_KEY_PREFIX + ip;
    }
}
