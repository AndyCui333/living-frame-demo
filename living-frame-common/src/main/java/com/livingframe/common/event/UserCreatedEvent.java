package com.livingframe.common.event;

/**
 * 后台用户创建完成事件；AI 模块据此为新用户初始化默认模型配置。
 */
public record UserCreatedEvent(Long userId) {
}
