package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignMissionInfoResponse {
    private Long id;
    private String missionGuide;  // 마크다운 형식
    private String[] missionKeywords;
    
    public static CampaignMissionInfoResponse fromEntity(Campaign campaign) {
        return CampaignMissionInfoResponse.builder()
                .id(campaign.getId())
                .missionGuide(campaign.getMissionGuide())
                .missionKeywords(campaign.getMissionKeywords())
                .build();
    }
}
