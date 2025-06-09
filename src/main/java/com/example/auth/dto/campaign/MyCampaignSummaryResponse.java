package com.example.auth.dto.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 내 캠페인 요약 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 캠페인 요약 정보 응답")
public class MyCampaignSummaryResponse {
    
    @Schema(description = "사용자 역할", example = "USER")
    private String role;
    
    @Schema(description = "카테고리별 요약 정보")
    private Map<String, CategorySummary> summary;
    
    /**
     * 카테고리별 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카테고리 요약 정보")
    public static class CategorySummary {
        @Schema(description = "카운트", example = "5")
        private Integer count;
        
        @Schema(description = "레이블", example = "지원")
        private String label;
    }
}
