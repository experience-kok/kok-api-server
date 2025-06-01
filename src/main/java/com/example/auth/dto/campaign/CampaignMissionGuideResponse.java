package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 미션 가이드 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignMissionGuideResponse {
    
    private Long campaignId;
    private String missionGuide;    // 미션 가이드
    
    public static CampaignMissionGuideResponse fromEntity(Campaign campaign) {
        return CampaignMissionGuideResponse.builder()
                .campaignId(campaign.getId())
                .missionGuide(campaign.getMissionGuide())
                .build();
    }
}
