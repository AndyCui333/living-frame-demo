package com.livingframe.business.dto;

public record CsRankingView(
        int rank,
        Long agentUserId,
        String name,
        String serviceType,
        long processedCount,
        double averageRating,
        long ratingCount
) {
}
