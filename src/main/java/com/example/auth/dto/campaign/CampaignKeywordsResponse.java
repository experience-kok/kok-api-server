package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 캠페인 필수 포함 키워드 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignKeywordsResponse {
    
    private Long campaignId;
    private List<String> missionKeywords;    // 필수 포함 키워드 목록
    
    public static CampaignKeywordsResponse fromEntity(Campaign campaign) {
        List<String> keywords = Collections.emptyList();
        
        if (campaign.getMissionKeywords() != null && campaign.getMissionKeywords().length > 0) {
            // String[] 배열을 List<String>으로 변환
            keywords = Arrays.asList(campaign.getMissionKeywords())
                    .stream()
                    .map(String::trim)
                    .filter(keyword -> !keyword.isEmpty())
                    .toList();
        }
        
        return CampaignKeywordsResponse.builder()
                .campaignId(campaign.getId())
                .missionKeywords(keywords)
                .build();
    }
}
