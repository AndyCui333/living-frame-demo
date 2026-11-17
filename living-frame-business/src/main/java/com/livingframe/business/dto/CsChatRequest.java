package com.livingframe.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CsChatRequest(
        Long ticketId,

        @NotBlank(message = "消息内容不能为空")
        @Size(max = 5000, message = "消息内容过长")
        String content,

        @Pattern(regexp = "CHAT|TECH", message = "客服类型不合法")
        String ticketType
) {
}
