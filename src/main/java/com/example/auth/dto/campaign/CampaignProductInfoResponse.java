package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignProductInfoResponse {
    private Long id;
    private String productShortInfo;
    private String productDetails;
    private LocalDate recruitmentStartDate;
    private LocalDate recruitmentEndDate;
    private LocalDate selectionDate;
    private LocalDate reviewDeadlineDate;
    
    public static CampaignProductInfoResponse fromEntity(Campaign campaign) {
        return CampaignProductInfoResponse.builder()
                .id(campaign.getId())
                .productShortInfo(campaign.getProductShortInfo())
                .productDetails(campaign.getProductDetails())
                .recruitmentStartDate(campaign.getRecruitmentStartDate())
                .recruitmentEndDate(campaign.getRecruitmentEndDate())
                .selectionDate(campaign.getSelectionDate())
                .reviewDeadlineDate(campaign.getReviewDeadlineDate())
                .build();
    }
}
