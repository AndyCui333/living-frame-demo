package com.livingframe.common.context;

import com.livingframe.common.api.ResultCode;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.common.model.LoginUser;

/**
 * 当前请求的登录用户上下文。
 */
public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
