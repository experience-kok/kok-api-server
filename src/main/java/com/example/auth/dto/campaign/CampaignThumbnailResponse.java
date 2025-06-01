package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 썸네일 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignThumbnailResponse {
    
    private Long campaignId;
    private String thumbnailUrl;
    
    public static CampaignThumbnailResponse fromEntity(Campaign campaign) {
        return CampaignThumbnailResponse.builder()
                .campaignId(campaign.getId())
                .thumbnailUrl(campaign.getThumbnailUrl())
                .build();
    }
}
