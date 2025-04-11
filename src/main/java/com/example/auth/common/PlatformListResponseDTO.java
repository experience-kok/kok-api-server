package com.example.auth.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 플랫폼 목록 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformListResponseDTO {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "SNS 플랫폼 목록을 성공적으로 조회했습니다.")
    private String message;

    @Schema(description = "HTTP 상태 코드", example = "200")
    private int status;

    @Schema(description = "응답 데이터 (플랫폼 목록)")
    private PlatformDataDTO[] data;
}