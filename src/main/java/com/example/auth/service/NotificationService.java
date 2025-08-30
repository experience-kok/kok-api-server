package com.example.auth.service;

import com.example.auth.domain.Notification;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.dto.notification.CampaignStatusNotificationRequest;
import com.example.auth.dto.notification.NotificationResponse;
import com.example.auth.dto.notification.NotificationSummaryResponse;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 서비스
 * 실시간 알림 전송 및 알림 관리를 담당합니다.
 * SSE(Server-Sent Events)를 통한 실시간 알림 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    /**
     * 사용자의 알림 목록 조회
     */
    public PageResponse<NotificationResponse> getUserNotifications(Long userId, String type, int page, int size) {
        log.info("사용자 알림 목록 조회: userId={}, type={}, page={}, size={}", userId, type, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage;

        switch (type.toLowerCase()) {
            case "unread":
                notificationPage = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
                break;
            case "read":
                notificationPage = notificationRepository.findByUserIdAndIsReadTrueOrderByCreatedAtDesc(userId, pageable);
                break;
            default:
                notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<NotificationResponse>builder()
                .content(responses)
                .pageNumber(notificationPage.getNumber() + 1)
                .pageSize(notificationPage.getSize())
                .totalPages(notificationPage.getTotalPages())
                .totalElements(notificationPage.getTotalElements())
                .first(notificationPage.isFirst())
                .last(notificationPage.isLast())
                .build();
    }

    /**
     * 사용자의 알림 요약 정보 조회
     */
    public NotificationSummaryResponse getUserNotificationSummary(Long userId) {
        log.info("사용자 알림 요약 조회: userId={}", userId);

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        boolean isConnected = sseEmitterService.isConnected(userId.toString());

        return NotificationSummaryResponse.builder()
                .unreadCount(unreadCount)
                .notificationEnabled(true)  // 추후 사용자 설정으로 변경 가능
                .realtimeConnected(isConnected)    // 실제 SSE 연결 상태
                .build();
    }

    /**
     * 알림을 읽음 처리
     */
    @Transactional
    public void markNotificationsAsRead(Long userId, List<Long> notificationIds) {
        log.info("알림 읽음 처리: userId={}, notificationIds={}", userId, notificationIds);

        LocalDateTime readAt = LocalDateTime.now();

        if (notificationIds == null || notificationIds.isEmpty()) {
            // 모든 알림 읽음 처리
            int updatedCount = notificationRepository.markAllAsReadByUserId(userId, readAt);
            log.info("모든 알림 읽음 처리 완료: userId={}, updatedCount={}", userId, updatedCount);
        } else {
            // 특정 알림들만 읽음 처리
            int updatedCount = notificationRepository.markAsReadByIds(notificationIds, userId, readAt);
            log.info("특정 알림 읽음 처리 완료: userId={}, notificationIds={}, updatedCount={}", 
                    userId, notificationIds, updatedCount);
        }

        // 실시간으로 읽음 상태 업데이트 전송
        sendNotificationSummaryUpdate(userId);
    }

    /**
     * 캠페인 상태 변경 알림 생성 및 전송 (관리자 프로젝트에서 호출)
     */
    @Transactional
    public void sendCampaignStatusNotification(CampaignStatusNotificationRequest request) {
        log.info("캠페인 상태 알림 생성: userId={}, campaignId={}, status={}", 
                request.getUserId(), request.getCampaignId(), request.getApprovalStatus());

        // 알림 내용 생성
        String title;
        String message;
        Notification.NotificationType notificationType;

        if ("APPROVED".equals(request.getApprovalStatus())) {
            notificationType = Notification.NotificationType.CAMPAIGN_APPROVED;
            title = "캠페인이 승인되었습니다";
            message = String.format("'%s' 캠페인이 승인되었습니다. 이제 인플루언서들이 신청할 수 있습니다.", 
                    request.getCampaignTitle());
        } else if ("REJECTED".equals(request.getApprovalStatus())) {
            notificationType = Notification.NotificationType.CAMPAIGN_REJECTED;
            title = "캠페인이 거절되었습니다";
            message = String.format("'%s' 캠페인이 거절되었습니다.", request.getCampaignTitle());
            
            if (request.getAdminComment() != null && !request.getAdminComment().trim().isEmpty()) {
                message += "\n거절 사유: " + request.getAdminComment();
            }
        } else {
            log.warn("알 수 없는 승인 상태: {}", request.getApprovalStatus());
            return;
        }

        // 알림 저장
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .notificationType(notificationType)
                .title(title)
                .message(message)
                .relatedEntityId(request.getCampaignId())
                .relatedEntityType("CAMPAIGN")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // SSE 실시간 알림 전송
        sendRealtimeNotification(request.getUserId(), savedNotification);
        
        log.info("캠페인 상태 알림 전송 완료: notificationId={}", savedNotification.getId());
    }

    /**
     * SSE를 통한 실시간 알림 전송
     */
    private void sendRealtimeNotification(Long userId, Notification notification) {
        try {
            NotificationResponse response = NotificationResponse.fromEntity(notification);
            
            // SSE를 통해 특정 사용자에게 알림 전송
            sseEmitterService.sendNotification(userId.toString(), response, "notification");
            
            log.info("SSE 실시간 알림 전송 성공: userId={}, notificationId={}", userId, notification.getId());
            
        } catch (Exception e) {
            log.error("SSE 실시간 알림 전송 실패: userId={}, notificationId={}, error={}", 
                    userId, notification.getId(), e.getMessage(), e);
        }
    }

    /**
     * 알림 요약 정보 업데이트 전송
     */
    private void sendNotificationSummaryUpdate(Long userId) {
        try {
            NotificationSummaryResponse summary = getUserNotificationSummary(userId);
            
            // SSE를 통해 알림 요약 정보 전송
            sseEmitterService.sendNotificationSummary(userId.toString(), summary);
            
            log.info("SSE 알림 요약 업데이트 전송 성공: userId={}", userId);
            
        } catch (Exception e) {
            log.error("SSE 알림 요약 업데이트 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 인플루언서 선정 알림 생성 및 전송
     */
    @Transactional
    public void sendInfluencerSelectedNotification(Long userId, String campaignTitle) {
        log.info("인플루언서 선정 알림 생성: userId={}, campaignTitle={}", userId, campaignTitle);

        String title = "인플루언서로 선정되었습니다!";
        String message = String.format("축하합니다! '%s' 캠페인의 인플루언서로 선정되셨습니다. 이제 미션을 수행해주세요.", campaignTitle);

        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(Notification.NotificationType.CAMPAIGN_SELECTED)
                .title(title)
                .message(message)
                .relatedEntityId(null) // 필요시 campaignId 추가
                .relatedEntityType("CAMPAIGN")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        sendRealtimeNotification(userId, savedNotification);
        
        log.info("인플루언서 선정 알림 전송 완료: notificationId={}", savedNotification.getId());
    }

    /**
     * 인플루언서 거절 알림 생성 및 전송
     */
    @Transactional
    public void sendInfluencerRejectedNotification(Long userId, String campaignTitle) {
        log.info("인플루언서 거절 알림 생성: userId={}, campaignTitle={}", userId, campaignTitle);

        String title = "캠페인 선정 결과 안내";
        String message = String.format("'%s' 캠페인의 선정 결과를 안내드립니다. 아쉽게도 이번 캠페인에는 선정되지 않으셨습니다. 더 좋은 기회로 다시 만날 수 있기를 기대합니다.", campaignTitle);

        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(Notification.NotificationType.CAMPAIGN_NOT_SELECTED)
                .title(title)
                .message(message)
                .relatedEntityId(null) // 필요시 campaignId 추가
                .relatedEntityType("CAMPAIGN")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        sendRealtimeNotification(userId, savedNotification);
        
        log.info("인플루언서 거절 알림 전송 완료: notificationId={}", savedNotification.getId());
    }

    /**
     * 캠페인 선정 알림 생성 및 전송 (선정된 사용자용)
     */
    @Transactional
    public void sendCampaignSelectionNotification(Long userId, Long campaignId, String campaignTitle, String additionalMessage) {
        log.info("캠페인 선정 알림 생성: userId={}, campaignId={}, campaignTitle={}", userId, campaignId, campaignTitle);

        String title = "캠페인에 선정되었습니다!";
        String message = String.format("축하합니다! '%s' 캠페인에 선정되셨습니다.", campaignTitle);
        
        if (additionalMessage != null && !additionalMessage.trim().isEmpty()) {
            message += "\n\n" + additionalMessage;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(Notification.NotificationType.CAMPAIGN_SELECTED)
                .title(title)
                .message(message)
                .relatedEntityId(campaignId)
                .relatedEntityType("CAMPAIGN")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        sendRealtimeNotification(userId, savedNotification);
        
        log.info("캠페인 선정 알림 전송 완료: notificationId={}", savedNotification.getId());
    }

    /**
     * 캠페인 미선정 알림 생성 및 전송 (미선정된 사용자용)
     */
    @Transactional
    public void sendCampaignNotSelectedNotification(Long userId, Long campaignId, String campaignTitle, String additionalMessage) {
        log.info("캠페인 미선정 알림 생성: userId={}, campaignId={}, campaignTitle={}", userId, campaignId, campaignTitle);

        String title = "캠페인 선정 결과 안내";
        String message = String.format("'%s' 캠페인의 선정 결과를 안내드립니다. 아쉽게도 이번 캠페인에는 선정되지 않으셨습니다.", campaignTitle);
        
        if (additionalMessage != null && !additionalMessage.trim().isEmpty()) {
            message += "\n\n" + additionalMessage;
        } else {
            message += "\n\n더 좋은 기회로 다시 만날 수 있기를 기대합니다.";
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(Notification.NotificationType.CAMPAIGN_NOT_SELECTED)
                .title(title)
                .message(message)
                .relatedEntityId(campaignId)
                .relatedEntityType("CAMPAIGN")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        sendRealtimeNotification(userId, savedNotification);
        
        log.info("캠페인 미선정 알림 전송 완료: notificationId={}", savedNotification.getId());
    }

    /**
     * 캠페인 신청 접수 알림 생성 및 전송 (신청자용)
     */
    @Transactional
    public void sendCampaignApplicationReceivedNotification(Long userId, Long campaignId, String campaignTitle, boolean isAlwaysOpen) {
        log.info("캠페인 신청 접수 알림 생성: userId={}, campaignId={}, campaignTitle={}, isAlwaysOpen={}", 
                userId, campaignId, campaignTitle, isAlwaysOpen);

        String title;
        String message;
        
        if (isAlwaysOpen) {
            title = "상시 캠페인 신청 완료";
            message = String.format("'%s' 상시 캠페인 신청이 완료되었습니다. 바로 선정 대기 상태로 전환되었어요. 선정 결과를 기다려주세요.", campaignTitle);
        } else {
            title = "캠페인 신청이 접수되었습니다";
            message = String.format("'%s' 캠페인 신청이 정상적으로 접수되었습니다. 선정 결과를 기다려주세요.", campaignTitle);
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(Notification.NotificationType.CAMPAIGN_APPLICATION_RECEIVED)
                .title(title)
                .message(message)
                .relatedEntityId(campaignId)
                .relatedEntityType("CAMPAIGN")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        sendRealtimeNotification(userId, savedNotification);
        
        log.info("캠페인 신청 접수 알림 전송 완료: notificationId={}", savedNotification.getId());
    }

    /**
     * 캠페인 신청 접수 알림 생성 및 전송 (기존 메서드 - 호환성 유지)
     */
    @Transactional
    public void sendCampaignApplicationReceivedNotification(Long userId, Long campaignId, String campaignTitle) {
        sendCampaignApplicationReceivedNotification(userId, campaignId, campaignTitle, false);
    }

    /**
     * 일반 알림 생성 및 전송
     */
    @Transactional
    public void sendNotification(Long userId, Notification.NotificationType type, String title, String message, 
                                 Long relatedEntityId, String relatedEntityType) {
        log.info("일반 알림 생성: userId={}, type={}, title={}", userId, type, title);

        Notification notification = Notification.builder()
                .userId(userId)
                .notificationType(type)
                .title(title)
                .message(message)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        sendRealtimeNotification(userId, savedNotification);
        
        log.info("일반 알림 전송 완료: notificationId={}", savedNotification.getId());
    }

    /**
     * 특정 알림 조회
     */
    public NotificationResponse getNotification(Long userId, Long notificationId) {
        log.info("알림 조회: userId={}, notificationId={}", userId, notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다. ID: " + notificationId));

        // 본인의 알림인지 확인
        if (!notification.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("접근할 수 없는 알림입니다.");
        }

        return NotificationResponse.fromEntity(notification);
    }

    /**
     * 오래된 읽은 알림 정리 (스케줄러에서 호출)
     */
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);  // 30일 이전 읽은 알림 삭제
        int deletedCount = notificationRepository.deleteOldReadNotifications(cutoffDate);
        log.info("오래된 알림 정리 완료: deletedCount={}", deletedCount);
    }
}
