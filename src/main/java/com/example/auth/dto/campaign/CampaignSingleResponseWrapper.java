package com.example.auth.dto.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 개별 캠페인 정보 조회 응답을 위한 Wrapper DTO
 * 일관된 응답 구조를 위해 모든 종류의 캠페인 정보를 campaign 객체 안에 포함시킵니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "개별 캠페인 정보 응답 Wrapper")
public class CampaignSingleResponseWrapper<T> {
    
    @Schema(description = "캠페인 정보")
    private T campaign;
    
    /**
     * 정적 팩토리 메서드: 어떤 타입의 캠페인 정보든 캠페인 필드에 포함시키는 Wrapper 생성
     */
    public static <T> CampaignSingleResponseWrapper<T> of(T campaignData) {
        return CampaignSingleResponseWrapper.<T>builder()
                .campaign(campaignData)
                .build();
    }
}
