package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NaverAuthRequest {
    
    @Schema(description = "네이버 OAuth 인증 코드", 
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "인증 코드는 필수입니다")
    private String code;
}
