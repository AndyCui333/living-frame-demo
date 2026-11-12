package com.livingframe.business.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CsTicketFeedbackRequest(
        @NotNull(message = "请选择评分")
        @Min(value = 1, message = "评分最低为 1")
        @Max(value = 5, message = "评分最高为 5")
        Integer rating,

        @Size(max = 1000, message = "评价内容过长")
        String feedback
) {
}
