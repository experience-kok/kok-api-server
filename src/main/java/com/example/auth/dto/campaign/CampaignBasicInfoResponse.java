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
public class CampaignBasicInfoResponse {
    private Long id;
    private String thumbnailUrl;
    private String campaignType;
    private String title;
    private Integer maxApplicants;
    private Integer currentApplicants;  // 임시로 0으로 설정
    private LocalDate applicationDeadlineDate;
    
    public static CampaignBasicInfoResponse fromEntity(Campaign campaign) {
        return CampaignBasicInfoResponse.builder()
                .id(campaign.getId())
                .thumbnailUrl(campaign.getThumbnailUrl())
                .campaignType(campaign.getCampaignType())
                .title(campaign.getTitle())
                .maxApplicants(campaign.getMaxApplicants())
                .currentApplicants(0)  // 임시 값
                .applicationDeadlineDate(campaign.getApplicationDeadlineDate())
                .build();
    }
}
