package com.example.auth.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 알림 읽음 처리 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReadRequest {
    
    /**
     * 읽음 처리할 알림 ID 목록
     * null이면 모든 알림을 읽음 처리
     */
    private List<Long> notificationIds;
}
