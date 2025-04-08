package com.example.auth.exception;

import lombok.Getter;

@Getter
public class TokenRefreshException extends RuntimeException {

    private final String errorCode;

    public TokenRefreshException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    // 자주 쓰이는 에러코드용 static 메서드 제공
    public static TokenRefreshException invalidRefreshToken() {
        return new TokenRefreshException("유효하지 않은 리프레시 토큰입니다.", "INVALID_REFRESH_TOKEN");
    }

    public static TokenRefreshException tokenRefreshError(String message) {
        return new TokenRefreshException(message, "TOKEN_REFRESH_ERROR");
    }
}
