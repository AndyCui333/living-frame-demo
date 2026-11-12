package com.livingframe.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PermissionUpdateRequest(
        @NotBlank(message = "权限名称不能为空")
        @Size(max = 64, message = "权限名称长度不能超过 64")
        String name,

        @Pattern(regexp = "^(MENU|BUTTON)$", message = "权限类型只能为 MENU 或 BUTTON")
        String type,

        Long parentId,

        @Size(max = 255, message = "路由地址长度不能超过 255")
        String path,

        @Size(max = 255, message = "组件地址长度不能超过 255")
        String component,

        @Size(max = 64, message = "图标长度不能超过 64")
        String icon,

        Integer sort,

        @Min(value = 0, message = "状态只能为 0 或 1")
        @Max(value = 1, message = "状态只能为 0 或 1")
        Integer status
) {
}
