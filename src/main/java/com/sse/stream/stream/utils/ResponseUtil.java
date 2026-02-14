package com.sse.stream.stream.utils;

public class ResponseUtil {

    public static <T> ApiResponse<T> success(T data, String msg) {
        return new ApiResponse<>(true, msg, data);
    }

    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(false, msg, null);
    }
}

