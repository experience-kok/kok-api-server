package com.example.auth.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 플랫폼 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformResponseDTO {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "SNS 플랫폼 정보를 성공적으로 조회했습니다.")
    private String message;

    @Schema(description = "HTTP 상태 코드", example = "200")
    private int status;

    @Schema(description = "응답 데이터")
    private PlatformDataDTO data;
}
