package com.example.auth.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 요약 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryResponse {
    
    /**
     * 읽지 않은 알림 개수
     */
    private long unreadCount;
    
    /**
     * 알림 기능 활성화 여부
     */
    private boolean notificationEnabled;
    
    /**
     * 실시간 알림 연결 상태
     */
    private boolean realtimeConnected;
}
