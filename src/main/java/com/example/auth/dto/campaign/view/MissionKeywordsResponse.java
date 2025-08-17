package com.example.auth.dto.campaign.view;

import com.example.auth.domain.Campaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 미션 키워드 응답")
public class MissionKeywordsResponse {
    @Schema(description = "리뷰 콘텐츠에 포함되어야 하는 키워드 (배열 형태)", example = "[\"카페추천\", \"디저트맛집\", \"강남카페\"]")
    private String[] missionKeywords;
    
    public static MissionKeywordsResponse fromEntity(Campaign campaign) {
        return MissionKeywordsResponse.builder()
                .missionKeywords(campaign.getMissionInfo() != null ? campaign.getMissionInfo().getBodyKeywords() : null)
                .build();
    }
}
