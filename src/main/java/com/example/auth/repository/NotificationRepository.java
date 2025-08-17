package com.example.auth.repository;

import com.example.auth.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 Repository
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 알림 목록 조회 (최신순)
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 목록 조회
     */
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자의 읽은 알림 목록 조회
     */
    Page<Notification> findByUserIdAndIsReadTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 개수 조회
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 사용자의 특정 타입 알림 조회
     */
    Page<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
            Long userId, Notification.NotificationType notificationType, Pageable pageable);

    /**
     * 사용자의 모든 알림을 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    /**
     * 특정 알림들을 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id IN :notificationIds AND n.userId = :userId")
    int markAsReadByIds(@Param("notificationIds") List<Long> notificationIds, 
                        @Param("userId") Long userId, 
                        @Param("readAt") LocalDateTime readAt);

    /**
     * 오래된 읽은 알림 삭제 (데이터 정리용)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.readAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 사용자의 알림 개수 제한 (최신 N개만 유지)
     */
    @Query("SELECT n.id FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Long> findNotificationIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 엔티티 관련 알림 조회
     */
    List<Notification> findByRelatedEntityIdAndRelatedEntityType(Long relatedEntityId, String relatedEntityType);

    /**
     * 사용자별 최근 N일간의 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.createdAt >= :fromDate ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate);

    /**
     * 실시간 알림 대상자 조회 (WebSocket 연결된 사용자 확인용)
     */
    @Query("SELECT DISTINCT n.userId FROM Notification n WHERE n.createdAt >= :recentTime")
    List<Long> findActiveUserIds(@Param("recentTime") LocalDateTime recentTime);
}
