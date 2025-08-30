package com.example.auth.constant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(enumAsRef = true, description = "SNS Platform Type")
public enum PlatformType {
    @Schema(description = "Instagram")
    INSTAGRAM("instagram", "인스타그램"),

    @Schema(description = "YouTube")
    YOUTUBE("youtube", "유튜브"),

    @Schema(description = "Naver Blog")
    BLOG("blog", "네이버 블로그"),

    @Schema(description = "TikTok")
    TIKTOK("tiktok", "틱톡");

    private final String value;
    private final String koreanName;

    PlatformType(String value, String koreanName) {
        this.value = value;
        this.koreanName = koreanName;
    }

    /**
     * Find PlatformType by string value
     */
    public static PlatformType fromString(String value) {
        for (PlatformType type : PlatformType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown platform type: " + value);
    }
}