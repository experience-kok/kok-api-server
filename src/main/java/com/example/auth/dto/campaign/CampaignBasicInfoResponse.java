package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 캠페인 기본 정보 조회 응답 DTO
 * (캠페인 타입, 카테고리, 제목, 신청 인원 정보, 모집 기간)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignBasicInfoResponse {
    
    private Long campaignId;
    private String campaignType;        // 캠페인 타입 (방문형/배송형)
    private String categoryType;        // 카테고리 타입
    private String categoryName;        // 카테고리 이름
    private String title;               // 캠페인 제목
    private Integer maxApplicants;      // 최대 신청 인원
    private Integer currentApplicants;  // 현재 신청 인원
    private LocalDate recruitmentStartDate; // 모집 시작일
    private LocalDate recruitmentEndDate;   // 모집 마감일
    
    public static CampaignBasicInfoResponse fromEntity(Campaign campaign) {
        return CampaignBasicInfoResponse.builder()
                .campaignId(campaign.getId())
                .campaignType(campaign.getCampaignType())
                .categoryType(campaign.getCategory() != null ? campaign.getCategory().getCategoryType().name() : null)
                .categoryName(campaign.getCategory() != null ? campaign.getCategory().getCategoryName() : null)
                .title(campaign.getTitle())
                .maxApplicants(campaign.getMaxApplicants())
                .currentApplicants(campaign.getCurrentApplicantCount())
                .recruitmentStartDate(campaign.getRecruitmentStartDate())
                .recruitmentEndDate(campaign.getRecruitmentEndDate())
                .build();
    }
}
