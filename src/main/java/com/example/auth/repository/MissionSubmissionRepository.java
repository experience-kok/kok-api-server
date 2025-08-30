package com.example.auth.repository;

import com.example.auth.domain.CampaignApplication;
import com.example.auth.domain.MissionSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MissionSubmissionRepository extends JpaRepository<MissionSubmission, Long> {

    /**
     * 캠페인 신청별 미션 제출 조회
     */
    Optional<MissionSubmission> findByCampaignApplicationId(Long campaignApplicationId);

    /**
     * 캠페인별 모든 미션 제출 조회 (클라이언트용)
     */
    @Query("SELECT ms FROM MissionSubmission ms " +
           "JOIN FETCH ms.campaignApplication ca " +
           "JOIN FETCH ca.user u " +
           "JOIN FETCH ca.campaign c " +
           "WHERE c.id = :campaignId " +
           "ORDER BY ms.submittedAt DESC")
    List<MissionSubmission> findByCampaignId(@Param("campaignId") Long campaignId);

    /**
     * 캠페인별 미션 제출 페이징 조회
     */
    @Query("SELECT ms FROM MissionSubmission ms " +
           "JOIN FETCH ms.campaignApplication ca " +
           "JOIN FETCH ca.user u " +
           "WHERE ca.campaign.id = :campaignId " +
           "ORDER BY ms.submittedAt DESC")
    Page<MissionSubmission> findByCampaignIdWithPaging(@Param("campaignId") Long campaignId, Pageable pageable);


    /**
     * 특정 유저의 미션 제출 이력 조회
     */
    @Query("SELECT ms FROM MissionSubmission ms " +
           "JOIN FETCH ms.campaignApplication ca " +
           "JOIN FETCH ca.campaign c " +
           "WHERE ca.user.id = :userId " +
           "ORDER BY ms.submittedAt DESC")
    List<MissionSubmission> findByUserId(@Param("userId") Long userId);

    /**
     * 특정 유저의 미션 제출 이력 페이징 조회
     */
    @Query("SELECT ms FROM MissionSubmission ms " +
           "JOIN ms.campaignApplication ca " +
           "WHERE ca.user.id = :userId " +
           "ORDER BY ms.submittedAt DESC")
    Page<MissionSubmission> findByUserIdWithPaging(@Param("userId") Long userId, Pageable pageable);

    /**
     * 클라이언트별 미션 제출 조회 (자신이 등록한 캠페인의 미션들)
     */
    @Query("SELECT ms FROM MissionSubmission ms " +
           "JOIN FETCH ms.campaignApplication ca " +
           "JOIN FETCH ca.campaign c " +
           "JOIN FETCH ca.user u " +
           "WHERE c.creator.id = :clientId " +
           "ORDER BY ms.submittedAt DESC")
    List<MissionSubmission> findByClientId(@Param("clientId") Long clientId);

    /**
     * 검토 대기 중인 미션 수 조회 (클라이언트별) - 완료되지 않은 미션 수
     */
    @Query("SELECT COUNT(ms) FROM MissionSubmission ms " +
           "JOIN ms.campaignApplication ca " +
           "WHERE ca.campaign.creator.id = :clientId " +
           "AND ms.isCompleted = false")
    Long countPendingMissionsByClientId(@Param("clientId") Long clientId);

    /**
     * 특정 기간 내 제출된 미션 조회
     */
    @Query("SELECT ms FROM MissionSubmission ms " +
           "WHERE ms.submittedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ms.submittedAt DESC")
    List<MissionSubmission> findBySubmittedAtBetween(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    /**
     * 플랫폼별 미션 제출 통계 조회
     */
    @Query("SELECT ms.platformType, COUNT(ms) " +
           "FROM MissionSubmission ms " +
           "GROUP BY ms.platformType " +
           "ORDER BY COUNT(ms) DESC")
    List<Object[]> getPlatformStatistics();

    /**
     * 미션 제출 승인율 조회 (클라이언트별) - 완료율 조회
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN ms.isCompleted = true THEN 1 END) * 100.0 / COUNT(ms) " +
           "FROM MissionSubmission ms " +
           "JOIN ms.campaignApplication ca " +
           "WHERE ca.campaign.creator.id = :clientId")
    Double getApprovalRateByClientId(@Param("clientId") Long clientId);

    /**
     * 캠페인별 미션 제출 통계 - isCompleted 기반
     */
    @Query("SELECT " +
           "COUNT(ms) as totalSubmissions, " +
           "COUNT(CASE WHEN ms.isCompleted = true THEN 1 END) as completedCount, " +
           "COUNT(CASE WHEN ms.isCompleted = false AND ms.reviewedAt IS NOT NULL THEN 1 END) as revisionRequestedCount, " +
           "COUNT(CASE WHEN ms.isCompleted = false AND ms.reviewedAt IS NULL THEN 1 END) as pendingCount " +
           "FROM MissionSubmission ms " +
           "JOIN ms.campaignApplication ca " +
           "WHERE ca.campaign.id = :campaignId")
    Object[] getCampaignMissionStatistics(@Param("campaignId") Long campaignId);

    /**
     * 검토가 필요한 미션 조회 (오래된 순) - 완료되지 않은 미션
     */
    @Query("SELECT ms FROM MissionSubmission ms " +
           "JOIN FETCH ms.campaignApplication ca " +
           "JOIN FETCH ca.user u " +
           "WHERE ms.isCompleted = false " +
           "AND ms.submittedAt < :cutoffDate " +
           "ORDER BY ms.submittedAt ASC")
    List<MissionSubmission> findPendingMissionsOlderThan(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * 캠페인 신청 존재 여부 확인
     */
    boolean existsByCampaignApplication(CampaignApplication campaignApplication);

    /**
     * 캠페인 신청별 미션 제출 조회 (단일)
     */
    Optional<MissionSubmission> findByCampaignApplication(CampaignApplication campaignApplication);

    /**
     * 캠페인 신청별 완료 상태 기반 미션 제출 존재 여부 확인
     */
    boolean existsByCampaignApplicationAndIsCompleted(CampaignApplication campaignApplication, Boolean isCompleted);

}
