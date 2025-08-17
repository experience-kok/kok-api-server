package com.example.auth.repository;

import com.example.auth.constant.ApplicationStatus;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignApplication;
import com.example.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignApplicationRepository extends JpaRepository<CampaignApplication, Long> {

    /**
     * 특정 캠페인의 모든 신청 정보를 조회합니다.
     *
     * @param campaign 조회할 캠페인
     * @return 신청 목록
     */
    List<CampaignApplication> findByCampaign(Campaign campaign);

    /**
     * 특정 캠페인의 모든 신청 정보를 페이징하여 조회합니다.
     *
     * @param campaign 조회할 캠페인
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    Page<CampaignApplication> findByCampaign(Campaign campaign, Pageable pageable);

    /**
     * 특정 사용자의 모든 신청 정보를 조회합니다.
     *
     * @param user 조회할 사용자
     * @return 신청 목록
     */
    List<CampaignApplication> findByUser(User user);

    /**
     * 특정 사용자의 모든 신청 정보를 페이징하여 조회합니다.
     *
     * @param user     조회할 사용자
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    Page<CampaignApplication> findByUser(User user, Pageable pageable);

    /**
     * 특정 사용자의 특정 상태 신청 정보를 페이징하여 조회합니다.
     *
     * @param user              조회할 사용자
     * @param applicationStatus 신청 상태
     * @param pageable          페이징 정보
     * @return 페이징된 신청 목록
     */
    Page<CampaignApplication> findByUserAndApplicationStatus(User user, ApplicationStatus applicationStatus, Pageable pageable);

    /**
     * 특정 사용자와 캠페인의 신청 정보를 조회합니다.
     *
     * @param user     사용자
     * @param campaign 캠페인
     * @return 신청 정보 (존재하는 경우)
     */
    Optional<CampaignApplication> findByUserAndCampaign(User user, Campaign campaign);

    /**
     * 특정 캠페인의 상태별 신청 수를 조회합니다.
     *
     * @param campaignId        캠페인 ID
     * @param applicationStatus 신청 상태
     * @return 해당 상태의 신청 수
     */
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId AND ca.applicationStatus = :applicationStatus")
    long countByCampaignIdAndApplicationStatus(@Param("campaignId") Long campaignId,
                                               @Param("applicationStatus") ApplicationStatus applicationStatus);

    /**
     * 특정 캠페인의 모든 신청 수를 조회합니다.
     *
     * @param campaignId 캠페인 ID
     * @return 전체 신청 수
     */
    long countByCampaignId(Long campaignId);

    /**
     * 특정 상태의 모든 신청 정보를 조회합니다.
     *
     * @param applicationStatus 신청 상태
     * @return 해당 상태의 신청 목록
     */
    List<CampaignApplication> findByApplicationStatus(ApplicationStatus applicationStatus);

    /**
     * 특정 캠페인의 특정 상태 신청 정보를 페이징하여 조회합니다.
     *
     * @param campaign          캠페인
     * @param applicationStatus 신청 상태
     * @param pageable          페이징 정보
     * @return 해당 캠페인의 해당 상태 신청 목록 (페이징)
     */
    Page<CampaignApplication> findByCampaignAndApplicationStatus(Campaign campaign,
                                                                 ApplicationStatus applicationStatus,
                                                                 Pageable pageable);

    /**
     * 특정 사용자의 특정 상태 신청 정보를 조회합니다.
     *
     * @param user              사용자
     * @param applicationStatus 신청 상태
     * @return 해당 사용자의 해당 상태 신청 목록
     */
    List<CampaignApplication> findByUserAndApplicationStatus(User user,
                                                             ApplicationStatus applicationStatus);

    /**
     * 특정 캠페인에 특정 사용자가 신청했는지 확인합니다.
     *
     * @param user     사용자
     * @param campaign 캠페인
     * @return 신청 여부
     */
    boolean existsByUserAndCampaign(User user, Campaign campaign);

    /**
     * 특정 캠페인의 현재 유효한 신청 수를 조회합니다. (APPLIED 상태만)
     *
     * @param campaignId 캠페인 ID
     * @return 현재 신청 수
     */
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId AND ca.applicationStatus = 'APPLIED'")
    long countCurrentApplicantsByCampaignId(@Param("campaignId") Long campaignId);

    /**
     * 특정 사용자의 모든 캠페인 신청을 삭제합니다.
     *
     * @param userId 신청자 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 특정 캠페인의 모든 신청을 삭제합니다.
     *
     * @param campaignId 캠페인 ID
     */
    void deleteByCampaignId(Long campaignId);

    /**
     * 특정 사용자의 신청 상태별 카운트를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 상태별 카운트 맵
     */
    @Query("SELECT ca.applicationStatus as status, COUNT(ca) as count " +
            "FROM CampaignApplication ca " +
            "WHERE ca.user.id = :userId " +
            "GROUP BY ca.applicationStatus")
    List<Object[]> countByUserIdGroupByStatus(@Param("userId") Long userId);

    /**
     * 특정 사용자의 모집 기간 중인 APPLIED 신청 수를 조회합니다. (applied)
     * 상시 캠페인도 포함하여 조회
     *
     * @param userId      사용자 ID
     * @param currentDate 현재 날짜
     * @return 모집 기간 중인 신청 수 + 상시 캠페인 신청 수
     */
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca " +
            "WHERE ca.user.id = :userId " +
            "AND ca.applicationStatus = 'APPLIED' " +
            "AND (ca.campaign.isAlwaysOpen = true OR ca.campaign.recruitmentEndDate >= :currentDate)")
    Long countPendingDuringRecruitment(@Param("userId") Long userId, @Param("currentDate") java.time.LocalDate currentDate);

    /**
     * 특정 사용자의 모집 기간 종료된 APPLIED 신청 수를 조회합니다. (pending)
     * 상시 캠페인은 제외하고 조회 (마감일이 있는 캠페인만)
     *
     * @param userId      사용자 ID
     * @param currentDate 현재 날짜
     * @return 모집 기간 종료된 신청 수 (상시 캠페인 제외)
     */
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca " +
            "WHERE ca.user.id = :userId " +
            "AND ca.applicationStatus = 'APPLIED' " +
            "AND ca.campaign.isAlwaysOpen = false " +
            "AND ca.campaign.recruitmentEndDate < :currentDate")
    Long countPendingAfterRecruitment(@Param("userId") Long userId, @Param("currentDate") java.time.LocalDate currentDate);

    /**
     * 특정 사용자의 모집 기간 중인 APPLIED 신청 목록을 조회합니다. (applied)
     * 상시 캠페인도 포함하여 조회
     *
     * @param userId      사용자 ID
     * @param currentDate 현재 날짜
     * @param pageable    페이징 정보
     * @return 모집 기간 중인 신청 목록 + 상시 캠페인 신청 목록
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
            "JOIN FETCH ca.campaign c " +
            "JOIN FETCH c.company comp " +
            "WHERE ca.user.id = :userId " +
            "AND ca.applicationStatus = 'APPLIED' " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findAppliedByUserId(@Param("userId") Long userId,
                                                  @Param("currentDate") java.time.LocalDate currentDate,
                                                  Pageable pageable);

    /**
     * 특정 사용자의 모집 기간 종료된 APPLIED 신청 목록을 조회합니다. (pending)
     * 상시 캠페인은 제외하고 조회 (마감일이 있는 캠페인만)
     *
     * @param userId      사용자 ID
     * @param currentDate 현재 날짜
     * @param pageable    페이징 정보
     * @return 모집 기간 종료된 신청 목록 (상시 캠페인 제외)
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
            "JOIN FETCH ca.campaign c " +
            "JOIN FETCH c.company comp " +
            "WHERE ca.user.id = :userId " +
            "AND ca.applicationStatus = 'APPLIED' " +
            "AND c.isAlwaysOpen = false " +
            "AND c.recruitmentEndDate < :currentDate " +
            "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findPendingByUserId(@Param("userId") Long userId,
                                                  @Param("currentDate") java.time.LocalDate currentDate,
                                                  Pageable pageable);

    /**
     * 디버깅용: 특정 사용자의 SELECTED 상태 신청을 네이티브 쿼리로 조회
     */
    @Query(value = "SELECT ca.*, c.title, c.company_id FROM campaign_applications ca " +
            "JOIN campaigns c ON ca.campaign_id = c.id " +
            "WHERE ca.user_id = :userId AND ca.application_status = 'SELECTED' " +
            "ORDER BY ca.created_at DESC",
            nativeQuery = true)
    List<Object[]> findSelectedByUserIdNative(@Param("userId") Long userId);

    /**
     * 특정 사용자의 특정 상태 신청 목록을 페이징 조회합니다.
     * 디버깅을 위해 JOIN을 단순화한 버전
     *
     * @param userId   사용자 ID
     * @param status   신청 상태
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
            "WHERE ca.user.id = :userId " +
            "AND ca.applicationStatus = :status " +
            "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findByUserIdAndStatusSimple(@Param("userId") Long userId,
                                                          @Param("status") ApplicationStatus status,
                                                          Pageable pageable);

    /**
     * 특정 사용자의 특정 상태 신청 목록을 페이징 조회합니다.
     *
     * @param userId   사용자 ID
     * @param status   신청 상태
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
            "JOIN FETCH ca.campaign c " +
            "LEFT JOIN FETCH c.company comp " +
            "WHERE ca.user.id = :userId " +
            "AND (:status IS NULL OR ca.applicationStatus = :status) " +
            "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findByUserIdAndStatus(@Param("userId") Long userId,
                                                    @Param("status") ApplicationStatus status,
                                                    Pageable pageable);

    /**
     * 특정 사용자의 모든 신청 목록을 페이징 조회합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
            "JOIN FETCH ca.campaign c " +
            "JOIN FETCH c.company comp " +
            "WHERE ca.user.id = :userId " +
            "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자의 APPLIED 상태 신청 목록을 네이티브 쿼리로 조회합니다. (PostgreSQL용)
     */
    @Query(value = "SELECT * FROM campaign_applications WHERE user_id = :userId AND UPPER(application_status) = 'APPLIED' ORDER BY created_at DESC",
           nativeQuery = true)
    List<CampaignApplication> findAppliedByUserIdNative(@Param("userId") Long userId);

    /**
     * 특정 사용자의 APPLIED 상태 신청 목록을 페이징 조회합니다. (디버그용)
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 APPLIED 신청 목록
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
            "WHERE ca.user.id = :userId " +
            "AND ca.applicationStatus = 'APPLIED' " +
            "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findAppliedApplicationsByUserIdSimple(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자의 APPLIED 상태 신청 목록을 페이징 조회합니다.
     * company_id가 NULL인 경우도 처리하기 위해 LEFT JOIN 사용
     * 상시 캠페인도 포함하여 조회
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 APPLIED 신청 목록
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
            "JOIN FETCH ca.campaign c " +
            "LEFT JOIN FETCH c.company comp " +
            "WHERE ca.user.id = :userId " +
            "AND ca.applicationStatus = 'APPLIED' " +
            "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findAppliedApplicationsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * CLIENT 사용자가 생성한 캠페인들에 대한 모든 신청 목록을 페이징 조회합니다.
     *
     * @param creator  캠페인 생성자
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
            "JOIN FETCH ca.campaign c " +
            "JOIN FETCH ca.user u " +
            "WHERE c.creator = :creator " +
            "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findByCampaignCreatedBy(@Param("creator") User creator, Pageable pageable);

    /**
     * 특정 캠페인의 특정 상태 신청 정보를 조회합니다.
     *
     * @param campaign          캠페인
     * @param applicationStatus 신청 상태
     * @return 해당 캠페인의 해당 상태 신청 목록
     */
    List<CampaignApplication> findByCampaignAndApplicationStatus(Campaign campaign, ApplicationStatus applicationStatus);

    /**
     * 특정 캠페인에서 선정된 신청자 수를 조회합니다.
     *
     * @param campaignId 캠페인 ID
     * @return 선정된 신청자 수
     */
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId AND ca.applicationStatus = 'SELECTED'")
    long countSelectedByCampaignId(@Param("campaignId") Long campaignId);

    // ===== 탈퇴 검증을 위한 메서드들 =====

    /**
     * 특정 사용자가 진행 중인 캠페인 신청이 있는지 확인 (인플루언서로서)
     * @param userId 신청자 ID
     * @param applicationStatuses 확인할 신청 상태 목록 (APPLIED, SELECTED 등)
     * @return 진행 중인 신청 존재 여부
     */
    boolean existsByUserIdAndApplicationStatusIn(Long userId, List<ApplicationStatus> applicationStatuses);

    /**
     * 특정 사용자의 진행 중인 캠페인 신청 수 조회
     * @param userId 신청자 ID
     * @param applicationStatuses 확인할 신청 상태 목록
     * @return 진행 중인 신청 수
     */
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.user.id = :userId AND ca.applicationStatus IN :applicationStatuses")
    long countByUserIdAndApplicationStatusIn(@Param("userId") Long userId, @Param("applicationStatuses") List<ApplicationStatus> applicationStatuses);

    /**
     * 특정 사용자의 특정 신청 상태가 있는지 확인 (단일 상태)
     * @param userId 신청자 ID
     * @param applicationStatus 확인할 신청 상태
     * @return 해당 상태 신청 존재 여부
     */
    boolean existsByUserIdAndApplicationStatus(Long userId, ApplicationStatus applicationStatus);
}
