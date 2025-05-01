package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class InstagramAuthRequest {
    
    @Schema(description = "페이스북 OAuth 인증 코드", 
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "인증 코드는 필수입니다")
    private String code;
    
    @Schema(description = "인스타그램 ID (사용자 아이디)", 
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false,
            example = "instagram_username")
    @NotBlank(message = "인스타그램 ID는 필수입니다")
    private String instagramId;
}
