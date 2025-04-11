package com.example.auth.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformDataDTO {
    @Schema(description = "플랫폼 ID", example = "1")
    private Long id;

    @Schema(description = "플랫폼 유형", example = "BLOG",
            allowableValues = {"BLOG", "INSTAGRAM", "YOUTUBE"})
    private String platformType;

    @Schema(description = "계정 URL", example = "https://myblog.com")
    private String accountUrl;

    @Schema(description = "계정 이름", example = "나의 블로그")
    private String accountName;

    @Schema(description = "인증 여부", example = "false")
    private Boolean verified;
}
