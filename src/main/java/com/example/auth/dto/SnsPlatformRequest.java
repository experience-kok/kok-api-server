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
            allowableValues = {"BLOG", "INSTAGRAM", "YOUTUBE"},
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "플랫폼 타입은 필수입니다")
    @Pattern(regexp = "^(BLOG|INSTAGRAM|YOUTUBE)$", message = "플랫폼 타입은 BLOG, INSTAGRAM, YOUTUBE 중 하나여야 합니다")
    private String platformType;

    @Schema(description = "계정 URL", example = "https://myblog.com",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "계정 URL은 필수입니다")
    private String accountUrl;

    @Schema(description = "계정 이름", example = "나의 블로그",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "계정 이름은 필수입니다")
    private String accountName;

    @Schema(description = "인증 여부 (무시됨, 항상 false로 설정됨)", example = "false",
            hidden = true,
            nullable = true)
    private Boolean verified;
}