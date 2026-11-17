package com.livingframe.business.vo;

import java.util.List;

public record BizUserStatisticsVO(
        long totalUsers,
        long enabledUsers,
        long disabledUsers,
        long onlineUsers,
        long newUsersToday,
        long newUsersSevenDays,
        long blacklistedUsers,
        List<DailyNew> trend,
        List<StatusSlice> statusDistribution
) {
    public record DailyNew(String date, long count) {
    }

    public record StatusSlice(String name, long value) {
    }
}
