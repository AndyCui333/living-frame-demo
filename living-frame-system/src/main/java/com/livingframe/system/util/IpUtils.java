package com.livingframe.system.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 请求信息提取工具。
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = firstValid(
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getHeader("Proxy-Client-IP"),
                request.getHeader("WL-Proxy-Client-IP"));
        if (StrUtil.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        return truncate(ip, 64);
    }

    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return truncate(request.getHeader("User-Agent"), 512);
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static String firstValid(String... values) {
        for (String value : values) {
            if (StrUtil.isBlank(value) || UNKNOWN.equalsIgnoreCase(value.trim())) {
                continue;
            }
            String first = value.split(",")[0].trim();
            if (StrUtil.isNotBlank(first) && !UNKNOWN.equalsIgnoreCase(first)) {
                return first;
            }
        }
        return "";
    }
}
