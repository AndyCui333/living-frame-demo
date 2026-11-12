package com.livingframe.business.dto;

import java.util.List;

public record CsTicketDetail(
        CsTicketView ticket,
        List<CsMessageView> messages
) {
}
