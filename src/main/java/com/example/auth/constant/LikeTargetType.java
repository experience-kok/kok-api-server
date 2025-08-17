package com.example.auth.constant;

/**
 * 좋아요 대상 타입
 * 현재는 캠페인만 지원
 */
public enum LikeTargetType {
    CAMPAIGN("캠페인");

    private final String description;

    LikeTargetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return this.name();
    }
}
