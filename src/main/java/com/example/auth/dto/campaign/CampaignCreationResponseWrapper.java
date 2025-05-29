package com.example.auth.dto.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 생성 응답을 위한 Wrapper DTO
 * 일관된 응답 구조를 위해 모든 종류의 캠페인 정보를 campaign 객체 안에 포함시킵니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 생성 응답 Wrapper")
public class CampaignCreationResponseWrapper {
    
    @Schema(description = "캠페인 정보")
    private CreateCampaignResponse campaign;
    
    /**
     * 정적 팩토리 메서드: 캠페인 생성 응답을 Wrapper로 감싸서 생성
     */
    public static CampaignCreationResponseWrapper of(CreateCampaignResponse campaignResponse) {
        return CampaignCreationResponseWrapper.builder()
                .campaign(campaignResponse)
                .build();
    }
}
