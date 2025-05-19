package com.example.auth.dto.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 캠페인 목록 조회 응답을 담는 Wrapper DTO
 * 페이징 정보와 함께 캠페인 목록을 구조화된 형태로 제공합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 목록 조회 응답")
public class CampaignListResponseWrapper {
    
    @Schema(description = "캠페인 목록")
    private List<CampaignListSimpleResponse> campaigns;
    
    @Schema(description = "페이징 정보")
    private PaginationInfo pagination;
    
    /**
     * 페이징 정보를 담는 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "페이징 정보")
    public static class PaginationInfo {
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int pageNumber;
        
        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;
        
        @Schema(description = "전체 페이지 수", example = "5")
        private int totalPages;
        
        @Schema(description = "전체 항목 수", example = "42")
        private long totalElements;
        
        @Schema(description = "첫 번째 페이지 여부", example = "true")
        private boolean first;
        
        @Schema(description = "마지막 페이지 여부", example = "false")
        private boolean last;
    }
}
