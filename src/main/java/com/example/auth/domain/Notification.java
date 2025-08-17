package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 엔티티
 * 사용자에게 전송되는 다양한 알림 정보를 저장합니다.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림 수신자 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 알림 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    /**
     * 알림 제목
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 알림 내용
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 관련 엔티티 ID (캠페인 ID, 신청 ID 등)
     */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /**
     * 관련 엔티티 타입 (CAMPAIGN, APPLICATION 등)
     */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    /**
     * 읽음 여부
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 알림 생성 시간
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 읽은 시간
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 알림 타입 열거형
     */
    public enum NotificationType {
        CAMPAIGN_APPROVED("캠페인 승인"),
        CAMPAIGN_REJECTED("캠페인 거절"),
        CAMPAIGN_APPLICATION_RECEIVED("캠페인 신청 접수"),
        CAMPAIGN_APPLICATION_APPROVED("캠페인 신청 승인"),
        CAMPAIGN_APPLICATION_REJECTED("캠페인 신청 거절"),
        CAMPAIGN_SELECTED("캠페인 선정"),
        CAMPAIGN_NOT_SELECTED("캠페인 미선정"),
        CAMPAIGN_DEADLINE_REMINDER("캠페인 마감 임박"),
        CAMPAIGN_REVIEW_DEADLINE_REMINDER("리뷰 제출 마감 임박"),
        SYSTEM_NOTICE("시스템 공지");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
    }

    /**
     * 알림을 읽음 처리합니다.
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * 알림을 읽지 않음 처리합니다.
     */
    public void markAsUnread() {
        this.isRead = false;
        this.readAt = null;
    }
}
