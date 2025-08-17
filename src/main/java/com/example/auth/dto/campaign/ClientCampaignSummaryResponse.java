package com.example.auth.dto.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CLIENT 역할의 캠페인 요약 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CLIENT 역할의 캠페인 요약 정보 응답")
public class ClientCampaignSummaryResponse {
    
    @Schema(description = "사용자 역할", example = "CLIENT")
    private String role;
    
    @Schema(description = "기업 캠페인 요약 정보")
    private ClientSummary summary;
    
    /**
     * CLIENT 역할의 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "기업 캠페인 요약 정보")
    public static class ClientSummary {
        @Schema(description = "관리자 승인 대기중인 캠페인")
        private CategorySummary pending;
        
        @Schema(description = "승인되어 활성화된 캠페인")
        private CategorySummary approved;
        
        @Schema(description = "관리자가 거절한 캠페인")
        private CategorySummary rejected;
        
        @Schema(description = "승인됐지만 신청기간이 종료된 캠페인")
        private CategorySummary expired;
    }
    
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
        
        @Schema(description = "레이블", example = "대기중")
        private String label;
    }
}
