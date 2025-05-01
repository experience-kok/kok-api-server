package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class FacebookAuthRequest {

    @Schema(description = "인증 코드", example = "AQDTdRJUlVFIMkhB...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "인증 코드는 필수입니다")
    private String authorizationCode;

    @Schema(description = "리다이렉트 URI", example = "http://localhost:3000/login/oauth2/code/facebook",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "리다이렉트 URI는 필수입니다")
    private String redirectUri;
    
    // 하위 호환성을 위한 별칭 메서드
    public String getCode() {
        return authorizationCode;
    }
}