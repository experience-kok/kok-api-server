package com.example.auth.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 오류 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {
    @Schema(description = "성공 여부 (항상 false)", example = "false")
    private boolean success;

    @Schema(description = "오류 메시지", example = "유효하지 않은 토큰입니다.")
    private String message;

    @Schema(description = "오류 코드", example = "UNAUTHORIZED",
            allowableValues = {"UNAUTHORIZED", "TOKEN_EXPIRED", "INVALID_REFRESH_TOKEN",
                    "VALIDATION_ERROR", "RESOURCE_NOT_FOUND", "INTERNAL_ERROR"})
    private String errorCode;

    @Schema(description = "HTTP 상태 코드", example = "401")
    private int status;
}
