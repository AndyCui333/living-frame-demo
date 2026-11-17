package com.livingframe.business.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.business.entity.BizNotification;
import com.livingframe.business.mapper.BizNotificationMapper;
import com.livingframe.business.vo.BizNotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BizNotificationService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizNotificationMapper notificationMapper;

    @Transactional(rollbackFor = Exception.class)
    public void create(Long userId, String type, String title, String content, String targetType, Long targetId) {
        if (userId == null) {
            return;
        }
        // 同一目标如果已经有一条未读通知，不再重复生成，避免连续客服消息刷屏。
        Long existing = notificationMapper.selectCount(Wrappers.<BizNotification>lambdaQuery()
                .eq(BizNotification::getUserId, userId)
                .eq(BizNotification::getTargetType, targetType)
                .eq(BizNotification::getTargetId, targetId)
                .eq(BizNotification::getReadStatus, 0));
        if (existing != null && existing > 0) {
            return;
        }
        notificationMapper.insert(new BizNotification()
                .setUserId(userId)
                .setType(type)
                .setTitle(title)
                .setContent(StrUtil.maxLength(content, 480))
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setReadStatus(0));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markTargetRead(Long userId, String targetType, Long targetId) {
        notificationMapper.update(null, Wrappers.<BizNotification>lambdaUpdate()
                .eq(BizNotification::getUserId, userId)
                .eq(BizNotification::getTargetType, targetType)
                .eq(BizNotification::getTargetId, targetId)
                .eq(BizNotification::getReadStatus, 0)
                .set(BizNotification::getReadStatus, 1));
    }

    public List<BizNotificationVO> list(Long userId, String filter) {
        var wrapper = Wrappers.<BizNotification>lambdaQuery()
                .eq(BizNotification::getUserId, userId)
                .orderByDesc(BizNotification::getCreatedAt)
                .orderByDesc(BizNotification::getId)
                .last("LIMIT 100");
        if ("unread".equalsIgnoreCase(filter)) {
            wrapper.eq(BizNotification::getReadStatus, 0);
        } else if ("read".equalsIgnoreCase(filter)) {
            wrapper.eq(BizNotification::getReadStatus, 1);
        }
        return notificationMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public long unreadCount(Long userId) {
        Long count = notificationMapper.selectCount(Wrappers.<BizNotification>lambdaQuery()
                .eq(BizNotification::getUserId, userId)
                .eq(BizNotification::getReadStatus, 0));
        return count == null ? 0 : count;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long id) {
        notificationMapper.update(null, Wrappers.<BizNotification>lambdaUpdate()
                .eq(BizNotification::getId, id)
                .eq(BizNotification::getUserId, userId)
                .set(BizNotification::getReadStatus, 1));
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        notificationMapper.update(null, Wrappers.<BizNotification>lambdaUpdate()
                .eq(BizNotification::getUserId, userId)
                .eq(BizNotification::getReadStatus, 0)
                .set(BizNotification::getReadStatus, 1));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        notificationMapper.delete(Wrappers.<BizNotification>lambdaQuery()
                .eq(BizNotification::getUserId, userId)
                .in(BizNotification::getId, ids));
    }

    private BizNotificationVO toVO(BizNotification notification) {
        return new BizNotificationVO(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType(),
                notification.getTargetId(),
                Integer.valueOf(1).equals(notification.getReadStatus()),
                notification.getCreatedAt() == null ? null : FORMATTER.format(notification.getCreatedAt()));
    }
}
