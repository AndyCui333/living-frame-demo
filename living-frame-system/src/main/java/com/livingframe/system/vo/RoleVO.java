package com.livingframe.system.vo;

public record RoleVO(
        Long id,
        String code,
        String name,
        String description,
        Integer status,
        Integer sort,
        String createdAt
) {
}
