package com.example.auth.service;

import com.example.auth.constant.ApplicationStatus;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignApplication;
import com.example.auth.dto.campaign.*;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.repository.CampaignApplicationRepository;
import com.example.auth.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 내 캠페인 목록 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyCampaignService {
    
    private final CampaignApplicationRepository applicationRepository;
    private final CampaignRepository campaignRepository;
    
    /**
     * 사용자 역할에 따른 캠페인 요약 정보 조회
     */
    public MyCampaignSummaryResponse getMyCampaignSummary(Long userId, String userRole) {
        log.info("내 캠페인 요약 조회: userId={}, role={}", userId, userRole);
        
        if ("CLIENT".equals(userRole)) {
            return getClientCampaignSummary(userId);
        } else {
            return getUserCampaignSummary(userId);
        }
    }
    
    /**
     * 사용자 역할에 따른 캠페인 상세 목록 조회
     */
    public MyCampaignListResponse getMyCampaignList(Long userId, String userRole, String category, Pageable pageable) {
        log.info("내 캠페인 목록 조회: userId={}, role={}, category={}", userId, userRole, category);
        
        if ("CLIENT".equals(userRole)) {
            return getClientCampaignList(userId, category, pageable);
        } else {
            return getUserCampaignList(userId, category, pageable);
        }
    }
    
    /**
     * USER 역할 - 캠페인 요약 정보 조회
     */
    private MyCampaignSummaryResponse getUserCampaignSummary(Long userId) {
        List<Object[]> statusCounts = applicationRepository.countByUserIdGroupByStatus(userId);
        
        Map<String, MyCampaignSummaryResponse.CategorySummary> summary = new LinkedHashMap<>();
        
        // 기본값 설정
        summary.put("applied", MyCampaignSummaryResponse.CategorySummary.builder()
                .count(0).label("지원").build());
        summary.put("pending", MyCampaignSummaryResponse.CategorySummary.builder()
                .count(0).label("대기중").build());
        summary.put("selected", MyCampaignSummaryResponse.CategorySummary.builder()
                .count(0).label("선정").build());
        summary.put("completed", MyCampaignSummaryResponse.CategorySummary.builder()
                .count(0).label("완료").build());
        
        int totalApplied = 0;
        
        // 실제 카운트 설정
        for (Object[] row : statusCounts) {
            ApplicationStatus status = (ApplicationStatus) row[0];
            Long count = (Long) row[1];
            
            totalApplied += count.intValue();
            
            switch (status) {
                case PENDING:
                    summary.get("pending").setCount(count.intValue());
                    break;
                case APPROVED:
                    summary.get("selected").setCount(count.intValue());
                    break;
                case COMPLETED:
                    summary.get("completed").setCount(count.intValue());
                    break;
            }
        }
        
        // 총 지원수 설정
        summary.get("applied").setCount(totalApplied);
        
        return MyCampaignSummaryResponse.builder()
                .role("USER")
                .summary(summary)
                .build();
    }
    
    /**
     * CLIENT 역할 - 캠페인 요약 정보 조회
     */
    private MyCampaignSummaryResponse getClientCampaignSummary(Long userId) {
        List<Object[]> statusCounts = campaignRepository.countByCreatorIdGroupByApprovalStatus(userId);
        Long expiredCount = campaignRepository.countExpiredByCreatorId(userId, LocalDate.now());
        
        Map<String, MyCampaignSummaryResponse.CategorySummary> summary = new LinkedHashMap<>();
        
        // 기본값 설정
        summary.put("pending", MyCampaignSummaryResponse.CategorySummary.builder()
                .count(0).label("대기중").build());
        summary.put("approved", MyCampaignSummaryResponse.CategorySummary.builder()
                .count(0).label("승인됨").build());
        summary.put("rejected", MyCampaignSummaryResponse.CategorySummary.builder()
                .count(0).label("거절됨").build());
        summary.put("expired", MyCampaignSummaryResponse.CategorySummary.builder()
                .count(expiredCount.intValue()).label("만료됨").build());
        
        // 실제 카운트 설정
        for (Object[] row : statusCounts) {
            Campaign.ApprovalStatus status = (Campaign.ApprovalStatus) row[0];
            Long count = (Long) row[1];
            
            switch (status) {
                case PENDING:
                    summary.get("pending").setCount(count.intValue());
                    break;
                case APPROVED:
                    // 승인된 것 중 만료되지 않은 것만 카운트
                    int approvedCount = count.intValue() - expiredCount.intValue();
                    summary.get("approved").setCount(Math.max(0, approvedCount));
                    break;
                case REJECTED:
                    summary.get("rejected").setCount(count.intValue());
                    break;
            }
        }
        
        return MyCampaignSummaryResponse.builder()
                .role("CLIENT")
                .summary(summary)
                .build();
    }
    
    /**
     * USER 역할 - 캠페인 상세 목록 조회
     */
    private MyCampaignListResponse getUserCampaignList(Long userId, String category, Pageable pageable) {
        Page<CampaignApplication> applications;
        
        // 카테고리에 따른 필터링
        switch (category) {
            case "applied":
                applications = applicationRepository.findByUserId(userId, pageable);
                break;
            case "pending":
                applications = applicationRepository.findByUserIdAndStatus(userId, ApplicationStatus.PENDING, pageable);
                break;
            case "selected":
                applications = applicationRepository.findByUserIdAndStatus(userId, ApplicationStatus.APPROVED, pageable);
                break;
            case "completed":
                applications = applicationRepository.findByUserIdAndStatus(userId, ApplicationStatus.COMPLETED, pageable);
                break;
            default:
                applications = applicationRepository.findByUserId(userId, pageable);
        }
        
        // DTO 변환
        List<UserCampaignResponse> campaigns = applications.getContent().stream()
                .map(this::convertToUserCampaignResponse)
                .collect(Collectors.toList());
        
        return MyCampaignListResponse.builder()
                .role("USER")
                .category(category)
                .campaigns(campaigns)
                .pagination(PageResponse.PaginationInfo.builder()
                        .pageNumber(applications.getNumber())
                        .pageSize(applications.getSize())
                        .totalPages(applications.getTotalPages())
                        .totalElements(applications.getTotalElements())
                        .first(applications.isFirst())
                        .last(applications.isLast())
                        .build())
                .build();
    }
    
    /**
     * CLIENT 역할 - 캠페인 상세 목록 조회
     */
    private MyCampaignListResponse getClientCampaignList(Long userId, String category, Pageable pageable) {
        Page<Campaign> campaigns;
        
        // 카테고리에 따른 필터링
        switch (category) {
            case "pending":
                campaigns = campaignRepository.findByCreatorIdAndApprovalStatus(userId, Campaign.ApprovalStatus.PENDING, pageable);
                break;
            case "approved":
                campaigns = campaignRepository.findByCreatorIdAndApprovalStatus(userId, Campaign.ApprovalStatus.APPROVED, pageable);
                break;
            case "rejected":
                campaigns = campaignRepository.findByCreatorIdAndApprovalStatus(userId, Campaign.ApprovalStatus.REJECTED, pageable);
                break;
            case "expired":
                campaigns = campaignRepository.findExpiredByCreatorId(userId, LocalDate.now(), pageable);
                break;
            default:
                campaigns = campaignRepository.findByCreatorIdWithCompany(userId, pageable);
        }
        
        // DTO 변환
        List<ClientCampaignResponse> campaignList = campaigns.getContent().stream()
                .map(this::convertToClientCampaignResponse)
                .collect(Collectors.toList());
        
        return MyCampaignListResponse.builder()
                .role("CLIENT")
                .category(category)
                .campaigns(campaignList)
                .pagination(PageResponse.PaginationInfo.builder()
                        .pageNumber(campaigns.getNumber())
                        .pageSize(campaigns.getSize())
                        .totalPages(campaigns.getTotalPages())
                        .totalElements(campaigns.getTotalElements())
                        .first(campaigns.isFirst())
                        .last(campaigns.isLast())
                        .build())
                .build();
    }
    
    /**
     * CampaignApplication을 UserCampaignResponse로 변환
     */
    private UserCampaignResponse convertToUserCampaignResponse(CampaignApplication application) {
        Campaign campaign = application.getCampaign();
        
        return UserCampaignResponse.builder()
                .applicationId(application.getId())
                .campaignId(campaign.getId())
                .title(campaign.getTitle())
                .applicationStatus(application.getApplicationStatus().toString())
                .hasApplied(true)
                .appliedAt(application.getCreatedAt() != null ? application.getCreatedAt().toString() : null)
                .updatedAt(application.getUpdatedAt() != null ? application.getUpdatedAt().toString() : null)
                .campaign(UserCampaignResponse.CampaignDetail.builder()
                        .id(campaign.getId())
                        .title(campaign.getTitle())
                        .description(campaign.getProductDetails())
                        .imageUrl(campaign.getThumbnailUrl())
                        .applicationStartDate(campaign.getRecruitmentStartDate() != null ? 
                                campaign.getRecruitmentStartDate().toString() : null)
                        .applicationEndDate(campaign.getRecruitmentEndDate() != null ? 
                                campaign.getRecruitmentEndDate().toString() : null)
                        .experienceStartDate(campaign.getSelectionDate() != null ? 
                                campaign.getSelectionDate().toString() : null)
                        .experienceEndDate(campaign.getReviewDeadlineDate() != null ? 
                                campaign.getReviewDeadlineDate().toString() : null)
                        .recruitmentCount(campaign.getMaxApplicants())
                        .currentApplicationCount(getCurrentApplicationCount(campaign.getId()))
                        .category(campaign.getCategory() != null ? campaign.getCategory().getCategoryName() : null)
                        .type(campaign.getCampaignType())
                        .location(null) // Campaign 엔티티에 location 필드가 없음
                        .company(UserCampaignResponse.CompanyInfo.builder()
                                .id(campaign.getCompany().getId())
                                .name(campaign.getCompany().getCompanyName())
                                .businessNumber(campaign.getCompany().getBusinessRegistrationNumber())
                                .build())
                        .build())
                .build();
    }
    
    /**
     * Campaign을 ClientCampaignResponse로 변환
     */
    private ClientCampaignResponse convertToClientCampaignResponse(Campaign campaign) {
        // 캠페인 통계 정보 조회
        Object[] statistics = campaignRepository.getCampaignStatistics(campaign.getId());
        
        ClientCampaignResponse.CampaignStatistics stats = ClientCampaignResponse.CampaignStatistics.builder()
                .totalApplications(statistics[0] != null ? ((Number) statistics[0]).intValue() : 0)
                .selectedCount(statistics[1] != null ? ((Number) statistics[1]).intValue() : 0)
                .completedCount(statistics[2] != null ? ((Number) statistics[2]).intValue() : 0)
                .pendingApplications(statistics[3] != null ? ((Number) statistics[3]).intValue() : 0)
                .build();
        
        // 캠페인 상태 계산
        String campaignStatus = calculateCampaignStatus(campaign);
        
        return ClientCampaignResponse.builder()
                .campaignId(campaign.getId())
                .title(campaign.getTitle())
                .description(campaign.getProductDetails())
                .imageUrl(campaign.getThumbnailUrl())
                .approvalStatus(campaign.getApprovalStatus().toString())
                .campaignStatus(campaignStatus)
                .createdAt(campaign.getCreatedAt() != null ? campaign.getCreatedAt().toString() : null)
                .updatedAt(campaign.getUpdatedAt() != null ? campaign.getUpdatedAt().toString() : null)
                .applicationStartDate(campaign.getRecruitmentStartDate() != null ? 
                        campaign.getRecruitmentStartDate().toString() : null)
                .applicationEndDate(campaign.getRecruitmentEndDate() != null ? 
                        campaign.getRecruitmentEndDate().toString() : null)
                .experienceStartDate(campaign.getSelectionDate() != null ? 
                        campaign.getSelectionDate().toString() : null)
                .experienceEndDate(campaign.getReviewDeadlineDate() != null ? 
                        campaign.getReviewDeadlineDate().toString() : null)
                .recruitmentCount(campaign.getMaxApplicants())
                .category(campaign.getCategory() != null ? campaign.getCategory().getCategoryName() : null)
                .type(campaign.getCampaignType())
                .location(null) // Campaign 엔티티에 location 필드가 없음
                .statistics(stats)
                .company(ClientCampaignResponse.CompanyInfo.builder()
                        .id(campaign.getCompany().getId())
                        .name(campaign.getCompany().getCompanyName())
                        .businessNumber(campaign.getCompany().getBusinessRegistrationNumber())
                        .build())
                .build();
    }
    
    /**
     * 현재 신청자 수 조회
     */
    private Integer getCurrentApplicationCount(Long campaignId) {
        return campaignRepository.countCurrentApplicationsByCampaignId(campaignId);
    }
    
    /**
     * 캠페인 상태 계산 (CLIENT용)
     */
    private String calculateCampaignStatus(Campaign campaign) {
        if (!"APPROVED".equals(campaign.getApprovalStatus().toString())) {
            return campaign.getApprovalStatus().toString();
        }
        
        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = campaign.getRecruitmentStartDate();
        LocalDate endDate = campaign.getRecruitmentEndDate();
        
        if (currentDate.isBefore(startDate)) {
            return "SCHEDULED"; // 시작 전
        } else if (currentDate.isAfter(endDate)) {
            return "EXPIRED"; // 만료됨
        } else {
            return "ACTIVE"; // 진행중
        }
    }
}
