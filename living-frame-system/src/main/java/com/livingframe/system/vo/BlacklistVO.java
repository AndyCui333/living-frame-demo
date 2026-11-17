package com.livingframe.system.vo;

public record BlacklistVO(
        Long id,
        String targetType,
        String targetValue,
        String userScope,
        String reason,
        Integer status,
        String createdBy
) {
}
