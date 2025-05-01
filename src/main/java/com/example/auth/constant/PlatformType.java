package com.example.auth.constant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "SNS 플랫폼 유형")
public enum PlatformType {
    @Schema(description = "네이버 블로그")
    BLOG("blog", "네이버 블로그"),

    @Schema(description = "페이스북")
    FACEBOOK("facebook", "페이스북"),

    @Schema(description = "인스타그램")
    INSTAGRAM("instagram", "인스타그램"),

    @Schema(description = "유튜브")
    YOUTUBE("youtube", "유튜브");

    private final String value;
    private final String displayName;

    PlatformType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PlatformType fromString(String value) {
        for (PlatformType type : PlatformType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("알 수 없는 플랫폼 유형: " + value);
    }
}