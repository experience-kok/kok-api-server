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
     * @param campaign 조회할 캠페인
     * @return 신청 목록
     */
    List<CampaignApplication> findByCampaign(Campaign campaign);

    /**
     * 특정 캠페인의 모든 신청 정보를 페이징하여 조회합니다.
     * @param campaign 조회할 캠페인
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    Page<CampaignApplication> findByCampaign(Campaign campaign, Pageable pageable);

    /**
     * 특정 사용자의 모든 신청 정보를 조회합니다.
     * @param user 조회할 사용자
     * @return 신청 목록
     */
    List<CampaignApplication> findByUser(User user);

    /**
     * 특정 사용자의 모든 신청 정보를 페이징하여 조회합니다.
     * @param user 조회할 사용자
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    Page<CampaignApplication> findByUser(User user, Pageable pageable);

    /**
     * 특정 사용자와 캠페인의 신청 정보를 조회합니다.
     * @param user 사용자
     * @param campaign 캠페인
     * @return 신청 정보 (존재하는 경우)
     */
    Optional<CampaignApplication> findByUserAndCampaign(User user, Campaign campaign);

    /**
     * 특정 캠페인의 상태별 신청 수를 조회합니다.
     * @param campaignId 캠페인 ID
     * @param applicationStatus 신청 상태
     * @return 해당 상태의 신청 수
     */
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId AND ca.applicationStatus = :applicationStatus")
    long countByCampaignIdAndApplicationStatus(@Param("campaignId") Long campaignId, 
                                              @Param("applicationStatus") ApplicationStatus applicationStatus);

    /**
     * 특정 캠페인의 모든 신청 수를 조회합니다.
     * @param campaignId 캠페인 ID
     * @return 전체 신청 수
     */
    long countByCampaignId(Long campaignId);

    /**
     * 특정 상태의 모든 신청 정보를 조회합니다.
     * @param applicationStatus 신청 상태
     * @return 해당 상태의 신청 목록
     */
    List<CampaignApplication> findByApplicationStatus(ApplicationStatus applicationStatus);

    /**
     * 특정 캠페인의 특정 상태 신청 정보를 조회합니다.
     * @param campaign 캠페인
     * @param applicationStatus 신청 상태
     * @return 해당 캠페인의 해당 상태 신청 목록
     */
    List<CampaignApplication> findByCampaignAndApplicationStatus(Campaign campaign, 
                                                               ApplicationStatus applicationStatus);

    /**
     * 특정 사용자의 특정 상태 신청 정보를 조회합니다.
     * @param user 사용자
     * @param applicationStatus 신청 상태
     * @return 해당 사용자의 해당 상태 신청 목록
     */
    List<CampaignApplication> findByUserAndApplicationStatus(User user, 
                                                           ApplicationStatus applicationStatus);

    /**
     * 특정 캠페인에 특정 사용자가 신청했는지 확인합니다.
     * @param user 사용자
     * @param campaign 캠페인
     * @return 신청 여부
     */
    boolean existsByUserAndCampaign(User user, Campaign campaign);

    /**
     * 특정 캠페인의 현재 유효한 신청 수를 조회합니다. (APPLIED 상태만)
     * @param campaignId 캠페인 ID
     * @return 현재 신청 수
     */
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId AND ca.applicationStatus = 'PENDING'")
    long countCurrentApplicantsByCampaignId(@Param("campaignId") Long campaignId);
    
    /**
     * 특정 사용자의 모든 캠페인 신청을 삭제합니다.
     * @param userId 신청자 ID
     */
    void deleteByUserId(Long userId);
    
    /**
     * 특정 캠페인의 모든 신청을 삭제합니다.
     * @param campaignId 캠페인 ID
     */
    void deleteByCampaignId(Long campaignId);
    
    /**
     * 특정 사용자의 신청 상태별 카운트를 조회합니다.
     * @param userId 사용자 ID
     * @return 상태별 카운트 맵
     */
    @Query("SELECT ca.applicationStatus as status, COUNT(ca) as count " +
           "FROM CampaignApplication ca " +
           "WHERE ca.user.id = :userId " +
           "GROUP BY ca.applicationStatus")
    List<Object[]> countByUserIdGroupByStatus(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 특정 상태 신청 목록을 페이징 조회합니다.
     * @param userId 사용자 ID
     * @param status 신청 상태
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
           "JOIN FETCH ca.campaign c " +
           "JOIN FETCH c.company comp " +
           "WHERE ca.user.id = :userId " +
           "AND (:status IS NULL OR ca.applicationStatus = :status) " +
           "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findByUserIdAndStatus(@Param("userId") Long userId, 
                                                   @Param("status") ApplicationStatus status, 
                                                   Pageable pageable);
    
    /**
     * 특정 사용자의 모든 신청 목록을 페이징 조회합니다.
     * @param userId 사용자 ID
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
     * CLIENT 사용자가 생성한 캠페인들에 대한 모든 신청 목록을 페이징 조회합니다.
     * @param creator 캠페인 생성자
     * @param pageable 페이징 정보
     * @return 페이징된 신청 목록
     */
    @Query("SELECT ca FROM CampaignApplication ca " +
           "JOIN FETCH ca.campaign c " +
           "JOIN FETCH ca.user u " +
           "WHERE c.creator = :creator " +
           "ORDER BY ca.createdAt DESC")
    Page<CampaignApplication> findByCampaignCreatedBy(@Param("creator") User creator, Pageable pageable);
}
