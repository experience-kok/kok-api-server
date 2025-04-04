package com.example.auth.exception;

public class JwtValidationException extends RuntimeException {
    private final TokenErrorType errorType;

    public JwtValidationException(String message, TokenErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public TokenErrorType getErrorType() {
        return errorType;
    }
}
