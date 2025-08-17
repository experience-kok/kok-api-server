package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 토큰 재발급 요청 DTO
 */
@Getter
@Setter
@Schema(description = "토큰 재발급 요청 - 만료된 액세스 토큰을 새로 발급받을 때 사용", example = """
    {
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMiLCJpYXQiOjE2NzAyNjUyMDAsImV4cCI6MTY3MTEyOTIwMH0.def456"
    }
    """)
public class RefreshTokenRequest {

    @Schema(description = "리프레시 토큰 - 로그인 시 발급받은 장기 유효 토큰 (액세스 토큰 재발급용, 일반적으로 7일 유효)", 
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            required = true)
    @NotBlank(message = "리프레시 토큰은 필수 입력값입니다.")
    private String refreshToken;
}
