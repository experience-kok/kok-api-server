package com.example.auth.constant;

/**
 * 알림 타입을 정의하는 열거형
 * Admin 서비스와 동일하게 유지
 */
public enum NotificationType {
    // 캠페인 관련 알림
    CAMPAIGN_APPROVED("캠페인 승인"),
    CAMPAIGN_REJECTED("캠페인 거절"),
    CAMPAIGN_COMPLETED("캠페인 완료"),
    CAMPAIGN_SUBMITTED("캠페인 제출"),
    CAMPAIGN_APPLICATION_RECEIVED("캠페인 신청 접수"),
    CAMPAIGN_APPLICATION_APPROVED("캠페인 신청 승인"),
    CAMPAIGN_APPLICATION_REJECTED("캠페인 신청 거절"),
    CAMPAIGN_SELECTED("캠페인 선정"),
    CAMPAIGN_NOT_SELECTED("캠페인 미선정"),
    CAMPAIGN_DEADLINE_REMINDER("캠페인 마감 임박"),
    CAMPAIGN_REVIEW_DEADLINE_REMINDER("리뷰 제출 마감 임박"),
    
    // 신청 관련 알림
    APPLICATION_APPROVED("신청 승인"),
    APPLICATION_REJECTED("신청 거절"),
    
    // 결제 관련 알림
    PAYMENT_COMPLETED("결제 완료"),
    
    // 리뷰 관련 알림
    REVIEW_REMINDER("리뷰 알림"),
    REVIEW_SUBMITTED("리뷰 제출"),
    REVIEW_APPROVED("리뷰 승인"),
    
    // 시스템 관련 알림
    SYSTEM_NOTICE("시스템 공지"),
    EVENT_NOTIFICATION("이벤트 알림"),
    MAINTENANCE_NOTICE("점검 공지");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
