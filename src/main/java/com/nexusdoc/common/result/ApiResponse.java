package com.nexusdoc.common.result;

import lombok.Data;

@Data
public class ApiResponse<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(ResultCode.SUCCESS.getCode());
        response.setMessage(ResultCode.SUCCESS.getMessage());
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(ResultCode.FAILED.getCode());
        response.setMessage(message);
        response.setData(null);
        return response;
    }

    public static <T> ApiResponse<T> fail(ResultCode resultCode, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(resultCode.getCode());
        response.setMessage(message);
        response.setData(null);
        return response;
    }
}
