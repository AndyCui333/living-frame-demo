package com.livingframe.ai.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livingframe.ai.config.AiMemoryProperties;
import com.livingframe.ai.dto.AiConversationDetail;
import com.livingframe.ai.dto.AiConversationView;
import com.livingframe.ai.dto.AiMessageView;
import com.livingframe.ai.entity.AiConversation;
import com.livingframe.ai.entity.AiMessage;
import com.livingframe.ai.mapper.AiConversationMapper;
import com.livingframe.ai.mapper.AiMessageMapper;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.event.UserDeletedEvent;
import com.livingframe.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话与消息：MySQL 持久化，Redis 缓存最近消息，Qdrant 保存语义记忆。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationService {

    private static final String CACHE_PREFIX = "living-frame:ai:chat:";
    private static final int CACHE_SIZE = 20;

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiMemoryProperties memoryProperties;
    private final AiQdrantMemoryService memoryService;

    public Long resolveConversation(String conversationId, Long userId, String firstMessage) {
        if (StrUtil.isBlank(conversationId)) {
            return createConversation(userId, firstMessage);
        }
        Long id;
        try {
            id = Long.valueOf(conversationId);
        } catch (NumberFormatException ex) {
            throw new BusinessException("会话标识不合法");
        }
        AiConversation conversation = conversationMapper.selectOne(Wrappers.<AiConversation>lambdaQuery()
                .eq(AiConversation::getId, id)
                .eq(AiConversation::getUserId, userId));
        if (conversation == null) {
            throw new BusinessException("会话不存在或无权访问");
        }
        return id;
    }

    public List<AiConversationView> listConversations() {
        Long userId = LoginUserContext.require().getId();
        return conversationMapper.selectList(Wrappers.<AiConversation>lambdaQuery()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getLastMessageAt)
                        .orderByDesc(AiConversation::getId)
                        .last("LIMIT 50"))
                .stream()
                .map(item -> new AiConversationView(
                        item.getId(),
                        item.getTitle(),
                        item.getMessageCount() == null ? 0 : item.getMessageCount(),
                        item.getLastMessageAt()))
                .toList();
    }

    public AiConversationDetail detail(Long conversationId) {
        Long userId = LoginUserContext.require().getId();
        AiConversation conversation = requireOwned(conversationId, userId);
        return new AiConversationDetail(
                conversation.getId(),
                conversation.getTitle(),
                messagesFromDb(conversationId, 200));
    }

    public List<AiMessageView> recentMessages(Long conversationId, int limit) {
        String key = cacheKey(conversationId);
        try {
            List<String> cached = redisTemplate.opsForList().range(key, 0, -1);
            if (cached != null && !cached.isEmpty()) {
                List<AiMessageView> messages = new ArrayList<>();
                for (String item : cached) {
                    messages.add(objectMapper.readValue(item, AiMessageView.class));
                }
                return messages;
            }
        } catch (Exception ex) {
            log.debug("读取 AI 会话 Redis 缓存失败，降级 MySQL: {}", ex.getMessage());
        }
        List<AiMessageView> messages = messagesFromDb(conversationId, limit);
        warmCache(conversationId, messages);
        return messages;
    }

    @Transactional(rollbackFor = Exception.class)
    public void append(Long conversationId, Long userId, String role, String content, String model) {
        AiMessage message = new AiMessage()
                .setConversationId(conversationId)
                .setUserId(userId)
                .setRole(role)
                .setContent(content)
                .setModel(model);
        messageMapper.insert(message);
        conversationMapper.update(null, Wrappers.<AiConversation>lambdaUpdate()
                .eq(AiConversation::getId, conversationId)
                .setSql("message_count = message_count + 1")
                .set(AiConversation::getLastMessageAt, LocalDateTime.now()));
        appendCache(conversationId, new AiMessageView(
                message.getId(), role, content, message.getCreatedAt()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long conversationId) {
        Long userId = LoginUserContext.require().getId();
        requireOwned(conversationId, userId);
        messageMapper.delete(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
        try {
            redisTemplate.delete(cacheKey(conversationId));
        } catch (Exception ex) {
            log.debug("删除 AI 会话 Redis 缓存失败: {}", ex.getMessage());
        }
        memoryService.deleteConversation(conversationId);
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onUserDeleted(UserDeletedEvent event) {
        try {
            List<AiConversation> conversations = conversationMapper.selectList(
                    Wrappers.<AiConversation>lambdaQuery()
                            .eq(AiConversation::getUserId, event.userId()));
            for (AiConversation conversation : conversations) {
                memoryService.deleteConversation(conversation.getId());
            }
            messageMapper.delete(Wrappers.<AiMessage>lambdaQuery()
                    .eq(AiMessage::getUserId, event.userId()));
            conversationMapper.delete(Wrappers.<AiConversation>lambdaQuery()
                    .eq(AiConversation::getUserId, event.userId()));
        } catch (Exception ex) {
            log.error("清理用户 AI 会话失败: userId={}", event.userId(), ex);
        }
    }

    private Long createConversation(Long userId, String firstMessage) {
        LocalDateTime now = LocalDateTime.now();
        AiConversation conversation = new AiConversation()
                .setUserId(userId)
                .setTitle(titleFrom(firstMessage))
                .setMessageCount(0)
                .setLastMessageAt(now);
        conversationMapper.insert(conversation);
        return conversation.getId();
    }

    private AiConversation requireOwned(Long conversationId, Long userId) {
        AiConversation conversation = conversationMapper.selectOne(Wrappers.<AiConversation>lambdaQuery()
                .eq(AiConversation::getId, conversationId)
                .eq(AiConversation::getUserId, userId));
        if (conversation == null) {
            throw new BusinessException("会话不存在或无权访问");
        }
        return conversation;
    }

    private List<AiMessageView> messagesFromDb(Long conversationId, int limit) {
        List<AiMessage> rows = messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200))));
        Collections.reverse(rows);
        return rows.stream()
                .map(item -> new AiMessageView(
                        item.getId(), item.getRole(), item.getContent(), item.getCreatedAt()))
                .toList();
    }

    private void warmCache(Long conversationId, List<AiMessageView> messages) {
        if (messages.isEmpty()) {
            return;
        }
        try {
            String key = cacheKey(conversationId);
            redisTemplate.delete(key);
            List<String> values = new ArrayList<>();
            for (AiMessageView message : messages) {
                values.add(objectMapper.writeValueAsString(message));
            }
            redisTemplate.opsForList().rightPushAll(key, values);
            redisTemplate.opsForList().trim(key, -CACHE_SIZE, -1);
            redisTemplate.expire(key, Duration.ofHours(memoryProperties.getRedisTtlHours()));
        } catch (Exception ex) {
            log.debug("写入 AI 会话 Redis 缓存失败: {}", ex.getMessage());
        }
    }

    private void appendCache(Long conversationId, AiMessageView message) {
        try {
            String key = cacheKey(conversationId);
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                // 首次写入时从 MySQL 回填完整最近上下文，保证 Redis 与会话同步
                warmCache(conversationId, messagesFromDb(conversationId, CACHE_SIZE));
                return;
            }
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(message));
            redisTemplate.opsForList().trim(key, -CACHE_SIZE, -1);
            redisTemplate.expire(key, Duration.ofHours(memoryProperties.getRedisTtlHours()));
        } catch (Exception ex) {
            log.debug("追加 AI 会话 Redis 缓存失败: {}", ex.getMessage());
        }
    }

    private String cacheKey(Long conversationId) {
        return CACHE_PREFIX + conversationId;
    }

    private String titleFrom(String message) {
        String title = StrUtil.blankToDefault(message, "新对话")
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return title.length() > 40 ? title.substring(0, 40) + "…" : title;
    }
}
