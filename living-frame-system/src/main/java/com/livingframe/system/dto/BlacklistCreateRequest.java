package com.livingframe.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BlacklistCreateRequest(
        @NotBlank(message = "黑名单类型不能为空")
        @Pattern(regexp = "^(IP|USERNAME)$", message = "黑名单类型只能为 IP 或 USERNAME")
        String targetType,

        @NotBlank(message = "黑名单目标不能为空")
        @Size(max = 128, message = "黑名单目标过长")
        String targetValue,

        @NotBlank(message = "请填写加入黑名单的完整原因")
        @Size(max = 500, message = "原因过长")
        String reason,

        @Min(value = 0, message = "状态只能为 0 或 1")
        @Max(value = 1, message = "状态只能为 0 或 1")
        Integer status
) {
}
