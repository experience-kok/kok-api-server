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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 등록 응답")
public class CreateCampaignResponse {

    @Schema(description = "캠페인 고유 식별자", example = "1", required = true)
    private Long id;
    
    @Schema(description = "상시 등록 여부", example = "false")
    private Boolean isAlwaysOpen;
    
    @Schema(description = "캠페인 썸네일 이미지 URL", example = "https://drxgfm74s70w1.cloudfront.net/campaign-images/campaign123.jpg")
    private String thumbnailUrl;
    
    @Schema(description = "캠페인 진행 플랫폼", example = "인스타그램", ref = "#/components/schemas/CampaignType")
    private String campaignType;
    
    @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집", required = true)
    private String title;
    
    @Schema(description = "제공 제품/서비스에 대한 간략 정보", example = "시그니처 음료 2잔 무료 제공", required = true)
    private String productShortInfo;
    
    @Schema(description = "최대 신청 가능 인원 수", example = "10", required = true)
    private Integer maxApplicants;
    
    @Schema(description = "모집 시작 날짜", example = "2025-05-01", required = true)
    private LocalDate recruitmentStartDate;
    
    @Schema(description = "모집 종료 날짜", example = "2025-05-15", required = true)
    private LocalDate recruitmentEndDate;
    
    @Schema(description = "참여자 선정 날짜", example = "2025-05-16", required = true)
    private LocalDate selectionDate;
    
    @Schema(description = "미션 시작일", example = "2025-05-17")
    private LocalDate reviewStartDate;
    
    @Schema(description = "리뷰 제출 마감일", example = "2025-05-30")
    private LocalDate reviewDeadlineDate;
    
    @Schema(description = "승인 상태", example = "PENDING", ref = "#/components/schemas/ApprovalStatus", required = true)
    private String approvalStatus;
    
    @Schema(description = "카테고리 정보", required = true)
    private CategoryDTO category;
    
    @Schema(description = "캠페인 등록 사용자 정보", required = true)
    private UserDTO user;
    
    @Schema(description = "캠페인 생성 시간", example = "2024-01-01T00:00:00Z", required = true)
    private ZonedDateTime createdAt;
    
    @Schema(description = "캠페인 수정 시간", example = "2024-01-01T00:00:00Z", required = true)
    private ZonedDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "카테고리 정보")
    public static class CategoryDTO {
        @Schema(description = "카테고리 ID", example = "1", required = true)
        private Long id;
        
        @Schema(description = "카테고리 타입", example = "방문", ref = "#/components/schemas/CategoryType", required = true)
        private String type;
        
        @Schema(description = "카테고리 이름", example = "카페", ref = "#/components/schemas/CategoryName", required = true)
        private String name;
    }

    public static CreateCampaignResponse fromEntity(Campaign campaign) {
        return CreateCampaignResponse.builder()
                .id(campaign.getId())
                .isAlwaysOpen(campaign.getIsAlwaysOpen())
                .thumbnailUrl(campaign.getThumbnailUrl())
                .campaignType(campaign.getCampaignType())
                .title(campaign.getTitle())
                .productShortInfo(campaign.getProductShortInfo())
                .maxApplicants(campaign.getMaxApplicants())
                .recruitmentStartDate(campaign.getRecruitmentStartDate())
                .recruitmentEndDate(campaign.getRecruitmentEndDate())
                .selectionDate(campaign.getSelectionDate())
                .reviewStartDate(campaign.getMissionInfo() != null ? campaign.getMissionInfo().getMissionStartDate() : null)
                .reviewDeadlineDate(campaign.getMissionInfo() != null ? campaign.getMissionInfo().getMissionDeadlineDate() : null)
                .approvalStatus(campaign.getApprovalStatus().name())
                .category(CategoryDTO.builder()
                        .id(campaign.getCategory().getId())
                        .type(campaign.getCategory().getCategoryType().name())
                        .name(campaign.getCategory().getCategoryName())
                        .build())
                .user(UserDTO.fromEntity(campaign.getCreator()))
                .createdAt(campaign.getCreatedAt() != null ? 
                    ZonedDateTime.of(campaign.getCreatedAt(), java.time.ZoneId.systemDefault()) : null)
                .updatedAt(campaign.getUpdatedAt() != null ? 
                    ZonedDateTime.of(campaign.getUpdatedAt(), java.time.ZoneId.systemDefault()) : null)
                .build();
    }
}
