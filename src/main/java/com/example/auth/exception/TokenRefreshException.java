package com.example.auth.exception;

import lombok.Getter;

/**
 * 토큰 재발급 과정에서 발생하는 예외
 */
@Getter
public class TokenRefreshException extends RuntimeException {

    private final String errorCode;

    public TokenRefreshException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}