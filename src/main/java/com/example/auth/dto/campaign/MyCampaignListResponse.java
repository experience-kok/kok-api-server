package com.example.auth.dto.campaign;

import com.example.auth.dto.common.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 내 캠페인 상세 목록 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 캠페인 상세 목록 응답")
public class MyCampaignListResponse {
    
    @Schema(description = "사용자 역할", example = "USER")
    private String role;
    
    @Schema(description = "조회한 카테고리", example = "applied")
    private String category;
    
    @Schema(description = "캠페인 목록")
    private List<?> campaigns;
    
    @Schema(description = "페이징 정보")
    private PageResponse.PaginationInfo pagination;
}
