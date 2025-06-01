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
    
    // 생성자별 캠페인 조회
    List<Campaign> findByCreator(User creator);
    List<Campaign> findByCreatorId(Long creatorId);
    
    // 업체별 캠페인 조회 (1:N 관계)
    List<Campaign> findByCompany(Company company);
    Page<Campaign> findByCompanyOrderByCreatedAtDesc(Company company, Pageable pageable);
    
    // 승인 상태별 캠페인 조회
    List<Campaign> findByApprovalStatus(Campaign.ApprovalStatus approvalStatus);
    Optional<Campaign> findByIdAndApprovalStatus(Long id, Campaign.ApprovalStatus approvalStatus);
    Page<Campaign> findByApprovalStatus(Campaign.ApprovalStatus approvalStatus, Pageable pageable);
    
    // 승인된 활성 캠페인 조회 (마감되지 않은 캠페인)
    Page<Campaign> findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqual(
            Campaign.ApprovalStatus approvalStatus, LocalDate currentDate, Pageable pageable);
    
    // 카테고리별 승인된 캠페인 페이지네이션 조회
    Page<Campaign> findByApprovalStatusAndCategory(
            Campaign.ApprovalStatus approvalStatus, CampaignCategory category, Pageable pageable);
    
    Page<Campaign> findByApprovalStatusAndCategoryCategoryType(
            Campaign.ApprovalStatus approvalStatus, CampaignCategory.CategoryType categoryType, Pageable pageable);
    
    // 캠페인 타입별 승인된 캠페인 페이지네이션 조회
    Page<Campaign> findByApprovalStatusAndCampaignType(
            Campaign.ApprovalStatus approvalStatus, String campaignType, Pageable pageable);
    
    // 캠페인 타입별 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCampaignTypeAndApplicationDeadlineDateGreaterThanEqual(
            Campaign.ApprovalStatus approvalStatus, String campaignType, LocalDate currentDate, Pageable pageable);
    
    // 현재 유효한 신청 인원수 조회를 위한 쿼리 (APPLIED 상태만)
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId AND ca.applicationStatus = 'APPLIED'")
    Integer countCurrentApplicationsByCampaignId(@Param("campaignId") Long campaignId);
    
    // 여러 캠페인의 현재 신청 인원수를 배치로 조회
    @Query("SELECT ca.campaign.id, COUNT(ca) FROM CampaignApplication ca " +
           "WHERE ca.campaign.id IN :campaignIds AND ca.applicationStatus = 'APPLIED' " +
           "GROUP BY ca.campaign.id")
    List<Object[]> countCurrentApplicationsByCampaignIds(@Param("campaignIds") List<Long> campaignIds);
    
    // 신청 인원수로 정렬된 승인된 캠페인 조회 (신청 많은 순)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.approvalStatus = :approvalStatus AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, Pageable pageable);
    
    // 신청 인원수로 정렬된 활성 캠페인 조회 (신청 많은 순)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.applicationDeadlineDate >= :currentDate " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqualOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("currentDate") LocalDate currentDate, Pageable pageable);
    
    // 카테고리별 신청 인원수로 정렬된 캠페인 조회 (신청 많은 순)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, Pageable pageable);
    
    // 캠페인 타입별 신청 인원수로 정렬된 캠페인 조회 (신청 많은 순)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.campaignType = :campaignType " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndCampaignTypeOrderByCurrentApplicantsDesc(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("campaignType") String campaignType, Pageable pageable);
    
    // ===== 세분화된 필터링을 위한 새로운 메서드들 =====
    
    // 카테고리명으로 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCategoryCategoryNameAndApplicationDeadlineDateGreaterThanEqual(
            Campaign.ApprovalStatus approvalStatus, String categoryName, LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입으로 활성 캠페인 조회  
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndApplicationDeadlineDateGreaterThanEqual(
            Campaign.ApprovalStatus approvalStatus, CampaignCategory.CategoryType categoryType, 
            LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입과 카테고리명으로 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndApplicationDeadlineDateGreaterThanEqual(
            Campaign.ApprovalStatus approvalStatus, CampaignCategory.CategoryType categoryType, 
            String categoryName, LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입과 캠페인 타입들로 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCampaignTypeInAndApplicationDeadlineDateGreaterThanEqual(
            Campaign.ApprovalStatus approvalStatus, CampaignCategory.CategoryType categoryType, 
            List<String> campaignTypes, LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입, 카테고리명, 캠페인 타입들로 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndCampaignTypeInAndApplicationDeadlineDateGreaterThanEqual(
            Campaign.ApprovalStatus approvalStatus, CampaignCategory.CategoryType categoryType, 
            String categoryName, List<String> campaignTypes, LocalDate currentDate, Pageable pageable);
    
    // 복합 조건 필터링 (카테고리명 + 캠페인 타입들)
    @Query("SELECT c FROM Campaign c " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.category.categoryName = :categoryName " +
           "AND c.campaignType IN :campaignTypes " +
           "AND c.applicationDeadlineDate >= :currentDate")
    Page<Campaign> findFilteredCampaigns(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("categoryType") CampaignCategory.CategoryType categoryType, 
            @Param("categoryName") String categoryName,
            @Param("campaignTypes") List<String> campaignTypes,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);
    
    // 캠페인 타입들만으로 필터링
    @Query("SELECT c FROM Campaign c " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.campaignType IN :campaignTypes " +
           "AND c.applicationDeadlineDate >= :currentDate")
    Page<Campaign> findFilteredCampaignsByTypes(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("campaignTypes") List<String> campaignTypes,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);
    
    // 복합 조건 + 인기순 정렬
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.category.categoryName = :categoryName " +
           "AND c.campaignType IN :campaignTypes " +
           "AND c.applicationDeadlineDate >= :currentDate " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findFilteredCampaignsOrderByPopularity(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignTypes") List<String> campaignTypes,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);
    
    // 캠페인 타입들만 + 인기순 정렬
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.campaignType IN :campaignTypes " +
           "AND c.applicationDeadlineDate >= :currentDate " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findFilteredCampaignsByTypesOrderByPopularity(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus,
            @Param("categoryType") CampaignCategory.CategoryType categoryType,
            @Param("campaignTypes") List<String> campaignTypes,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);
    
    // 카테고리명으로 인기순 정렬
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryName = :categoryName " +
           "AND c.applicationDeadlineDate >= :currentDate " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryNameAndApplicationDeadlineDateGreaterThanEqualOrderByPopularity(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("categoryName") String categoryName, 
            @Param("currentDate") LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입으로 인기순 정렬
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.applicationDeadlineDate >= :currentDate " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndApplicationDeadlineDateGreaterThanEqualOrderByPopularity(
            @Param("approvalStatus") Campaign.ApprovalStatus approvalStatus, 
            @Param("categoryType") CampaignCategory.CategoryType categoryType, 
            @Param("currentDate") LocalDate currentDate, Pageable pageable);

    // 관리자용 - 승인 대기 중인 캠페인 조회
    Page<Campaign> findByApprovalStatusOrderByCreatedAtDesc(
            Campaign.ApprovalStatus approvalStatus, Pageable pageable);
    
    // 관리자용 - 특정 사용자의 캠페인 조회
    Page<Campaign> findByCreatorOrderByCreatedAtDesc(User creator, Pageable pageable);
    
    // 관리자용 - 특정 기간 내 생성된 캠페인 조회
    @Query("SELECT c FROM Campaign c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate ORDER BY c.createdAt DESC")
    Page<Campaign> findByCreatedAtBetween(
            @Param("startDate") java.time.ZonedDateTime startDate,
            @Param("endDate") java.time.ZonedDateTime endDate,
            Pageable pageable);

    // ===== 승인 상태 무관 조회 메서드들 =====
    
    // 카테고리 타입별 조회 (승인 상태 무관)
    Page<Campaign> findByCategoryCategoryType(CampaignCategory.CategoryType categoryType, Pageable pageable);
    
    // 캠페인 타입별 조회 (승인 상태 무관)
    Page<Campaign> findByCampaignType(String campaignType, Pageable pageable);
    
    // 카테고리 타입과 카테고리명으로 조회 (승인 상태 무관)
    Page<Campaign> findByCategoryCategoryTypeAndCategoryCategoryName(
            CampaignCategory.CategoryType categoryType, String categoryName, Pageable pageable);
    
    // 카테고리 타입과 캠페인 타입들로 조회 (승인 상태 무관)
    Page<Campaign> findByCategoryCategoryTypeAndCampaignTypeIn(
            CampaignCategory.CategoryType categoryType, List<String> campaignTypes, Pageable pageable);
    
    // 카테고리 타입, 카테고리명, 캠페인 타입들로 조회 (승인 상태 무관)
    Page<Campaign> findByCategoryCategoryTypeAndCategoryCategoryNameAndCampaignTypeIn(
            CampaignCategory.CategoryType categoryType, String categoryName, List<String> campaignTypes, Pageable pageable);

    // ===== 승인 상태 무관 + 신청자 수 기준 정렬 메서드들 =====
    
    // 모든 캠페인을 신청자 수 기준으로 정렬 (승인 상태 무관)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findAllOrderByCurrentApplicantsDesc(Pageable pageable);
    
    // 카테고리별 신청자 수 정렬 (승인 상태 무관)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.category.categoryType = :categoryType " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByCategoryCategoryTypeOrderByCurrentApplicantsDesc(
            @Param("categoryType") CampaignCategory.CategoryType categoryType, Pageable pageable);
    
    // 캠페인 타입별 신청자 수 정렬 (승인 상태 무관)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.campaignType = :campaignType " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByCampaignTypeOrderByCurrentApplicantsDesc(
            @Param("campaignType") String campaignType, Pageable pageable);
    
    // 카테고리 타입과 카테고리명으로 신청자 수 정렬 (승인 상태 무관)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.category.categoryType = :categoryType " +
           "AND c.category.categoryName = :categoryName " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByCategoryCategoryTypeAndCategoryCategoryNameOrderByCurrentApplicantsDesc(
            @Param("categoryType") CampaignCategory.CategoryType categoryType, 
            @Param("categoryName") String categoryName, Pageable pageable);
    
    // 카테고리 타입과 캠페인 타입들로 신청자 수 정렬 (승인 상태 무관)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.category.categoryType = :categoryType " +
           "AND c.campaignType IN :campaignTypes " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByCategoryCategoryTypeAndCampaignTypeInOrderByCurrentApplicantsDesc(
            @Param("categoryType") CampaignCategory.CategoryType categoryType, 
            @Param("campaignTypes") List<String> campaignTypes, Pageable pageable);
    
    // 카테고리 타입, 카테고리명, 캠페인 타입들로 신청자 수 정렬 (승인 상태 무관)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN c.applications ca " +
           "WHERE c.category.categoryType = :categoryType " +
           "AND c.category.categoryName = :categoryName " +
           "AND c.campaignType IN :campaignTypes " +
           "AND (ca.applicationStatus = 'APPLIED' OR ca IS NULL) " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByCategoryCategoryTypeAndCategoryCategoryNameAndCampaignTypeInOrderByCurrentApplicantsDesc(
            @Param("categoryType") CampaignCategory.CategoryType categoryType, 
            @Param("categoryName") String categoryName, 
            @Param("campaignTypes") List<String> campaignTypes, Pageable pageable);
}
