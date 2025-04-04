package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 카카오 로그인 요청 DTO
 */
@Getter
@Setter
public class KakaoAuthRequest {

    @NotBlank(message = "인가 코드는 필수 입력값입니다.")
    private String authorizationCode;

    @NotBlank(message = "리다이렉트 URI는 필수 입력값입니다.")
    private String redirectUri;
}