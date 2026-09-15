package com.example.form.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应结构.
 *
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "ok",
 *   "data": {...}
 * }
 * </pre>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 成功码 */
    public static final int CODE_SUCCESS = 0;
    /** 通用失败码 */
    public static final int CODE_FAIL = 1;
    /** 未认证 */
    public static final int CODE_UNAUTHORIZED = 401;
    /** 无权限 */
    public static final int CODE_FORBIDDEN = 403;

    private int code;
    private String msg;
    private T data;

    public Result() {
    }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(CODE_SUCCESS, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_SUCCESS, "ok", data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(CODE_FAIL, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> unauthorized(String msg) {
        return new Result<>(CODE_UNAUTHORIZED, msg, null);
    }

    public static <T> Result<T> forbidden(String msg) {
        return new Result<>(CODE_FORBIDDEN, msg, null);
    }
}
