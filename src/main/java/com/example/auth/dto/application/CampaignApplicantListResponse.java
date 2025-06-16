package com.example.auth.dto.application;

import com.example.auth.dto.common.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 캠페인 신청자 목록 응답 Wrapper DTO
 * 신청자 목록과 페이징 정보를 구조화된 형태로 제공합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 신청자 목록 응답")
public class CampaignApplicantListResponse {
    
    @Schema(description = "캠페인 기본 정보")
    private CampaignBasicInfo campaign;
    
    @Schema(description = "신청자 목록")
    private List<CampaignApplicantResponse> applicants;
    
    @Schema(description = "페이징 정보")
    private PaginationInfo pagination;
    
    /**
     * 캠페인 기본 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캠페인 기본 정보")
    public static class CampaignBasicInfo {
        @Schema(description = "캠페인 ID", example = "42")
        private Long id;
        
        @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
        private String title;
        
        @Schema(description = "전체 신청자 수", example = "15")
        private long totalApplicants;
    }
    
    /**
     * 페이징 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "페이징 정보")
    public static class PaginationInfo {
        @Schema(description = "현재 페이지 번호 (1부터 시작)", example = "1")
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
    
    /**
     * PageResponse를 이용해 응답 생성
     */
    public static CampaignApplicantListResponse from(
            Long campaignId, 
            String campaignTitle, 
            long totalApplicants,
            PageResponse<CampaignApplicantResponse> pageResponse) {
        
        return CampaignApplicantListResponse.builder()
                .campaign(CampaignBasicInfo.builder()
                        .id(campaignId)
                        .title(campaignTitle)
                        .totalApplicants(totalApplicants)
                        .build())
                .applicants(pageResponse.getContent())
                .pagination(PaginationInfo.builder()
                        .pageNumber(pageResponse.getPageNumber() + 1) // 1부터 시작하도록 변환
                        .pageSize(pageResponse.getPageSize())
                        .totalPages(pageResponse.getTotalPages())
                        .totalElements(pageResponse.getTotalElements())
                        .first(pageResponse.isFirst())
                        .last(pageResponse.isLast())
                        .build())
                .build();
    }
}
