package com.example.auth.constant;

/**
 * 찜 대상 타입
 * 현재는 캠페인만 지원
 */
public enum BookmarkTargetType {
    CAMPAIGN("캠페인");

    private final String description;

    BookmarkTargetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return this.name();
    }
}
