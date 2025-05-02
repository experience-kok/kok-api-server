package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConnectRequest {

    @NotNull(message = "플랫폼 타입은 필수입니다")
    @Pattern(regexp = "BLOG|INSTAGRAM|YOUTUBE", message = "플랫폼 타입은 BLOG, INSTAGRAM, YOUTUBE 중 하나여야 합니다")
    private String type;

    @NotBlank(message = "URL은 필수입니다")
    @Pattern(regexp = "https?://.*", message = "유효한 URL 형식이어야 합니다")
    private String url;
}
