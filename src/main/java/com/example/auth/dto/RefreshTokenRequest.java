package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 토큰 재발급 요청 DTO
 */
@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = "리프레시 토큰은 필수 입력값입니다.")
    private String refreshToken;
}