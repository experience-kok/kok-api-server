package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class BlogConnectRequest {
    
    @Schema(description = "네이버 블로그 URL", 
            example = "https://blog.naver.com/blogid",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotBlank(message = "블로그 URL은 필수입니다")
    private String blogUrl;
}
