package com.example.auth.dto.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * USER 역할의 캠페인 요약 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "USER 역할의 캠페인 요약 정보 응답")
public class UserCampaignSummaryResponse {
    
    @Schema(description = "사용자 역할", example = "USER")
    private String role;
    
    @Schema(description = "인플루언서 신청 요약 정보")
    private UserSummary summary;
    
    /**
     * USER 역할의 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인플루언서 신청 요약 정보")
    public static class UserSummary {
        @Schema(description = "총 지원한 신청")
        private CategorySummary applied;
        
        @Schema(description = "대기중인 신청")
        private CategorySummary pending;
        
        @Schema(description = "선정된 신청")
        private CategorySummary selected;
        
        @Schema(description = "반려된 신청")
        private CategorySummary rejected;
        
        @Schema(description = "완료된 신청")
        private CategorySummary completed;
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
