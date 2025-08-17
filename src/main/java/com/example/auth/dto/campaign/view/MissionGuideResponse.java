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
@Schema(description = "캠페인 미션 가이드 응답")
public class MissionGuideResponse {
    @Schema(description = "리뷰어 미션 가이드 (마크다운 형식)", example = "1. 카페 방문 시 직원에게 체험단임을 알려주세요.\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.\n3. 인스타그램에 사진과 함께 솔직한 후기를 작성해주세요.")
    private String missionGuide;
    
    public static MissionGuideResponse fromEntity(Campaign campaign) {
        return MissionGuideResponse.builder()
                .missionGuide(campaign.getMissionInfo() != null ? campaign.getMissionInfo().getMissionGuide() : null)
                .build();
    }
}
