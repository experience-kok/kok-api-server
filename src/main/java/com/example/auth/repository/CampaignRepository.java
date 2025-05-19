package com.example.auth.repository;

import com.example.auth.domain.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    
    // 현재 신청 인원수 조회를 위한 쿼리 (나중에 신청 기능이 추가되면 구현)
    // @Query("SELECT COUNT(a) FROM Application a WHERE a.campaign.id = :campaignId AND a.status IN ('PENDING', 'ACCEPTED')")
    // Integer countApplicationsByCampaignId(Long campaignId);
}