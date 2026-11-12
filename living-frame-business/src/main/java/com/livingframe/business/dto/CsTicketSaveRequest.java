package com.livingframe.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CsTicketSaveRequest(
        @NotBlank(message = "工单标题不能为空")
        @Size(max = 200, message = "工单标题过长")
        String title,

        @NotBlank(message = "问题描述不能为空")
        @Size(max = 10000, message = "问题描述过长")
        String content,

        @NotBlank(message = "请选择客服类型")
        @Pattern(regexp = "CHAT|TECH", message = "客服类型不合法")
        String ticketType
) {
}
