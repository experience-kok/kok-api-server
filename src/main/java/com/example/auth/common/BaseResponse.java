package com.example.auth.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@Schema(description = "API 응답의 기본 형식 - 모든 API가 일관된 구조로 응답하기 위한 표준 래퍼 클래스")
public class BaseResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "성공 응답 - API 호출이 정상적으로 처리된 경우의 응답 구조", example = """
        {
          "success": true,
          "message": "요청이 성공적으로 처리되었습니다.",
          "status": 200,
          "data": {
            "id": 123,
            "name": "예시 데이터",
            "createdAt": "2025-07-10T15:30:00Z"
          }
        }
        """)
    @Hidden
    public static class Success<T> {
        @Schema(description = "성공 여부 - 항상 true", example = "true", required = true)
        private final boolean success = true;

        @Schema(description = "응답 메시지 - 처리 결과에 대한 사용자 친화적 메시지", example = "요청이 성공적으로 처리되었습니다.", required = true)
        private final String message;

        @Schema(description = "HTTP 상태 코드 - 응답의 HTTP 상태", example = "200", required = true)
        private final int status;

        @Schema(description = "응답 데이터 - 실제 반환되는 비즈니스 데이터 (null일 수 있음)")
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
    @Schema(description = "오류 응답 - API 호출 중 오류가 발생한 경우의 응답 구조", example = """
        {
          "success": false,
          "message": "요청을 처리하는 중 오류가 발생했습니다.",
          "errorCode": "VALIDATION_ERROR",
          "status": 400
        }
        """)
    @Hidden
    public static class Error {
        @Schema(description = "성공 여부 - 항상 false", example = "false", required = true)
        private final boolean success = false;

        @Schema(description = "오류 메시지 - 사용자에게 표시할 수 있는 친화적인 오류 설명", example = "요청을 처리하는 중 오류가 발생했습니다.", required = true)
        private final String message;

        @Schema(description = "오류 코드 - 개발자가 오류 유형을 식별할 수 있는 코드", example = "VALIDATION_ERROR", required = true, allowableValues = {
                "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND", 
                "TOKEN_EXPIRED", "TOKEN_INVALID", "VALIDATION_ERROR",
                "DUPLICATE_DATA", "INTERNAL_ERROR"
        })
        private final String errorCode;
        
        @Schema(description = "HTTP 상태 코드 - 오류에 해당하는 HTTP 상태", example = "400", required = true)
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