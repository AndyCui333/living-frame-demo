package com.livingframe.common.event;

/**
 * 后台用户删除事件；AI 模块据此清理该用户的模型配置。
 */
public record UserDeletedEvent(Long userId) {
}
