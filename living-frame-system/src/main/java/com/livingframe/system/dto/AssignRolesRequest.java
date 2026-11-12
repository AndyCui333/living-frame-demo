package com.livingframe.system.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignRolesRequest(
        @NotNull(message = "角色列表不能为空")
        List<Long> roleIds
) {
}
