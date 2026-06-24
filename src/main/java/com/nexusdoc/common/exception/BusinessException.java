package com.nexusdoc.common.exception;

import com.nexusdoc.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(String message) {
        super(message);
        this.resultCode = ResultCode.BAD_REQUEST;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
