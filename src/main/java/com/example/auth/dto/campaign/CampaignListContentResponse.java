package com.example.auth.dto.campaign;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 목록 콘텐츠만 포함한 응답을 위한 Swagger 문서화 클래스
 * 페이징 정보 없이 캠페인 목록만 포함한 응답 형식 (includePaging=false 경우)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 목록만 포함한 응답 (페이징 정보 없음)")
public class CampaignListContentResponse {
    
    @Schema(description = "응답 상태", example = "success", required = true)
    private String status;
    
    @Schema(description = "응답 메시지", example = "캠페인 목록 조회 성공", required = true)
    private String message;
    
    @Schema(description = "응답 데이터 (캠페인 목록만 포함)", required = true)
    private List<CampaignListResponse> data;
    
    @Schema(description = "응답 코드", example = "200", required = true)
    private int code;
}