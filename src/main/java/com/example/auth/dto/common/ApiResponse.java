package com.example.auth.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 통합 API 응답 DTO
 * V1/V2 공통으로 사용되는 표준 응답 포맷
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 응답 표준 포맷")
public class ApiResponse<T> {
    
    @Schema(description = "성공 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean success;
    
    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
    
    @Schema(description = "HTTP 상태 코드", example = "200", requiredMode = Schema.RequiredMode.REQUIRED)
    private int status;
    
    @Schema(description = "응답 데이터")
    private T data;
    
    @Schema(description = "오류 코드 (오류 응답 시에만 포함)", example = "VALIDATION_ERROR")
    private String errorCode;

    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data, String message, int status) {
        return new ApiResponse<>(true, message, status, data, null);
    }

    /**
     * 성공 응답 생성 (기본 메시지)
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "요청이 성공적으로 처리되었습니다.", 200);
    }

    /**
     * 오류 응답 생성
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, int status) {
        return new ApiResponse<>(false, message, status, null, errorCode);
    }

    /**
     * 오류 응답 생성 (기본 상태코드)
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return error(message, errorCode, 500);
    }
}
