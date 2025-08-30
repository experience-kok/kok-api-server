package com.example.auth.dto.brandzone;

import com.example.auth.domain.Campaign;
import com.example.auth.dto.campaign.CampaignCategoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZonedDateTime;

/**
 * 브랜드존 캠페인 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandCampaignResponse {
    
    /**
     * 캠페인 ID
     */
    private Long campaignId;
    
    /**
     * 캠페인 제목
     */
    private String title;
    
    /**
     * 캠페인 타입 (인스타그램, 유튜브 등)
     */
    private String campaignType;
    
    /**
     * 썸네일 이미지 URL
     */
    private String thumbnailUrl;
    
    /**
     * 제품 간단 정보
     */
    private String productShortInfo;
    
    /**
     * 현재 신청자 수
     */
    private int currentApplicants;
    
    /**
     * 최대 신청자 수
     */
    private int maxApplicants;
    
    /**
     * 모집 마감일
     */
    private LocalDate recruitmentEndDate;
    
    /**
     * 승인 상태
     */
    private String approvalStatus;
    
    /**
     * 캠페인 생성일
     */
    private ZonedDateTime createdAt;
    
    /**
     * 카테고리 정보
     */
    private CampaignCategoryResponse category;
    
    /**
     * 모집 상태 (진행중/마감)
     */
    private boolean isActive;
    
    /**
     * 좋아요 수 (선택사항)
     */
    private Long likeCount;
    
    /**
     * Campaign 엔티티로부터 DTO 생성
     */
    public static BrandCampaignResponse fromCampaign(Campaign campaign, int currentApplicants, Long likeCount) {
        boolean isActive = campaign.getRecruitmentEndDate().isAfter(LocalDate.now()) ||
                          campaign.getRecruitmentEndDate().isEqual(LocalDate.now());
        
        return BrandCampaignResponse.builder()
                .campaignId(campaign.getId())
                .title(campaign.getTitle())
                .campaignType(campaign.getCampaignType())
                .thumbnailUrl(campaign.getThumbnailUrl())
                .productShortInfo(campaign.getProductShortInfo())
                .currentApplicants(currentApplicants)
                .maxApplicants(campaign.getMaxApplicants())
                .recruitmentEndDate(campaign.getRecruitmentEndDate())
                .approvalStatus(campaign.getApprovalStatus().name())
                .createdAt(campaign.getCreatedAt() != null ? 
                    ZonedDateTime.of(campaign.getCreatedAt(), java.time.ZoneId.systemDefault()) : null)
                .category(campaign.getCategory() != null ? 
                    CampaignCategoryResponse.builder()
                        .type(campaign.getCategory().getCategoryType().name())
                        .name(campaign.getCategory().getCategoryName())
                        .build() : null)
                .isActive(isActive)
                .likeCount(likeCount != null ? likeCount : 0L)
                .build();
    }
    
    /**
     * Campaign 엔티티로부터 DTO 생성 (좋아요 수 없이)
     */
    public static BrandCampaignResponse fromCampaign(Campaign campaign, int currentApplicants) {
        return fromCampaign(campaign, currentApplicants, null);
    }
}
