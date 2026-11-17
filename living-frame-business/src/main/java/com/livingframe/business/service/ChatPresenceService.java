package com.livingframe.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 客服/门户当前正在查看的工单，用于避免用户正在阅读会话时再次生成通知。
 */
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private static final String PORTAL_KEY_PREFIX = "cs:portal-active:";
    private static final String AGENT_KEY_PREFIX = "cs:agent-active:";
    private static final Duration PORTAL_TTL = Duration.ofSeconds(90);
    private static final Duration AGENT_TTL = Duration.ofSeconds(35);

    private final StringRedisTemplate stringRedisTemplate;

    public void touchPortal(Long userId, Long ticketId) {
        if (userId == null || ticketId == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(PORTAL_KEY_PREFIX + userId, ticketId.toString(), PORTAL_TTL);
    }

    public boolean isPortalViewing(Long userId, Long ticketId) {
        if (userId == null || ticketId == null) {
            return false;
        }
        String active = stringRedisTemplate.opsForValue().get(PORTAL_KEY_PREFIX + userId);
        return ticketId.toString().equals(active);
    }

    public void leavePortal(Long userId) {
        if (userId != null) {
            stringRedisTemplate.delete(PORTAL_KEY_PREFIX + userId);
        }
    }

    public void touchAgent(Long userId, Long ticketId) {
        if (userId == null || ticketId == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(AGENT_KEY_PREFIX + userId, ticketId.toString(), AGENT_TTL);
    }

    public Long activeAgentTicket(Long userId) {
        if (userId == null) {
            return null;
        }
        String active = stringRedisTemplate.opsForValue().get(AGENT_KEY_PREFIX + userId);
        if (active == null) {
            return null;
        }
        try {
            return Long.valueOf(active);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public void leaveAgent(Long userId) {
        if (userId != null) {
            stringRedisTemplate.delete(AGENT_KEY_PREFIX + userId);
        }
    }
}
