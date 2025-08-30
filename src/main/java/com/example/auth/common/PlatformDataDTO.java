package com.example.auth.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformDataDTO {
    @Schema(description = "플랫폼 ID", example = "1")
    private Long id;

    @Schema(description = "플랫폼 유형", example = "BLOG",
            allowableValues = {"BLOG", "INSTAGRAM", "YOUTUBE", "TIKTOK"})
    private String platformType;

    @Schema(description = "계정 URL", example = "https://myblog.com")
    private String accountUrl;

    @Schema(description = "팔로워 수", example = "12345", nullable = true)
    private Integer followerCount;

    @Schema(description = "마지막 크롤링 일시", example = "2025-04-13T14:30:15", nullable = true)
    private LocalDateTime lastCrawledAt;
}