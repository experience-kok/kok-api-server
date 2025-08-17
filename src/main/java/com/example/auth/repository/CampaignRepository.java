package com.example.auth.repository;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignCategory;
import com.example.auth.domain.Company;
import com.example.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    // ===== 자동완성용 제목만 조회 메서드 =====

    /**
     * 자동완성을 위한 모든 캠페인 제목만 조회 (성능 최적화)
     */
    @Query("SELECT c.title FROM Campaign c WHERE c.title IS NOT NULL AND c.title != ''")
    List<String> findAllTitles();

    /**
     * 승인된 캠페인의 제목만 조회 (자동완성 품질 향상)
     */
    @Query("SELECT c.title FROM Campaign c WHERE c.title IS NOT NULL AND c.title != '' AND c.approvalStatus = 'APPROVED'")
    List<String> findApprovedTitles();

    // 생성자별 캠페인 조회
    List<Campaign> findByCreator(User creator);

    Page<Campaign> findByCreator(User creator, Pageable pageable);

    List<Campaign> findByCreatorId(Long creatorId);

    // 생성자와 승인 상태별 캠페인 조회
    List<Campaign> findByCreatorAndApprovalStatus(User creator, Campaign.ApprovalStatus approvalStatus);

    Page<Campaign> findByCreatorAndApprovalStatus(User creator, Campaign.ApprovalStatus approvalStatus, Pageable pageable);

    // 생성자, 승인 상태, 모집 마감일로 캠페인 조회 (EXPIRED 필터용)
    List<Campaign> findByCreatorAndApprovalStatusAndRecruitmentEndDateBefore(User creator, Campaign.ApprovalStatus approvalStatus, LocalDate date);

    Page<Campaign> findByCreatorAndApprovalStatusAndRecruitmentEndDateBefore(User creator, Campaign.ApprovalStatus approvalStatus, LocalDate date, Pageable pageable);

    // 생성자, 승인 상태, 모집 마감일로 캠페인 조회 (APPROVED 필터용 - 아직 만료되지 않은 캠페인)
    List<Campaign> findByCreatorAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(User creator, Campaign.ApprovalStatus approvalStatus, LocalDate date);

    Page<Campaign> findByCreatorAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(User creator, Campaign.ApprovalStatus approvalStatus, LocalDate date, Pageable pageable);

    
    // 브랜드존 전용 쿼리들
    long countByCompany(Company company);
    
    long countByCompanyAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
            Company company, Campaign.ApprovalStatus approvalStatus, LocalDate currentDate);


    Optional<Campaign> findByIdAndApprovalStatus(Long id, Campaign.ApprovalStatus approvalStatus);

    /**
     * 특정 승인 상태 목록에 해당하는 캠페인 조회 (거절된 캠페인 제외용)
     */
    Optional<Campaign> findByIdAndApprovalStatusIn(Long id, List<Campaign.ApprovalStatus> approvalStatuses);


    // 마감 임박 캠페인 조회용 - 복수 캠페인 타입, 상시 캠페인 제외
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.isAlwaysOpen = false " +
            "AND c.recruitmentEndDate >= :currentDate " +
            "AND c.campaignType IN :campaignTypes " +
            "ORDER BY c.recruitmentEndDate ASC")
    Page<Campaign> findApprovedActiveByDeadlineSoonWithCampaignTypes(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // 마감 임박 캠페인 조회용 - 카테고리 타입과 복수 캠페인 타입, 상시 캠페인 제외
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.isAlwaysOpen = false " +
            "AND c.recruitmentEndDate >= :currentDate " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.campaignType IN :campaignTypes " +
            "ORDER BY c.recruitmentEndDate ASC")
    Page<Campaign> findApprovedActiveByDeadlineSoonWithCategoryTypeAndCampaignTypes(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // 마감 임박 캠페인 조회용 - 모든 필터 조건, 상시 캠페인 제외
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.isAlwaysOpen = false " +
            "AND c.recruitmentEndDate >= :currentDate " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.category.categoryName = :categoryName " +
            "AND c.campaignType IN :campaignTypes " +
            "ORDER BY c.recruitmentEndDate ASC")
    Page<Campaign> findApprovedActiveByDeadlineSoonWithAllFilters(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // 마감 임박 캠페인 조회용 - 상시 캠페인 제외
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.isAlwaysOpen = false " +
            "AND c.recruitmentEndDate >= :currentDate")
    Page<Campaign> findByApprovalStatusAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 카테고리 타입으로 마감 임박 캠페인 조회 - 상시 캠페인 제외
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.isAlwaysOpen = false " +
            "AND c.recruitmentEndDate >= :currentDate")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 캠페인 타입으로 마감 임박 캠페인 조회 - 상시 캠페인 제외
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.campaignType = :campaignType " +
            "AND c.isAlwaysOpen = false " +
            "AND c.recruitmentEndDate >= :currentDate")
    Page<Campaign> findByApprovalStatusAndCampaignTypeAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("campaignType") String campaignType, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 카테고리 타입과 카테고리명으로 마감 임박 캠페인 조회 - 상시 캠페인 제외
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.category.categoryName = :categoryName " +
            "AND c.isAlwaysOpen = false " +
            "AND c.recruitmentEndDate >= :currentDate")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 승인된 활성 캠페인 조회 (마감되지 않은 캠페인 + 상시 캠페인)
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate)")
    Page<Campaign> findByApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 캠페인 타입별 활성 캠페인 조회 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.campaignType = :campaignType " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate)")
    Page<Campaign> findByApprovalStatusAndCampaignTypeAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("campaignType") String campaignType, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 현재 유효한 신청 인원수 조회를 위한 쿼리 (APPLIED 상태)
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId AND ca.applicationStatus = 'APPLIED'")
    Integer countCurrentApplicationsByCampaignId(@Param("campaignId") Long campaignId);


    // 카테고리 타입으로 활성 캠페인 조회 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.category.categoryType = :categoryType " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate)")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 카테고리 타입과 카테고리명으로 활성 캠페인 조회 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.category.categoryName = :categoryName " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate)")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);


    /**
     * CLIENT용 - 특정 생성자의 승인 상태별 캠페인 카운트를 조회합니다.
     */
    @Query("SELECT c.approvalStatus as status, COUNT(c) as count " +
            "FROM Campaign c " +
            "WHERE c.creator.id = :creatorId " +
            "GROUP BY c.approvalStatus")
    List<Object[]> countByCreatorIdGroupByApprovalStatus(@Param("creatorId") Long creatorId);

    /**
     * CLIENT용 - 특정 생성자의 만료된 캠페인 카운트를 조회합니다.
     */
    @Query("SELECT COUNT(c) FROM Campaign c " +
            "WHERE c.creator.id = :creatorId " +
            "AND c.approvalStatus = 'APPROVED' " +
            "AND c.recruitmentEndDate < :currentDate")
    Long countExpiredByCreatorId(@Param("creatorId") Long creatorId, @Param("currentDate") LocalDate currentDate);

    /**
     * CLIENT용 - 특정 생성자의 승인 상태별 캠페인 목록을 페이징 조회합니다.
     */
    @Query("SELECT c FROM Campaign c " +
            "JOIN FETCH c.company comp " +
            "WHERE c.creator.id = :creatorId " +
            "AND c.approvalStatus = :approvalStatus " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findByCreatorIdAndApprovalStatus(@Param("creatorId") Long creatorId,
                                                    @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
                                                    Pageable pageable);

    /**
     * CLIENT용 - 특정 생성자의 만료된 캠페인 목록을 페이징 조회합니다.
     */
    @Query("SELECT c FROM Campaign c " +
            "JOIN FETCH c.company comp " +
            "WHERE c.creator.id = :creatorId " +
            "AND c.approvalStatus = 'APPROVED' " +
            "AND c.recruitmentEndDate < :currentDate " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findExpiredByCreatorId(@Param("creatorId") Long creatorId,
                                          @Param("currentDate") LocalDate currentDate,
                                          Pageable pageable);

    /**
     * CLIENT용 - 특정 생성자의 모든 캠페인 목록을 페이징 조회합니다.
     */
    @Query("SELECT c FROM Campaign c " +
            "JOIN FETCH c.company comp " +
            "WHERE c.creator.id = :creatorId " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findByCreatorIdWithCompany(@Param("creatorId") Long creatorId, Pageable pageable);

    /**
     * 특정 캠페인의 신청 통계를 조회합니다.
     */
    @Query("SELECT " +
            "COUNT(ca) as totalApplications, " +
            "SUM(CASE WHEN ca.applicationStatus = 'APPROVED' THEN 1 ELSE 0 END) as selectedCount, " +
            "SUM(CASE WHEN ca.applicationStatus = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount, " +
            "SUM(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 ELSE 0 END) as appliedCount " +
            "FROM CampaignApplication ca " +
            "WHERE ca.campaign.id = :campaignId")
    Object[] getCampaignStatistics(@Param("campaignId") Long campaignId);


    // ===== 개선된 두 단계 쿼리 방식 - 인기순 정렬을 위한 메서드들 =====

    /**
     * 1단계: 인기순으로 정렬된 캠페인 ID 목록 조회 (전체) - H2 호환
     */
    @Query(value = """
        SELECT c.id, COUNT(CASE WHEN ca.application_status = 'APPLIED' THEN 1 END) as app_count
        FROM campaigns c
        LEFT JOIN campaign_applications ca ON c.id = ca.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND c.recruitment_end_date >= :currentDate
        GROUP BY c.id, c.created_at
        ORDER BY app_count DESC, c.created_at DESC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> findCampaignIdsOrderByPopularity(
        @Param("approvalStatus") String approvalStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 1단계: 인기순으로 정렬된 캠페인 ID 목록 조회 (카테고리 타입 필터) - H2 호환
     */
    @Query(value = """
        SELECT c.id, COUNT(CASE WHEN ca.application_status = 'APPLIED' THEN 1 END) as app_count
        FROM campaigns c
        JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN campaign_applications ca ON c.id = ca.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND c.recruitment_end_date >= :currentDate
        AND cc.category_type = :categoryType
        GROUP BY c.id, c.created_at
        ORDER BY app_count DESC, c.created_at DESC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> findCampaignIdsByCategoryTypeOrderByPopularity(
        @Param("approvalStatus") String approvalStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("categoryType") String categoryType,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 1단계: 인기순으로 정렬된 캠페인 ID 목록 조회 (카테고리 타입 + 카테고리명 필터) - H2 호환
     */
    @Query(value = """
        SELECT c.id, COUNT(CASE WHEN ca.application_status = 'APPLIED' THEN 1 END) as app_count
        FROM campaigns c
        JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN campaign_applications ca ON c.id = ca.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND c.recruitment_end_date >= :currentDate
        AND cc.category_type = :categoryType
        AND cc.category_name = :categoryName
        GROUP BY c.id, c.created_at
        ORDER BY app_count DESC, c.created_at DESC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> findCampaignIdsByCategoryTypeAndNameOrderByPopularity(
        @Param("approvalStatus") String approvalStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("categoryType") String categoryType,
        @Param("categoryName") String categoryName,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 1단계: 인기순으로 정렬된 캠페인 ID 목록 조회 (캠페인 타입 필터) - H2 호환
     */
    @Query(value = """
        SELECT c.id, COUNT(CASE WHEN ca.application_status = 'APPLIED' THEN 1 END) as app_count
        FROM campaigns c
        LEFT JOIN campaign_applications ca ON c.id = ca.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND c.recruitment_end_date >= :currentDate
        AND c.campaign_type = :campaignType
        GROUP BY c.id, c.created_at
        ORDER BY app_count DESC, c.created_at DESC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> findCampaignIdsByCampaignTypeOrderByPopularity(
        @Param("approvalStatus") String approvalStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("campaignType") String campaignType,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 2단계: 캠페인 ID 목록으로 실제 캠페인 데이터 조회
     */
    @Query("SELECT c FROM Campaign c " +
           "JOIN FETCH c.category " +
           "WHERE c.id IN :campaignIds")
    List<Campaign> findCampaignsByIds(@Param("campaignIds") List<Long> campaignIds);

    /**
     * 전체 개수 조회 (인기순용) - 전체
     */
    @Query("SELECT COUNT(DISTINCT c.id) " +
           "FROM Campaign c " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.recruitmentEndDate >= :currentDate")
    long countCampaignsForPopular(
        @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
        @Param("currentDate") LocalDate currentDate
    );

    /**
     * 전체 개수 조회 (인기순용) - 카테고리 타입 필터
     */
    @Query("SELECT COUNT(DISTINCT c.id) " +
           "FROM Campaign c " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.recruitmentEndDate >= :currentDate " +
           "AND c.category.categoryType = :categoryType")
    long countCampaignsByCategoryTypeForPopular(
        @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("categoryType") CampaignCategory.CategoryType categoryType
    );

    /**
     * 전체 개수 조회 (인기순용) - 카테고리 타입 + 카테고리명 필터
     */
    @Query("SELECT COUNT(DISTINCT c.id) " +
           "FROM Campaign c " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.recruitmentEndDate >= :currentDate " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.category.categoryName = :categoryName")
    long countCampaignsByCategoryTypeAndNameForPopular(
        @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("categoryType") CampaignCategory.CategoryType categoryType,
        @Param("categoryName") String categoryName
    );

    // ===== 기존 승인된 + 활성 캠페인 전용 메서드들 (최신순 정렬용) =====

    // 승인된 + 활성 + 인기순 (전체) - 모든 캠페인 포함하도록 수정, 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "LEFT JOIN c.applications ca " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) DESC, c.createdAt DESC")
    Page<Campaign> findApprovedActiveOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);

    // 승인된 + 활성 + 최신순 (전체) - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveOrderByLatest(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);

    // 승인된 + 활성 + 카테고리 타입 + 인기순 - 모든 캠페인 포함하도록 수정, 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "LEFT JOIN c.applications ca " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.category.categoryType = :categoryType " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) DESC, c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            Pageable pageable);

    // 승인된 + 활성 + 카테고리 타입 + 최신순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.category.categoryType = :categoryType " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeOrderByLatest(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            Pageable pageable);

    // 승인된 + 활성 + 캠페인 타입 + 인기순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "LEFT JOIN c.applications ca " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.campaignType = :campaignType " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) DESC, c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCampaignTypeOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("campaignType") String campaignType,
            Pageable pageable);

    // 승인된 + 활성 + 캠페인 타입 + 최신순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.campaignType = :campaignType " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCampaignTypeOrderByLatest(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("campaignType") String campaignType,
            Pageable pageable);

    // 승인된 + 활성 + 카테고리 타입과 카테고리명 + 인기순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "LEFT JOIN c.applications ca " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.category.categoryName = :categoryName " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) DESC, c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndNameOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName,
            Pageable pageable);

    // 승인된 + 활성 + 카테고리 타입과 카테고리명 + 최신순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.category.categoryName = :categoryName " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndNameOrderByLatest(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName,
            Pageable pageable);

    // 승인된 + 활성 + 카테고리 타입과 캠페인 타입들 + 인기순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "LEFT JOIN c.applications ca " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.campaignType IN :campaignTypes " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) DESC, c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndCampaignTypesOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // 승인된 + 활성 + 카테고리 타입과 캠페인 타입들 + 최신순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.campaignType IN :campaignTypes " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndCampaignTypesOrderByLatest(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // 승인된 + 활성 + 카테고리 타입, 카테고리명, 캠페인 타입들 + 인기순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "LEFT JOIN c.applications ca " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.category.categoryName = :categoryName " +
            "AND c.campaignType IN :campaignTypes " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) DESC, c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndNameAndCampaignTypesOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // 승인된 + 활성 + 카테고리 타입, 카테고리명, 캠페인 타입들 + 최신순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND c.category.categoryType = :categoryType " +
            "AND c.category.categoryName = :categoryName " +
            "AND c.campaignType IN :campaignTypes " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndNameAndCampaignTypesOrderByLatest(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // 승인된 + 활성 + 검색 + 최신순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND (:categoryType IS NULL OR c.category.categoryType = :categoryType) " +
            "AND (:categoryName IS NULL OR c.category.categoryName = :categoryName) " +
            "AND (:campaignType IS NULL OR c.campaignType = :campaignType) " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> searchApprovedActiveByKeywordOrderByLatest(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("keyword") String keyword,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignType") String campaignType,
            Pageable pageable);

    // 승인된 + 활성 + 검색 + 인기순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "LEFT JOIN c.applications ca " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) DESC, c.createdAt DESC")
    Page<Campaign> searchApprovedActiveByKeywordOrderByPopularity(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("keyword") String keyword,
            Pageable pageable);

    // 승인된 + 활성 + 검색 + 플랫폼 필터 + 최신순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND c.campaignType IN :campaignTypes " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> searchApprovedActiveByKeywordAndCampaignTypesOrderByLatest(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("keyword") String keyword,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // 승인된 + 활성 + 검색 + 플랫폼 필터 + 인기순 - 상시 캠페인 포함
    @Query("SELECT c FROM Campaign c " +
            "LEFT JOIN c.applications ca " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND (c.isAlwaysOpen = true OR c.recruitmentEndDate >= :currentDate) " +
            "AND LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND c.campaignType IN :campaignTypes " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) DESC, c.createdAt DESC")
    Page<Campaign> searchApprovedActiveByKeywordAndCampaignTypesOrderByPopularity(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("keyword") String keyword,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // ===== N+1 문제 해결을 위한 최적화된 메서드들 =====

    /**
     * JOIN FETCH로 N+1 문제를 해결한 최적화된 인기순 조회
     */
    @Query("SELECT DISTINCT c FROM Campaign c " +
            "JOIN FETCH c.category " +
            "LEFT JOIN c.applications ca " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.recruitmentEndDate >= :currentDate " +
            "AND (:categoryType IS NULL OR c.category.categoryType = :categoryType) " +
            "GROUP BY c.id, c.category.id " +
            "ORDER BY COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) DESC, c.createdAt DESC")
    Page<Campaign> findApprovedActiveOrderByPopularityOptimized(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            Pageable pageable);

    /**
     * JOIN FETCH로 N+1 문제를 해결한 최적화된 최신순 조회
     */
    @Query("SELECT c FROM Campaign c " +
            "JOIN FETCH c.category " +
            "WHERE c.approvalStatus = :approvalStatus " +
            "AND c.recruitmentEndDate >= :currentDate " +
            "AND (:categoryType IS NULL OR c.category.categoryType = :categoryType) " +
            "ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveOrderByLatestOptimized(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            Pageable pageable);

    /**
     * N+1 문제 해결을 위한 배치 단위 신청자 수 조회
     */
    @Query("SELECT ca.campaign.id, COUNT(ca) " +
           "FROM CampaignApplication ca " +
           "WHERE ca.campaign.id IN :campaignIds " +
           "AND ca.applicationStatus = 'APPLIED' " +
           "GROUP BY ca.campaign.id")
    List<Object[]> countApplicationsByCampaignIds(@Param("campaignIds") List<Long> campaignIds);

    /**
     * N+1 문제 해결을 위한 최적화된 캠페인 + 신청자 수 조회 (인기순) - H2 호환, 상시 캠페인 포함
     */
    @Query(value = """
        SELECT 
            c.id,
            c.title,
            c.thumbnail_url,
            c.recruitment_end_date,
            c.max_applicants,
            c.campaign_type,
            c.product_short_info,
            c.created_at,
            cc.category_name,
            cc.category_type,
            COALESCE(app_counts.applied_count, 0) as current_applicants
        FROM campaigns c
        JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT 
                campaign_id,
                COUNT(*) as applied_count
            FROM campaign_applications 
            WHERE application_status = 'APPLIED'
            GROUP BY campaign_id
        ) app_counts ON c.id = app_counts.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND (c.is_always_open = true OR c.recruitment_end_date >= :currentDate)
        AND (:categoryType IS NULL OR cc.category_type = :categoryType)
        AND (:categoryName IS NULL OR cc.category_name = :categoryName)
        AND (:campaignType IS NULL OR c.campaign_type = :campaignType)
        ORDER BY app_counts.applied_count DESC NULLS LAST, c.created_at DESC
        """, 
        countQuery = """
            SELECT COUNT(*)
            FROM campaigns c
            JOIN campaign_categories cc ON c.category_id = cc.id
            WHERE c.approval_status = :approvalStatus
            AND (c.is_always_open = true OR c.recruitment_end_date >= :currentDate)
            AND (:categoryType IS NULL OR cc.category_type = :categoryType)
            AND (:categoryName IS NULL OR cc.category_name = :categoryName)
            AND (:campaignType IS NULL OR c.campaign_type = :campaignType)
        """,
        nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListByPopularity(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignType") String campaignType,
            Pageable pageable);

    /**
     * N+1 문제 해결을 위한 최적화된 캠페인 + 신청자 수 조회 (최신순) - H2 호환, 상시 캠페인 포함
     */
    @Query(value = """
        SELECT 
            c.id,
            c.title,
            c.thumbnail_url,
            c.recruitment_end_date,
            c.max_applicants,
            c.campaign_type,
            c.product_short_info,
            c.created_at,
            cc.category_name,
            cc.category_type,
            COALESCE(app_counts.applied_count, 0) as current_applicants
        FROM campaigns c
        JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT 
                campaign_id,
                COUNT(*) as applied_count
            FROM campaign_applications 
            WHERE application_status = 'APPLIED'
            GROUP BY campaign_id
        ) app_counts ON c.id = app_counts.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND (c.is_always_open = true OR c.recruitment_end_date >= :currentDate)
        AND (:categoryType IS NULL OR cc.category_type = :categoryType)
        AND (:categoryName IS NULL OR cc.category_name = :categoryName)
        AND (:campaignType IS NULL OR c.campaign_type = :campaignType)
        ORDER BY c.created_at DESC
        """, 
        countQuery = """
            SELECT COUNT(*)
            FROM campaigns c
            JOIN campaign_categories cc ON c.category_id = cc.id
            WHERE c.approval_status = :approvalStatus
            AND (c.is_always_open = true OR c.recruitment_end_date >= :currentDate)
            AND (:categoryType IS NULL OR cc.category_type = :categoryType)
            AND (:categoryName IS NULL OR cc.category_name = :categoryName)
            AND (:campaignType IS NULL OR c.campaign_type = :campaignType)
        """,
        nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListByLatest(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignType") String campaignType,
            Pageable pageable);

    /**
     * 복수 캠페인 타입을 지원하는 최적화된 캠페인 + 신청자 수 조회 (인기순) - 상시 캠페인 포함
     */
    @Query(value = """
        SELECT 
            c.id,
            c.title,
            c.thumbnail_url,
            c.recruitment_end_date,
            c.max_applicants,
            c.campaign_type,
            c.product_short_info,
            c.created_at,
            cc.category_name,
            cc.category_type,
            COALESCE(app_counts.applied_count, 0) as current_applicants
        FROM campaigns c
        JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT 
                campaign_id,
                COUNT(*) as applied_count
            FROM campaign_applications 
            WHERE application_status = 'APPLIED'
            GROUP BY campaign_id
        ) app_counts ON c.id = app_counts.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND (c.is_always_open = true OR c.recruitment_end_date >= :currentDate)
        AND (:categoryType IS NULL OR cc.category_type = :categoryType)
        AND (:categoryName IS NULL OR cc.category_name = :categoryName)
        AND (COALESCE(:campaignTypesSize, 0) = 0 OR c.campaign_type = ANY(CAST(:campaignTypesArray AS text[])))
        ORDER BY app_counts.applied_count DESC NULLS LAST, c.created_at DESC
        """, 
        countQuery = """
            SELECT COUNT(*)
            FROM campaigns c
            JOIN campaign_categories cc ON c.category_id = cc.id
            WHERE c.approval_status = :approvalStatus
            AND (c.is_always_open = true OR c.recruitment_end_date >= :currentDate)
            AND (:categoryType IS NULL OR cc.category_type = :categoryType)
            AND (:categoryName IS NULL OR cc.category_name = :categoryName)
            AND (COALESCE(:campaignTypesSize, 0) = 0 OR c.campaign_type = ANY(CAST(:campaignTypesArray AS text[])))
        """,
        nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListByPopularityWithTypes(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignTypesSize") Integer campaignTypesSize,
            @Param("campaignTypesArray") String campaignTypesArray,
            Pageable pageable);

    /**
     * 복수 캠페인 타입을 지원하는 최적화된 캠페인 + 신청자 수 조회 (최신순) - 상시 캠페인 포함
     */
    @Query(value = """
        SELECT 
            c.id,
            c.title,
            c.thumbnail_url,
            c.recruitment_end_date,
            c.max_applicants,
            c.campaign_type,
            c.product_short_info,
            c.created_at,
            cc.category_name,
            cc.category_type,
            COALESCE(app_counts.applied_count, 0) as current_applicants
        FROM campaigns c
        JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT 
                campaign_id,
                COUNT(*) as applied_count
            FROM campaign_applications 
            WHERE application_status = 'APPLIED'
            GROUP BY campaign_id
        ) app_counts ON c.id = app_counts.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND (c.is_always_open = true OR c.recruitment_end_date >= :currentDate)
        AND (:categoryType IS NULL OR cc.category_type = :categoryType)
        AND (:categoryName IS NULL OR cc.category_name = :categoryName)
        AND (COALESCE(:campaignTypesSize, 0) = 0 OR c.campaign_type = ANY(CAST(:campaignTypesArray AS text[])))
        ORDER BY c.created_at DESC
        """, 
        countQuery = """
            SELECT COUNT(*)
            FROM campaigns c
            JOIN campaign_categories cc ON c.category_id = cc.id
            WHERE c.approval_status = :approvalStatus
            AND (c.is_always_open = true OR c.recruitment_end_date >= :currentDate)
            AND (:categoryType IS NULL OR cc.category_type = :categoryType)
            AND (:categoryName IS NULL OR cc.category_name = :categoryName)
            AND (COALESCE(:campaignTypesSize, 0) = 0 OR c.campaign_type = ANY(CAST(:campaignTypesArray AS text[])))
        """,
        nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListByLatestWithTypes(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignTypesSize") Integer campaignTypesSize,
            @Param("campaignTypesArray") String campaignTypesArray,
            Pageable pageable);
    @Query(value = """
        SELECT 
            c.id,
            c.title,
            c.thumbnail_url,
            c.recruitment_end_date,
            c.max_applicants,
            c.created_at,
            cc.category_name,
            cc.category_type,
            COALESCE(app_counts.pending_count, 0) as current_applicants
        FROM CAMPAIGNS c
        JOIN CAMPAIGN_CATEGORIES cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT 
                campaign_id,
                COUNT(*) as pending_count
            FROM CAMPAIGN_APPLICATIONS 
            WHERE application_status = 'PENDING'
            GROUP BY campaign_id
        ) app_counts ON c.id = app_counts.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND c.recruitment_end_date >= :currentDate
        AND (:categoryType IS NULL OR cc.category_type = :categoryType)
        ORDER BY 
            CASE WHEN :sortBy = 'popular' THEN app_counts.pending_count END DESC NULLS LAST,
            CASE WHEN :sortBy = 'latest' THEN c.created_at END DESC
        """, 
        countQuery = """
            SELECT COUNT(*)
            FROM CAMPAIGNS c
            JOIN CAMPAIGN_CATEGORIES cc ON c.category_id = cc.id
            WHERE c.approval_status = :approvalStatus
            AND c.recruitment_end_date >= :currentDate
            AND (:categoryType IS NULL OR cc.category_type = :categoryType)
        """,
        nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListNative(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("sortBy") String sortBy,
            Pageable pageable);

    // ===== 탈퇴 검증을 위한 메서드들 =====

    /**
     * 특정 사용자가 진행 중인 캠페인이 있는지 확인 (캠페인 생성자로서)
     * @param creatorId 캠페인 생성자 ID
     * @param approvalStatuses 확인할 승인 상태 목록
     * @return 진행 중인 캠페인 존재 여부
     */
    boolean existsByCreatorIdAndApprovalStatusIn(Long creatorId, List<Campaign.ApprovalStatus> approvalStatuses);

    /**
     * 특정 사용자의 진행 중인 캠페인 수 조회
     * @param creatorId 캠페인 생성자 ID
     * @param approvalStatuses 확인할 승인 상태 목록
     * @return 진행 중인 캠페인 수
     */
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.creator.id = :creatorId AND c.approvalStatus IN :approvalStatuses")
    long countByCreatorIdAndApprovalStatusIn(@Param("creatorId") Long creatorId, @Param("approvalStatuses") List<Campaign.ApprovalStatus> approvalStatuses);

    /**
     * 특정 사용자의 특정 승인 상태 캠페인이 있는지 확인 (단일 상태)
     * @param creatorId 캠페인 생성자 ID  
     * @param approvalStatus 확인할 승인 상태
     * @return 해당 상태 캠페인 존재 여부
     */
    boolean existsByCreatorIdAndApprovalStatus(Long creatorId, Campaign.ApprovalStatus approvalStatus);

    /**
     * 특정 사용자의 승인된 활성 캠페인이 있는지 확인 (만료되지 않은 캠페인)
     * @param creatorId 캠페인 생성자 ID
     * @param approvalStatus 승인 상태
     * @param currentDate 현재 날짜 (모집 마감일과 비교)
     * @return 승인된 활성 캠페인 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Campaign c WHERE c.creator.id = :creatorId AND c.approvalStatus = :approvalStatus AND c.recruitmentEndDate >= :currentDate")
    boolean existsByCreatorIdAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
            @Param("creatorId") Long creatorId, 
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("currentDate") java.time.LocalDate currentDate);
}