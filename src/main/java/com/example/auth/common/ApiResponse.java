package com.example.auth.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private int status;
    private T data;

    // 성공 응답 (데이터 있음)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, HttpStatus.OK.value(), data);
    }

    // 성공 응답 (데이터 없음)
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, HttpStatus.OK.value(), null);
    }

    // 성공 응답 (커스텀 상태코드)
    public static <T> ApiResponse<T> success(String message, T data, HttpStatus status) {
        return new ApiResponse<>(true, message, status.value(), data);
    }

    // 실패 응답
    public static ApiResponse<Void> error(String message, HttpStatus status) {
        return new ApiResponse<>(false, message, status.value(), null);
    }

    // 실패 응답 (기본 400)
    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, HttpStatus.BAD_REQUEST.value(), null);
    }
}
