package com.livingframe.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 4000, message = "消息内容不能超过 4000 字")
        String message,

        @Size(max = 64, message = "会话标识过长")
        String conversationId
) {
}
