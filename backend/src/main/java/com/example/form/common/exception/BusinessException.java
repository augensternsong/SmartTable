package com.example.form.common.exception;

import com.example.form.common.Result;
import lombok.Getter;

/**
 * 业务异常, 用于显式抛出可预期的错误信息(如"模板编码已存在").
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = Result.CODE_FAIL;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
