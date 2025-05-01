package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GoogleAuthRequest {

    @Schema(description = "인증 코드", example = "4/P7q7W91a-oMsCeLvIaQm6bTrgtp7",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "인증 코드는 필수입니다")
    private String authorizationCode;

    @Schema(description = "리다이렉트 URI", example = "http://localhost:3000/login/oauth2/code/google",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "리다이렉트 URI는 필수입니다")
    private String redirectUri;
    
    // 하위 호환성을 위한 별칭 메서드
    public String getCode() {
        return authorizationCode;
    }
}