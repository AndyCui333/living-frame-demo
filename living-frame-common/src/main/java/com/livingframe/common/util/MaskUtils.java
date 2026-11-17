package com.livingframe.common.util;

import cn.hutool.core.util.StrUtil;

/**
 * 敏感信息脱敏工具。仅在返回前端时使用，不修改数据库中的原始值。
 */
public final class MaskUtils {

    private MaskUtils() {
    }

    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return phone;
        }
        String value = phone.trim();
        if (value.length() >= 7) {
            return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
        }
        if (value.length() >= 3) {
            return value.charAt(0) + "****" + value.charAt(value.length() - 1);
        }
        return "****";
    }

    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0 || at == value.length() - 1) {
            return value;
        }
        String local = value.substring(0, at);
        String domain = value.substring(at);
        if (local.length() >= 4) {
            return local.substring(0, 2) + "****" + local.charAt(local.length() - 1) + domain;
        }
        if (local.length() >= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return "***" + domain;
    }

    public static boolean isMasked(String value) {
        return value != null && value.indexOf('*') >= 0;
    }
}
