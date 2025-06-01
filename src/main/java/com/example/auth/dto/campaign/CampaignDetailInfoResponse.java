package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 캠페인 상세 정보 조회 응답 DTO
 * (제품/서비스 상세정보, 선정기준, 리뷰제출 마감일, 참가자 선정일)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDetailInfoResponse {
    
    private Long campaignId;
    private String productShortInfo;    // 제공 제품/서비스 간단 정보
    private String productDetails;      // 제공 제품/서비스 상세 정보
    private String selectionCriteria;   // 선정 기준
    private LocalDate reviewDeadlineDate;   // 리뷰 제출 마감일
    private LocalDate selectionDate;        // 참가자 선정일
    private LocalDate applicationDeadlineDate; // 신청 마감일
    
    public static CampaignDetailInfoResponse fromEntity(Campaign campaign) {
        return CampaignDetailInfoResponse.builder()
                .campaignId(campaign.getId())
                .productShortInfo(campaign.getProductShortInfo())
                .productDetails(campaign.getProductDetails())
                .selectionCriteria(campaign.getSelectionCriteria())
                .reviewDeadlineDate(campaign.getReviewDeadlineDate())
                .selectionDate(campaign.getSelectionDate())
                .applicationDeadlineDate(campaign.getApplicationDeadlineDate())
                .build();
    }
}
