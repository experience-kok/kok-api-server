package com.example.auth.constant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "SNS 플랫폼 유형")
public enum PlatformType {
    @Schema(description = "블로그")
    BLOG("BLOG"),

    @Schema(description = "인스타그램")
    INSTAGRAM("INSTAGRAM"),

    @Schema(description = "유튜브")
    YOUTUBE("YOUTUBE");

    private final String value;

    PlatformType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
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