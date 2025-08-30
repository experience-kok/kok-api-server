package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformResponse {
    @Schema(description = "플랫폼 ID", example = "1")
    private Long id;

    @Schema(description = "플랫폼 유형", example = "BLOG",
            allowableValues = {"BLOG", "INSTAGRAM", "YOUTUBE", "TIKTOK"})
    private String platformType;

    @Schema(description = "계정 URL", example = "https://myblog.com")
    private String accountUrl;

    @Schema(description = "팔로워 수", example = "1000", nullable = true)
    private Integer followerCount;

    @Schema(description = "마지막 크롤링 일시", nullable = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCrawledAt;

    @Schema(description = "생성일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}