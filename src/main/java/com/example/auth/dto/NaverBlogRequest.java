package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NaverBlogRequest {
    
    @Schema(description = "네이버 블로그 URL", 
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false,
            example = "https://blog.naver.com/yourblogid")
    @NotBlank(message = "블로그 URL은 필수입니다")
    @Pattern(regexp = "^https?://blog\\.naver\\.com/[^/]+/?.*$", message = "유효한 네이버 블로그 URL이 아닙니다")
    private String blogUrl;
}
