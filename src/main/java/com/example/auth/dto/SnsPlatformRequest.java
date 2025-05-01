package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SnsPlatformRequest {

    @Schema(description = "플랫폼 유형", example = "BLOG",
            allowableValues = {"BLOG", "FACEBOOK", "INSTAGRAM", "YOUTUBE"},
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "플랫폼 타입은 필수입니다")
    @Pattern(regexp = "^(BLOG|FACEBOOK|INSTAGRAM|YOUTUBE)$", message = "플랫폼 타입은 BLOG, FACEBOOK, INSTAGRAM, YOUTUBE 중 하나여야 합니다")
    private String platformType;

    @Schema(description = "계정 URL", example = "https://myblog.com",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "계정 URL은 필수입니다")
    private String accountUrl;
}