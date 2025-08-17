package com.example.auth.dto.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CLIENT 역할 - 내 캠페인 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CLIENT 역할 캠페인 정보")
public class ClientCampaignResponse {
    
    @Schema(description = "캠페인 ID", example = "42")
    private Long campaignId;
    
    @Schema(description = "상시 등록 여부", example = "false")
    private Boolean isAlwaysOpen;
    
    @Schema(description = "제목", example = "신상 음료 체험단 모집")
    private String title;
    
    @Schema(description = "설명")
    private String description;
    
    @Schema(description = "이미지 URL")
    private String imageUrl;
    
    @Schema(description = "승인 상태", example = "APPROVED")
    private String approvalStatus;
    
    @Schema(description = "캠페인 상태", example = "ACTIVE")
    private String campaignStatus;
    
    @Schema(description = "생성 시간")
    private String createdAt;
    
    @Schema(description = "마지막 수정 시간")
    private String updatedAt;
    
    @Schema(description = "승인 시간")
    private String approvedAt;
    
    @Schema(description = "신청 시작일", example = "2023-05-15")
    private String applicationStartDate;
    
    @Schema(description = "신청 종료일 (상시 캠페인에서는 null)", example = "2023-05-25")
    private String applicationEndDate;
    
    @Schema(description = "체험 시작일 (상시 캠페인에서는 null)", example = "2023-05-30")
    private String experienceStartDate;
    
    @Schema(description = "체험 종료일 (상시 캠페인에서는 null)", example = "2023-06-05")
    private String experienceEndDate;
    
    @Schema(description = "모집 인원", example = "10")
    private Integer recruitmentCount;
    
    @Schema(description = "카테고리", example = "식품")
    private String category;
    
    @Schema(description = "타입", example = "배송")
    private String type;
    
    @Schema(description = "위치")
    private String location;
    
    @Schema(description = "통계 정보")
    private CampaignStatistics statistics;
    
    @Schema(description = "업체 정보")
    private CompanyInfo company;
    
    /**
     * 캠페인 통계 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캠페인 통계 정보")
    public static class CampaignStatistics {
        @Schema(description = "총 신청자 수", example = "25")
        private Integer totalApplications;
        
        @Schema(description = "선정자 수", example = "8")
        private Integer selectedCount;
        
        @Schema(description = "완료자 수", example = "0")
        private Integer completedCount;
        
        @Schema(description = "대기중 신청자 수", example = "17")
        private Integer pendingApplications;
    }
    
    /**
     * 업체 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "업체 정보")
    public static class CompanyInfo {
        @Schema(description = "업체 ID", example = "5")
        private Long id;
        
        @Schema(description = "업체명", example = "음료회사")
        private String name;
        
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        private String businessNumber;
    }
}
