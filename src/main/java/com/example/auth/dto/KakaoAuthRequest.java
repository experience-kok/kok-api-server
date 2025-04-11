package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 카카오 로그인 요청 DTO
 */
@Getter
@Setter
@ToString
public class KakaoAuthRequest {

    @Schema(description = "카카오로부터 받은 인가 코드", example = "0987654321abcdefghijk",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "인가 코드는 필수 입력값입니다.")
    private String authorizationCode;

    @Schema(description = "리다이렉트 URI",
            example = "http://localhost:3000/login/oauth2/code/kakao",
            allowableValues = {"http://localhost:3000/login/oauth2/code/kakao", "https://ckok.kr/login/oauth2/code/kakao"},
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "리다이렉트 URI는 필수 입력값입니다.")
    private String redirectUri;
}