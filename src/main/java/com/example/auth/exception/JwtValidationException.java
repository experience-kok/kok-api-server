    package com.example.auth.exception;

    import com.example.auth.exception.TokenErrorType;

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
