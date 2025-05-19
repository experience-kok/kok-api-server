package com.example.auth.dto.campaign;

import com.example.auth.dto.common.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 목록 페이지 응답을 위한 Swagger 문서화 클래스
 * 실제로는 BaseResponse<PageResponse<CampaignListResponse>>로 반환되지만,
 * Swagger 문서화를 위해 별도 클래스로 정의
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 목록 조회 응답")
public class CampaignListPageResponse {
    
    @Schema(description = "응답 상태", example = "success", required = true)
    private String status;
    
    @Schema(description = "응답 메시지", example = "캠페인 목록 조회 성공", required = true)
    private String message;
    
    @Schema(description = "응답 데이터 (페이지네이션 정보 포함)", required = true)
    private PageResponse<CampaignListResponse> data;
    
    @Schema(description = "캠페인 목록 페이지네이션 정보에 대한 설명")
    public static class PageInfo {
        @Schema(description = "현재 페이지의 캠페인 목록 (각 캠페인은 모든 상세 정보 포함)")
        private CampaignListResponse[] content;
        
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int pageNumber;
        
        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;
        
        @Schema(description = "전체 페이지 수", example = "5")
        private int totalPages;
        
        @Schema(description = "전체 항목 수", example = "42")
        private long totalElements;
        
        @Schema(description = "현재 페이지가 첫 페이지인지 여부", example = "true")
        private boolean first;
        
        @Schema(description = "현재 페이지가 마지막 페이지인지 여부", example = "false")
        private boolean last;
    }
}