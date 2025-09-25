package com.example.auth.repository;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.Campaign.ApprovalStatus;
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

    // 기본 조회 메서드들 (Enum 사용)
    Optional<Campaign> findByIdAndApprovalStatus(Long id, ApprovalStatus approvalStatus);
    
    @Query("SELECT c FROM Campaign c WHERE c.id = :id AND c.approvalStatus IN :statuses")
    Optional<Campaign> findByIdAndApprovalStatusIn(@Param("id") Long id, @Param("statuses") List<ApprovalStatus> statuses);

    // 승인된 활성 캠페인 조회 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true)")
    Page<Campaign> findByApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // Creator 관련 메서드들 (Enum 사용)
    List<Campaign> findByCreatorId(Long creatorId);
    List<Campaign> findByCreator(User creator);
    Page<Campaign> findByCreator(User creator, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c JOIN FETCH c.company WHERE c.creator.id = :creatorId")
    Page<Campaign> findByCreatorIdWithCompany(@Param("creatorId") Long creatorId, Pageable pageable);
    
    Page<Campaign> findByCreatorIdAndApprovalStatus(Long creatorId, ApprovalStatus approvalStatus, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.creator = :creator AND c.approvalStatus = :approvalStatus AND c.recruitmentEndDate < :date")
    Page<Campaign> findByCreatorAndApprovalStatusAndRecruitmentEndDateBefore(
            @Param("creator") User creator, @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("date") LocalDate date, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.creator = :creator AND c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :date OR c.isAlwaysOpen = true)")
    Page<Campaign> findByCreatorAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
            @Param("creator") User creator, @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("date") LocalDate date, Pageable pageable);
            
    Page<Campaign> findByCreatorAndApprovalStatus(User creator, ApprovalStatus approvalStatus, Pageable pageable);

    // 오버로드된 메서드들 (Pageable 없는 버전, 상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.creator = :creator AND c.approvalStatus = :approvalStatus AND c.recruitmentEndDate < :date AND c.isAlwaysOpen = false")
    List<Campaign> findByCreatorAndApprovalStatusAndRecruitmentEndDateBefore(
            @Param("creator") User creator, @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("date") LocalDate date);
    
    @Query("SELECT c FROM Campaign c WHERE c.creator = :creator AND c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :date OR c.isAlwaysOpen = true)")
    List<Campaign> findByCreatorAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
            @Param("creator") User creator, @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("date") LocalDate date);
            
    List<Campaign> findByCreatorAndApprovalStatus(User creator, ApprovalStatus approvalStatus);

    // Company 관련 메서드들 (상시 캠페인 포함)
    long countByCompany(Company company);
    
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.company = :company AND c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :date OR c.isAlwaysOpen = true)")
    long countByCompanyAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
            @Param("company") Company company, @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("date") LocalDate date);

    // 통계 관련 메서드들 (상시 캠페인 고려)
    @Query("SELECT c.approvalStatus, COUNT(c) FROM Campaign c WHERE c.creator.id = :creatorId GROUP BY c.approvalStatus")
    List<Object[]> countByCreatorIdGroupByApprovalStatus(@Param("creatorId") Long creatorId);
    
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.creator.id = :creatorId AND c.recruitmentEndDate < :date AND c.isAlwaysOpen = false")
    Long countExpiredByCreatorId(@Param("creatorId") Long creatorId, @Param("date") LocalDate date);
    
    @Query("SELECT c FROM Campaign c WHERE c.creator.id = :creatorId AND c.recruitmentEndDate < :date AND c.isAlwaysOpen = false")
    Page<Campaign> findExpiredByCreatorId(@Param("creatorId") Long creatorId, @Param("date") LocalDate date, Pageable pageable);

    // 자동완성용 메서드 (승인된 캠페인만)
    @Query("SELECT DISTINCT c.title FROM Campaign c WHERE c.approvalStatus = :approvalStatus")
    List<String> findApprovedTitles(@Param("approvalStatus") ApprovalStatus approvalStatus);

    // 신청자 수 관련 메서드들
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId AND ca.applicationStatus IN ('APPLIED', 'SELECTED')")
    int countCurrentApplicationsByCampaignId(@Param("campaignId") Long campaignId);

    // 캠페인 통계 메서드
    @Query("SELECT " +
           "COUNT(CASE WHEN ca.applicationStatus = 'APPLIED' THEN 1 END) as applied, " +
           "COUNT(CASE WHEN ca.applicationStatus = 'SELECTED' THEN 1 END) as selected, " +
           "COUNT(CASE WHEN ca.applicationStatus = 'REJECTED' THEN 1 END) as rejected, " +
           "COUNT(ca) as total " +
           "FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId")
    Object[] getCampaignStatistics(@Param("campaignId") Long campaignId);

    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND c.category.categoryType = :categoryType AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true)")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 상시 캠페인 필터링 (Enum 사용)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND c.isAlwaysOpen = false AND c.recruitmentEndDate >= :currentDate")
    Page<Campaign> findByApprovalStatusAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND c.category.categoryType = :categoryType AND c.isAlwaysOpen = false AND c.recruitmentEndDate >= :currentDate")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 캠페인 타입별 조회 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND c.campaignType = :campaignType AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true)")
    Page<Campaign> findByApprovalStatusAndCampaignTypeAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("campaignType") String campaignType, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND c.campaignType = :campaignType AND c.isAlwaysOpen = false AND c.recruitmentEndDate >= :currentDate")
    Page<Campaign> findByApprovalStatusAndCampaignTypeAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("campaignType") String campaignType, 
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 카테고리명으로 조회 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND c.category.categoryType = :categoryType AND c.category.categoryName = :categoryName AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true)")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName,
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND c.category.categoryType = :categoryType AND c.category.categoryName = :categoryName AND c.isAlwaysOpen = false AND c.recruitmentEndDate >= :currentDate")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
            @Param("approvalStatus") ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName,
            @Param("currentDate") LocalDate currentDate, 
            Pageable pageable);

    // 최적화된 네이티브 쿼리들 (String 사용 - 네이티브 쿼리에서는 enum을 직접 사용할 수 없음)
    @Query(value = """
        SELECT c.id, c.title, c.thumbnail_url, c.recruitment_end_date, c.max_applicants, 
               c.campaign_type, c.product_short_info, c.created_at, 
               cc.category_name, cc.category_type, 
               COALESCE(app_count.current_applicants, 0) as current_applicants,
               c.is_always_open
        FROM campaigns c
        LEFT JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT ca.campaign_id, COUNT(*) as current_applicants
            FROM campaign_applications ca 
            WHERE ca.application_status IN ('APPLIED', 'SELECTED')
            GROUP BY ca.campaign_id
        ) app_count ON c.id = app_count.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND (:currentDate IS NULL OR c.recruitment_end_date >= :currentDate OR c.is_always_open = true)
        AND (:categoryType IS NULL OR cc.category_type = :categoryType)
        AND (:categoryName IS NULL OR cc.category_name = :categoryName)
        ORDER BY current_applicants DESC, c.created_at DESC
        """, nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListByPopularity(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("categoryName") String categoryName,
            Pageable pageable);

    // 최적화된 네이티브 쿼리들 (최신순)
    @Query(value = """
        SELECT c.id, c.title, c.thumbnail_url, c.recruitment_end_date, c.max_applicants, 
               c.campaign_type, c.product_short_info, c.created_at, 
               cc.category_name, cc.category_type, 
               COALESCE(app_count.current_applicants, 0) as current_applicants,
               c.is_always_open
        FROM campaigns c
        LEFT JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT ca.campaign_id, COUNT(*) as current_applicants
            FROM campaign_applications ca 
            WHERE ca.application_status IN ('APPLIED', 'SELECTED')
            GROUP BY ca.campaign_id
        ) app_count ON c.id = app_count.campaign_id
        WHERE c.approval_status = CAST(:approvalStatus AS text)
        AND (CAST(:currentDate AS date) IS NULL OR c.recruitment_end_date >= CAST(:currentDate AS date) OR c.is_always_open = true)
        AND (CAST(:categoryType AS text) IS NULL OR cc.category_type = CAST(:categoryType AS text))
        AND (CAST(:categoryName AS text) IS NULL OR cc.category_name = CAST(:categoryName AS text))
        ORDER BY c.created_at DESC
        """, nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListByLatest(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("categoryName") String categoryName,
            Pageable pageable);

    // 복수 캠페인 타입 지원 네이티브 쿼리들
    @Query(value = """
        SELECT c.id, c.title, c.thumbnail_url, c.recruitment_end_date, c.max_applicants, 
               c.campaign_type, c.product_short_info, c.created_at, 
               cc.category_name, cc.category_type, 
               COALESCE(app_count.current_applicants, 0) as current_applicants,
               c.is_always_open
        FROM campaigns c
        LEFT JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT ca.campaign_id, COUNT(*) as current_applicants
            FROM campaign_applications ca 
            WHERE ca.application_status IN ('APPLIED', 'SELECTED')
            GROUP BY ca.campaign_id
        ) app_count ON c.id = app_count.campaign_id
        WHERE c.approval_status = CAST(:approvalStatus AS text)
        AND (CAST(:currentDate AS date) IS NULL OR c.recruitment_end_date >= CAST(:currentDate AS date) OR c.is_always_open = true)
        AND (CAST(:categoryType AS text) IS NULL OR cc.category_type = CAST(:categoryType AS text))
        AND (CAST(:categoryName AS text) IS NULL OR cc.category_name = CAST(:categoryName AS text))
        AND (:campaignTypesSize = 0 OR c.campaign_type = ANY(CAST(:campaignTypesArray AS text[])))
        ORDER BY current_applicants DESC, c.created_at DESC
        """, nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListByPopularityWithTypes(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignTypesSize") Integer campaignTypesSize,
            @Param("campaignTypesArray") String campaignTypesArray,
            Pageable pageable);

    @Query(value = """
        SELECT c.id, c.title, c.thumbnail_url, c.recruitment_end_date, c.max_applicants, 
               c.campaign_type, c.product_short_info, c.created_at, 
               cc.category_name, cc.category_type, 
               COALESCE(app_count.current_applicants, 0) as current_applicants,
               c.is_always_open
        FROM campaigns c
        LEFT JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT ca.campaign_id, COUNT(*) as current_applicants
            FROM campaign_applications ca 
            WHERE ca.application_status IN ('APPLIED', 'SELECTED')
            GROUP BY ca.campaign_id
        ) app_count ON c.id = app_count.campaign_id
        WHERE c.approval_status = CAST(:approvalStatus AS text)
        AND (CAST(:currentDate AS date) IS NULL OR c.recruitment_end_date >= CAST(:currentDate AS date) OR c.is_always_open = true)
        AND (CAST(:categoryType AS text) IS NULL OR cc.category_type = CAST(:categoryType AS text))
        AND (CAST(:categoryName AS text) IS NULL OR cc.category_name = CAST(:categoryName AS text))
        AND (:campaignTypesSize = 0 OR c.campaign_type = ANY(CAST(:campaignTypesArray AS text[])))
        ORDER BY c.created_at DESC
        """, nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListByLatestWithTypes(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignTypesSize") Integer campaignTypesSize,
            @Param("campaignTypesArray") String campaignTypesArray,
            Pageable pageable);

    // 기존 네이티브 쿼리 (레거시)
    @Query(value = """
        SELECT c.id, c.title, c.thumbnail_url, c.recruitment_end_date, c.max_applicants, 
               c.created_at, cc.category_name, cc.category_type, 
               COALESCE(app_count.current_applicants, 0) as current_applicants
        FROM campaigns c
        LEFT JOIN campaign_categories cc ON c.category_id = cc.id
        LEFT JOIN (
            SELECT ca.campaign_id, COUNT(*) as current_applicants
            FROM campaign_applications ca 
            WHERE ca.application_status IN ('APPLIED', 'SELECTED')
            GROUP BY ca.campaign_id
        ) app_count ON c.id = app_count.campaign_id
        WHERE c.approval_status = :approvalStatus
        AND c.recruitment_end_date >= :currentDate
        AND (:categoryType IS NULL OR cc.category_type = :categoryType)
        ORDER BY CASE WHEN :sort = 'popular' THEN current_applicants END DESC,
                 CASE WHEN :sort = 'latest' THEN c.created_at END DESC
        """, nativeQuery = true)
    Page<Object[]> findOptimizedCampaignListNative(
            @Param("approvalStatus") String approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("categoryType") String categoryType,
            @Param("sort") String sort,
            Pageable pageable);

    // 검색 관련 쿼리들 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND (c.title LIKE %:keyword% OR c.productShortInfo LIKE %:keyword%)")
    Page<Campaign> searchApprovedActiveByKeywordOrderByLatest(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND (c.title LIKE %:keyword% OR c.productShortInfo LIKE %:keyword%) ORDER BY (SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign = c AND ca.applicationStatus IN ('APPLIED', 'SELECTED')) DESC")
    Page<Campaign> searchApprovedActiveByKeywordOrderByPopularity(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND (c.title LIKE %:keyword% OR c.productShortInfo LIKE %:keyword%) AND c.campaignType IN :campaignTypes ORDER BY c.createdAt DESC")
    Page<Campaign> searchApprovedActiveByKeywordAndCampaignTypesOrderByLatest(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("keyword") String keyword,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND (c.title LIKE %:keyword% OR c.productShortInfo LIKE %:keyword%) AND c.campaignType IN :campaignTypes ORDER BY (SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign = c AND ca.applicationStatus IN ('APPLIED', 'SELECTED')) DESC")
    Page<Campaign> searchApprovedActiveByKeywordAndCampaignTypesOrderByPopularity(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("keyword") String keyword,
            @Param("campaignTypes") List<String> campaignTypes,
            Pageable pageable);

    // 정렬별 승인된 활성 캠페인 조회 메서드들 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveOrderByLatest(@Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) ORDER BY (SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign = c AND ca.applicationStatus IN ('APPLIED', 'SELECTED')) DESC")
    Page<Campaign> findApprovedActiveOrderByCurrentApplicantsDesc(@Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, Pageable pageable);

    // 카테고리별 정렬 쿼리들 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND c.category.categoryType = :categoryType ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeOrderByLatest(
            @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND c.category.categoryType = :categoryType ORDER BY (SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign = c AND ca.applicationStatus IN ('APPLIED', 'SELECTED')) DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, Pageable pageable);

    // 캠페인 타입별 정렬 쿼리들 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND c.campaignType = :campaignType ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCampaignTypeOrderByLatest(
            @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, 
            @Param("campaignType") String campaignType, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND c.campaignType = :campaignType ORDER BY (SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign = c AND ca.applicationStatus IN ('APPLIED', 'SELECTED')) DESC")
    Page<Campaign> findApprovedActiveByCampaignTypeOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, 
            @Param("campaignType") String campaignType, Pageable pageable);

    // 카테고리명별 정렬 쿼리들 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND c.category.categoryType = :categoryType AND c.category.categoryName = :categoryName ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndNameOrderByLatest(
            @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, @Param("categoryName") String categoryName, 
            Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND c.category.categoryType = :categoryType AND c.category.categoryName = :categoryName ORDER BY (SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign = c AND ca.applicationStatus IN ('APPLIED', 'SELECTED')) DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndNameOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, @Param("categoryName") String categoryName, 
            Pageable pageable);

    // 복수 캠페인 타입 지원 쿼리들 (상시 캠페인 포함)
    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND c.category.categoryType = :categoryType AND c.campaignType IN :campaignTypes ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndCampaignTypesOrderByLatest(
            @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, @Param("campaignTypes") List<String> campaignTypes, 
            Pageable pageable);


    @Query("SELECT c FROM Campaign c WHERE c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :currentDate OR c.isAlwaysOpen = true) AND c.category.categoryType = :categoryType AND c.category.categoryName = :categoryName AND c.campaignType IN :campaignTypes ORDER BY c.createdAt DESC")
    Page<Campaign> findApprovedActiveByCategoryTypeAndNameAndCampaignTypesOrderByLatest(
            @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("currentDate") LocalDate currentDate, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, @Param("categoryName") String categoryName, 
            @Param("campaignTypes") List<String> campaignTypes, Pageable pageable);

    // UserWithdrawalService에서 사용하는 메서드 (상시 캠페인 포함)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Campaign c WHERE c.creator.id = :creatorId AND c.approvalStatus = :approvalStatus AND (c.recruitmentEndDate >= :date OR c.isAlwaysOpen = true)")
    boolean existsByCreatorIdAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
            @Param("creatorId") Long creatorId, @Param("approvalStatus") ApprovalStatus approvalStatus, @Param("date") LocalDate date);

    // CampaignStatusScheduler에서 사용하는 메서드들 (상시 캠페인 고려)
    @Query("SELECT c FROM Campaign c WHERE c.recruitmentEndDate < :currentDate AND c.isAlwaysOpen = false AND c.approvalStatus = :approvalStatus")
    List<Campaign> findExpiredCampaigns(@Param("currentDate") LocalDate currentDate, @Param("approvalStatus") ApprovalStatus approvalStatus);

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.recruitmentEndDate BETWEEN :currentDate AND :futureDate AND c.isAlwaysOpen = false AND c.approvalStatus = :approvalStatus")
    long countExpiringSoonCampaigns(@Param("currentDate") LocalDate currentDate, @Param("futureDate") LocalDate futureDate, @Param("approvalStatus") ApprovalStatus approvalStatus);

    // 오버로드된 메서드 (기존 스케줄러 호환용)
    default List<Campaign> findExpiredCampaigns(LocalDate currentDate) {
        return findExpiredCampaigns(currentDate, ApprovalStatus.APPROVED);
    }

    default long countExpiringSoonCampaigns(LocalDate currentDate) {
        LocalDate futureDate = currentDate.plusDays(3); // 3일 후까지 마감 예정인 캠페인
        return countExpiringSoonCampaigns(currentDate, futureDate, ApprovalStatus.APPROVED);
    }
}
