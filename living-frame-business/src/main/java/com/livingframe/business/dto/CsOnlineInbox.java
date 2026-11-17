package com.livingframe.business.dto;

import java.util.List;

public record CsOnlineInbox(
        int activeCount,
        int capacity,
        long unreadCount,
        Long earliestUnreadTicketId,
        List<CsTicketView> unreadTickets
) {
}
