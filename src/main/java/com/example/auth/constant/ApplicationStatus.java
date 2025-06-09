package com.example.auth.constant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 캠페인 신청 상태를 나타내는 열거형
 */
@Schema(description = "캠페인 신청 상태")
public enum ApplicationStatus {
    
    @Schema(description = "신청 접수 (기본값)")
    PENDING("신청 접수"),
    
    @Schema(description = "선정됨")
    APPROVED("선정됨"),
    
    @Schema(description = "거절됨")
    REJECTED("거절됨"),
    
    @Schema(description = "체험 및 리뷰 완료")
    COMPLETED("완료됨");
    
    private final String description;
    
    ApplicationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 문자열에서 ApplicationStatus로 변환
     * 대소문자 구분 없이 변환 가능
     */
    public static ApplicationStatus fromString(String status) {
        if (status == null) {
            return PENDING; // 기본값
        }
        
        try {
            return ApplicationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING; // 잘못된 값인 경우 기본값 반환
        }
    }
    
    /**
     * 대문자 문자열로 반환
     */
    @Override
    public String toString() {
        return this.name();
    }
}
