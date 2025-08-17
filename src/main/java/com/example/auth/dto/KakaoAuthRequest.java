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
@Schema(description = "카카오 로그인 요청 - 카카오 OAuth 인증을 통한 로그인/회원가입 처리를 위한 데이터", example = """
    {
      "authorizationCode": "0987654321abcdefghijk",
      "redirectUri": "http://localhost:3000/login/oauth2/code/kakao"
    }
    """)
public class KakaoAuthRequest {

    @Schema(description = "카카오 OAuth 인가 코드 - 카카오 로그인 후 콜백으로 받은 임시 인가 코드 (access_token 교환용)", 
            example = "0987654321abcdefghijk",
            required = true)
    @NotBlank(message = "인가 코드는 필수 입력값입니다.")
    private String authorizationCode;

    @Schema(description = "리다이렉트 URI - 카카오 앱 설정에 등록된 콜백 URL (환경별로 다름)", 
            example = "http://localhost:3000/login/oauth2/code/kakao",
            allowableValues = {"http://localhost:3000/login/oauth2/code/kakao", "https://chkok.kr/login/oauth2/code/kakao"},
            required = true)
    @NotBlank(message = "리다이렉트 URI는 필수 입력값입니다.")
    private String redirectUri;
}
