package com.livingframe.common.api;

import lombok.Getter;

/**
 * 统一错误码。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据已存在"),
    ERROR(500, "服务器内部错误"),
    AI_UNAVAILABLE(502, "AI 服务暂时不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
