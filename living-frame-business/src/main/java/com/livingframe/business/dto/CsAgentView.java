package com.livingframe.business.dto;

public record CsAgentView(
        Long id,
        Long userId,
        String name,
        String serviceType,
        String onlineStatus,
        Long currentTicketId
) {
}
