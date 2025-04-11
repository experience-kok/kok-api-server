package com.example.auth.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 토큰 재발급 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponseDTO {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "토큰이 성공적으로 재발급되었습니다.")
    private String message;

    @Schema(description = "HTTP 상태 코드", example = "200")
    private int status;

    @Schema(description = "응답 데이터")
    private TokenRefreshDataDTO data;
}
