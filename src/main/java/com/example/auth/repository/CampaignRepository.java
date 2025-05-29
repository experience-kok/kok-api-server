package com.example.auth.repository;

import com.example.auth.domain.Campaign;
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
    List<Campaign> findByCreatorId(Long creatorId);
    
    // 승인된 캠페인만 조회하는 메서드들
    List<Campaign> findByApprovalStatus(String approvalStatus);
    Optional<Campaign> findByIdAndApprovalStatus(Long id, String approvalStatus);
    
    // 승인된 캠페인 페이지네이션 조회
    Page<Campaign> findByApprovalStatus(String approvalStatus, Pageable pageable);
    
    // 승인되고 신청 마감일이 현재 이후인 캠페인만 페이지네이션 조회 (마감되지 않은 캠페인)
    Page<Campaign> findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqual(
            String approvalStatus, LocalDate currentDate, Pageable pageable);
    
    // 카테고리별 승인된 캠페인 페이지네이션 조회
    Page<Campaign> findByApprovalStatusAndCategoryCategoryType(
            String approvalStatus, String categoryType, Pageable pageable);
    
    // 캠페인 타입별 승인된 캠페인 페이지네이션 조회
    Page<Campaign> findByApprovalStatusAndCampaignType(
            String approvalStatus, String campaignType, Pageable pageable);
    
    // 캠페인 타입별 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCampaignTypeAndApplicationDeadlineDateGreaterThanEqual(
            String approvalStatus, String campaignType, LocalDate currentDate, Pageable pageable);
    
    // 현재 신청 인원수 조회를 위한 쿼리
    @Query("SELECT COUNT(ca) FROM CampaignApplication ca WHERE ca.campaign.id = :campaignId AND ca.status IN ('pending', 'completed')")
    Integer countApplicationsByCampaignId(Long campaignId);
    
    // 여러 캠페인의 신청 인원수를 배치로 조회
    @Query("SELECT ca.campaign.id, COUNT(ca) FROM CampaignApplication ca " +
           "WHERE ca.campaign.id IN :campaignIds AND ca.status IN ('pending', 'completed') " +
           "GROUP BY ca.campaign.id")
    List<Object[]> countApplicationsByCampaignIds(List<Long> campaignIds);
    
    // 신청 인원수로 정렬된 승인된 캠페인 조회 (신청 많은 순)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN CampaignApplication ca ON c.id = ca.campaign.id AND ca.status IN ('pending', 'completed') " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusOrderByCurrentApplicantsDesc(String approvalStatus, Pageable pageable);
    
    // 신청 인원수로 정렬된 활성 캠페인 조회 (신청 많은 순)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN CampaignApplication ca ON c.id = ca.campaign.id AND ca.status IN ('pending', 'completed') " +
           "WHERE c.approvalStatus = :approvalStatus AND c.applicationDeadlineDate >= :currentDate " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqualOrderByCurrentApplicantsDesc(
            String approvalStatus, LocalDate currentDate, Pageable pageable);
    
    // 카테고리별 신청 인원수로 정렬된 캠페인 조회 (신청 많은 순)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN CampaignApplication ca ON c.id = ca.campaign.id AND ca.status IN ('pending', 'completed') " +
           "WHERE c.approvalStatus = :approvalStatus AND c.category.categoryType = :categoryType " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeOrderByCurrentApplicantsDesc(
            String approvalStatus, String categoryType, Pageable pageable);
    
    // 캠페인 타입별 신청 인원수로 정렬된 캠페인 조회 (신청 많은 순)
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN CampaignApplication ca ON c.id = ca.campaign.id AND ca.status IN ('pending', 'completed') " +
           "WHERE c.approvalStatus = :approvalStatus AND c.campaignType = :campaignType " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndCampaignTypeOrderByCurrentApplicantsDesc(
            String approvalStatus, String campaignType, Pageable pageable);
    
    // ===== 세분화된 필터링을 위한 새로운 메서드들 =====
    
    // 카테고리명으로 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCategoryCategoryNameAndApplicationDeadlineDateGreaterThanEqual(
            String approvalStatus, String categoryName, LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입으로 활성 캠페인 조회  
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndApplicationDeadlineDateGreaterThanEqual(
            String approvalStatus, String categoryType, LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입과 카테고리명으로 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndApplicationDeadlineDateGreaterThanEqual(
            String approvalStatus, String categoryType, String categoryName, LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입과 캠페인 타입들로 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCampaignTypeInAndApplicationDeadlineDateGreaterThanEqual(
            String approvalStatus, String categoryType, List<String> campaignTypes, LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입, 카테고리명, 캠페인 타입들로 활성 캠페인 조회
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndCampaignTypeInAndApplicationDeadlineDateGreaterThanEqual(
            String approvalStatus, String categoryType, String categoryName, List<String> campaignTypes, LocalDate currentDate, Pageable pageable);
    
    // 복합 조건 필터링 (카테고리명 + 캠페인 타입들)
    @Query("SELECT c FROM Campaign c " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.category.categoryName = :categoryName " +
           "AND c.campaignType IN :campaignTypes " +
           "AND c.applicationDeadlineDate >= :currentDate")
    Page<Campaign> findFilteredCampaigns(
            @Param("approvalStatus") String approvalStatus,
            @Param("categoryType") String categoryType, 
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
            @Param("approvalStatus") String approvalStatus,
            @Param("categoryType") String categoryType,
            @Param("campaignTypes") List<String> campaignTypes,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);
    
    // 복합 조건 + 인기순 정렬
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN CampaignApplication ca ON c.id = ca.campaign.id AND ca.status IN ('pending', 'completed') " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.category.categoryName = :categoryName " +
           "AND c.campaignType IN :campaignTypes " +
           "AND c.applicationDeadlineDate >= :currentDate " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findFilteredCampaignsOrderByPopularity(
            @Param("approvalStatus") String approvalStatus,
            @Param("categoryType") String categoryType,
            @Param("categoryName") String categoryName,
            @Param("campaignTypes") List<String> campaignTypes,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);
    
    // 캠페인 타입들만 + 인기순 정렬
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN CampaignApplication ca ON c.id = ca.campaign.id AND ca.status IN ('pending', 'completed') " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.campaignType IN :campaignTypes " +
           "AND c.applicationDeadlineDate >= :currentDate " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findFilteredCampaignsByTypesOrderByPopularity(
            @Param("approvalStatus") String approvalStatus,
            @Param("categoryType") String categoryType,
            @Param("campaignTypes") List<String> campaignTypes,
            @Param("currentDate") LocalDate currentDate,
            Pageable pageable);
    
    // 카테고리명으로 인기순 정렬
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN CampaignApplication ca ON c.id = ca.campaign.id AND ca.status IN ('pending', 'completed') " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryName = :categoryName " +
           "AND c.applicationDeadlineDate >= :currentDate " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryNameAndApplicationDeadlineDateGreaterThanEqualOrderByPopularity(
            String approvalStatus, String categoryName, LocalDate currentDate, Pageable pageable);
    
    // 카테고리 타입으로 인기순 정렬
    @Query("SELECT c FROM Campaign c " +
           "LEFT JOIN CampaignApplication ca ON c.id = ca.campaign.id AND ca.status IN ('pending', 'completed') " +
           "WHERE c.approvalStatus = :approvalStatus " +
           "AND c.category.categoryType = :categoryType " +
           "AND c.applicationDeadlineDate >= :currentDate " +
           "GROUP BY c.id " +
           "ORDER BY COUNT(ca.id) DESC, c.createdAt DESC")
    Page<Campaign> findByApprovalStatusAndCategoryCategoryTypeAndApplicationDeadlineDateGreaterThanEqualOrderByPopularity(
            String approvalStatus, String categoryType, LocalDate currentDate, Pageable pageable);
}