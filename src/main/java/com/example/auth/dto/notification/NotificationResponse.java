package com.example.auth.dto.notification;

import com.example.auth.domain.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    /**
     * 알림 ID
     */
    private Long notificationId;
    
    /**
     * 알림 타입
     */
    private String notificationType;
    
    /**
     * 알림 타입 설명
     */
    private String notificationTypeDescription;
    
    /**
     * 알림 제목
     */
    private String title;
    
    /**
     * 알림 내용
     */
    private String message;
    
    /**
     * 관련 엔티티 ID (캠페인 ID 등)
     */
    private Long relatedEntityId;
    
    /**
     * 관련 엔티티 타입 (CAMPAIGN, APPLICATION 등)
     */
    private String relatedEntityType;
    
    /**
     * 읽음 여부
     */
    private boolean isRead;
    
    /**
     * 알림 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 읽은 시간
     */
    private LocalDateTime readAt;
    
    /**
     * Notification 엔티티로부터 DTO 생성
     */
    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .notificationType(notification.getNotificationType().name())
                .notificationTypeDescription(notification.getNotificationType().getDescription())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
