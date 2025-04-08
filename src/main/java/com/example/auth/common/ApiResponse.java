package com.example.auth.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

public class ApiResponse {

@JsonInclude(JsonInclude.Include.NON_NULL)
public static class Success<T> {
    private final boolean success = true;
    private final String message;
    private final int status;
    private final T data;

    private Success(String message, int status, T data) {
        this.message = message;
        this.status = status;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
public static class Error {
    private final boolean success = false;
    private final String message;
    private final String errorCode;
    private final int status;

    private Error(String message, String errorCode, int status) {
        this.message = message;
        this.errorCode = errorCode;
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatus() {
        return status;
    }
}

/**
 * 성공 응답 생성 (200 OK)
 */
public static <T> Success<T> success(T data, String message) {
    return new Success<>(message, HttpStatus.OK.value(), data);
}

/**
 * 성공 응답 생성 (상태 코드 지정)
 */
public static <T> Success<T> success(T data, String message, int status) {
    return new Success<>(message, status, data);
}

/**
 * 오류 응답 생성 (500 서버 오류)
 */
public static Error fail(String message) {
    return new Error(message, "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value());
}

/**
 * 오류 응답 생성 (상태 코드 지정)
 */
public static Error fail(String message, int status) {
    return new Error(message, "INTERNAL_ERROR", status);
}

/**
 * 오류 응답 생성 (오류 코드와 상태 코드 지정)
 */
public static Error fail(String message, String errorCode, int status) {
    return new Error(message, errorCode, status);
}
}