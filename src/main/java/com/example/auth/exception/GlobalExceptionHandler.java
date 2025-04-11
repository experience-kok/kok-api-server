package com.example.auth.exception;

import com.example.auth.common.BaseResponse;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ 401 전용 헬퍼
    private ResponseEntity<BaseResponse.Error> unauthorized(String message, String code) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.fail(message, code, HttpStatus.UNAUTHORIZED.value()));
    }

    // ✅ 토큰 관련 예외
    @ExceptionHandler(JwtValidationException.class)
    public ResponseEntity<?> handleJwtValidation(JwtValidationException ex) {
        log.warn("JWT 검증 오류: {}, 타입: {}", ex.getMessage(), ex.getErrorType());

        String errorCode = switch (ex.getErrorType()) {
            case EXPIRED -> "TOKEN_EXPIRED";
            case REFRESH_INVALID -> "INVALID_REFRESH_TOKEN";
            default -> "UNAUTHORIZED";
        };

        return unauthorized(ex.getMessage(), errorCode);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex) {
        log.warn("인증 오류: {}", ex.getMessage());
        return unauthorized(ex.getMessage(), "UNAUTHORIZED");
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpired(ExpiredJwtException ex) {
        log.warn("토큰 만료: {}", ex.getMessage());
        return unauthorized("토큰이 만료되었습니다.", "TOKEN_EXPIRED");
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<?> handleTokenRefresh(TokenRefreshException ex) {
        log.warn("토큰 재발급 오류: {}, 코드: {}", ex.getMessage(), ex.getErrorCode());

        HttpStatus status;
        if ("INVALID_REFRESH_TOKEN".equals(ex.getErrorCode())) {
            status = HttpStatus.UNAUTHORIZED;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(status)
                .body(BaseResponse.fail(ex.getMessage(), ex.getErrorCode(), status.value()));
    }

    // ✅ 유효성 검증
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        String errorMessage = result.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("유효성 검증 오류: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.fail(errorMessage, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST.value()));
    }

    // ✅ 기타 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex) {
        log.error("서버 오류: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
