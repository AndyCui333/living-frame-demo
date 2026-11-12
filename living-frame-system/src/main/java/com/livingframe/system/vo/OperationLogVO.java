package com.livingframe.system.vo;

public record OperationLogVO(
        Long id,
        Long userId,
        String username,
        String operation,
        String method,
        String requestUri,
        String ip,
        String status,
        String errorMessage,
        Long durationMs,
        String createdAt
) {
}
