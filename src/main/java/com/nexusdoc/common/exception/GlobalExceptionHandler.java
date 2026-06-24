package com.nexusdoc.common.exception;

import com.nexusdoc.common.result.ApiResponse;
import com.nexusdoc.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        log.warn("业务异常：{}", exception.getMessage());
        return ApiResponse.fail(exception.getResultCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().isEmpty()
                ? "请求参数错误"
                : exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验失败：{}", message);
        return ApiResponse.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return ApiResponse.fail(ResultCode.FAILED, ResultCode.FAILED.getMessage());
    }
}
