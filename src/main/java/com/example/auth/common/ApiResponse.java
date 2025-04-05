package com.example.auth.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private int status;
    private T data;

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, 200, data); // 기본 200 OK
    }

    public static <T> ApiResponse<T> success(T data, String message, int status) {
        return new ApiResponse<>(true, message, status, data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, 500, null); // 기본 500 오류
    }

    public static <T> ApiResponse<T> fail(String message, int status) {
        return new ApiResponse<>(false, message, status, null);
    }
    public static <T> ApiResponse<T> error(String message, int status) {
        return new ApiResponse<>(false, message, status, null);
    }

}
