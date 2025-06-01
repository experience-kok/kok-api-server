package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import com.example.auth.dto.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 등록 응답")
public class CreateCampaignResponse {

    @Schema(description = "캠페인 고유 식별자", example = "1")
    private Long id;
    
    @Schema(description = "캠페인 썸네일 이미지 URL", example = "https://example.com/images/campaign.jpg")
    private String thumbnailUrl;
    
    @Schema(description = "캠페인 진행 플랫폼", example = "인스타그램")
    private String campaignType;
    
    @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집")
    private String title;
    
    @Schema(description = "제공 제품/서비스에 대한 간략 정보", example = "시그니처 음료 2잔 무료 제공")
    private String productShortInfo;
    
    @Schema(description = "최대 신청 가능 인원 수", example = "10")
    private Integer maxApplicants;
    
    @Schema(description = "모집 시작 날짜", example = "2025-05-01")
    private LocalDate recruitmentStartDate;
    
    @Schema(description = "모집 종료 날짜", example = "2025-05-15")
    private LocalDate recruitmentEndDate;
    
    @Schema(description = "참여자 선정 날짜", example = "2025-05-16")
    private LocalDate selectionDate;
    
    @Schema(description = "리뷰 제출 마감일", example = "2025-05-30")
    private LocalDate reviewDeadlineDate;
    
    @Schema(description = "승인 상태: 'PENDING'(대기), 'APPROVED'(승인), 'REJECTED'(거절)", example = "PENDING")
    private String approvalStatus;
    
    @Schema(description = "카테고리 정보")
    private CategoryDTO category;
    
    @Schema(description = "캠페인 등록 사용자 정보")
    private UserDTO user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "카테고리 정보")
    public static class CategoryDTO {
        @Schema(description = "카테고리 ID", example = "1")
        private Long id;
        
        @Schema(description = "카테고리 타입", example = "방문")
        private String type;
        
        @Schema(description = "카테고리 이름", example = "카페")
        private String name;
    }

    public static CreateCampaignResponse fromEntity(Campaign campaign) {
        return CreateCampaignResponse.builder()
                .id(campaign.getId())
                .thumbnailUrl(campaign.getThumbnailUrl())
                .campaignType(campaign.getCampaignType())
                .title(campaign.getTitle())
                .productShortInfo(campaign.getProductShortInfo())
                .maxApplicants(campaign.getMaxApplicants())
                .recruitmentStartDate(campaign.getRecruitmentStartDate())
                .recruitmentEndDate(campaign.getRecruitmentEndDate())
                .selectionDate(campaign.getSelectionDate())
                .reviewDeadlineDate(campaign.getReviewDeadlineDate())
                .approvalStatus(campaign.getApprovalStatus().name())
                .category(CategoryDTO.builder()
                        .id(campaign.getCategory().getId())
                        .type(campaign.getCategory().getCategoryType().name())
                        .name(campaign.getCategory().getCategoryName())
                        .build())
                .user(UserDTO.fromEntity(campaign.getCreator()))
                .build();
    }
}
