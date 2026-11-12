package com.livingframe.common.constant;

/**
 * 全局常量。
 */
public final class Constants {

    private Constants() {
    }

    /** sa-token token session 中保存 LoginUser 的 key。 */
    public static final String LOGIN_USER_KEY = "loginUser";
    /** 验证码 Redis key 前缀。 */
    public static final String CAPTCHA_KEY_PREFIX = "living-frame:captcha:";
    /** 登录失败计数 Redis key 前缀。 */
    public static final String LOGIN_FAIL_KEY_PREFIX = "living-frame:login:fail:";
    /** 登录失败锁定 Redis key 前缀。 */
    public static final String LOGIN_LOCK_KEY_PREFIX = "living-frame:login:lock:";
    /** 登录 IP 失败计数 Redis key 前缀。 */
    public static final String LOGIN_IP_FAIL_KEY_PREFIX = "living-frame:login:ip-fail:";
    /** 管理员登录 ID 前缀。 */
    public static final String ADMIN_LOGIN_ID_PREFIX = "admin_";
    /** 业务用户登录 ID 前缀。 */
    public static final String BUSINESS_LOGIN_ID_PREFIX = "biz_";

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String ROLE_BUSINESS_ADMIN = "BUSINESS_ADMIN";
    public static final String ROLE_VIEWER = "VIEWER";
    public static final String ROLE_BUSINESS_USER = "BIZ_USER";

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    public static final int MAX_LOGIN_FAIL_COUNT = 5;
    public static final int LOGIN_BLACKLIST_THRESHOLD = 10;
    public static final long LOGIN_LOCK_SECONDS = 5 * 60L;
    public static final long LOGIN_FAIL_TTL_SECONDS = 30 * 60L;
    public static final long LOGIN_IP_FAIL_TTL_SECONDS = 60 * 60L;
    public static final long CAPTCHA_TTL_SECONDS = 60L;

    public static final long TOKEN_TIMEOUT_SECONDS = 7 * 24 * 60 * 60L;
}
