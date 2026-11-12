package com.livingframe.business.dto;

import jakarta.validation.constraints.NotNull;

public record CsTicketTransferRequest(
        @NotNull(message = "请选择转接客服")
        Long targetAgentId
) {
}
