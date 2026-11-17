package com.livingframe.business.vo;

public record BizUserActivityVO(
        long totalUsers,
        long onlineUsers,
        long todayActiveUsers,
        long sevenDayActiveUsers,
        long newUsersToday,
        long disabledUsers,
        long blacklistedUsers,
        double onlineRate,
        double todayActiveRate
) {
}
