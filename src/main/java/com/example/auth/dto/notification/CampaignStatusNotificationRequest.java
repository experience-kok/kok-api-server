package com.example.auth.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캠페인 상태 알림 요청 DTO (관리자 프로젝트에서 호출용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatusNotificationRequest {
    
    /**
     * 캠페인 생성자(클라이언트) ID
     */
    private Long userId;
    
    /**
     * 캠페인 ID
     */
    private Long campaignId;
    
    /**
     * 캠페인 제목
     */
    private String campaignTitle;
    
    /**
     * 승인 상태 (APPROVED, REJECTED)
     */
    private String approvalStatus;
    
    /**
     * 관리자 코멘트 (거절 사유 등)
     */
    private String adminComment;
    
    /**
     * 승인한 관리자 ID
     */
    private Long adminId;
}
