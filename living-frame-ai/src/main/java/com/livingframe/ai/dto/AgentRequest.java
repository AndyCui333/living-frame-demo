package com.livingframe.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRequest(
        @NotBlank(message = "任务内容不能为空")
        @Size(max = 4000, message = "任务内容不能超过 4000 字")
        String task
) {
}
