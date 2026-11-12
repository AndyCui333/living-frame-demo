package com.livingframe.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64, message = "角色名称长度不能超过 64")
        String name,

        @Size(max = 255, message = "角色描述长度不能超过 255")
        String description,

        Integer sort,

        @Min(value = 0, message = "状态只能为 0 或 1")
        @Max(value = 1, message = "状态只能为 0 或 1")
        Integer status
) {
}
