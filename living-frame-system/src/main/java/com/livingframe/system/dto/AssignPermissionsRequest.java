package com.livingframe.system.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignPermissionsRequest(
        @NotNull(message = "权限列表不能为空")
        List<Long> permissionIds
) {
}
