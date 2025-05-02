package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @deprecated 대신 {@link PlatformConnectRequest}를 사용하세요.
 */
@Deprecated
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InstagramConnectRequest {
    @NotBlank(message = "인스타그램 URL을 입력해주세요")
    @Pattern(regexp = "^https?://(www\\.)?instagram\\.com/[^/]+/?.*$", 
            message = "올바른 인스타그램 URL 형식이 아닙니다")
    private String instagramUrl;
}
