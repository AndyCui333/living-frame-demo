package com.livingframe.business.dto;

import java.time.LocalDateTime;

public record CsTicketView(
        Long id,
        String ticketNo,
        String title,
        String content,
        String ticketType,
        String status,
        String portalUserName,
        Long assignedAgentId,
        String assignedAgentName,
        Integer rating,
        String feedback,
        LocalDateTime assignedAt,
        LocalDateTime lastMessageAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
