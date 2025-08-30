package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 간소화된 캠페인 목록 조회 응답 DTO
 * 필요한 정보만 포함합니다: 캠페인 타입, 제목, 제품 간략 정보, 현재 신청 인원, 최대 신청 인원, 마감일
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "간소화된 캠페인 목록 항목")
public class CampaignListSimpleResponse {

    @Schema(description = "캠페인 ID", example = "1", required = true)
    private Long id;

    @Schema(description = "상시 등록 여부", example = "false")
    private Boolean isAlwaysOpen;

    @Schema(description = "캠페인 타입", example = "인스타그램", required = true)
    private String campaignType;

    @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집", required = true)
    private String title;

    @Schema(description = "제품/서비스 간략 정보", example = "신상 라떼 + 디저트", required = true)
    private String productShortInfo;

    @Schema(description = "현재 신청 인원", example = "12", required = true)
    private Integer currentApplicants;

    @Schema(description = "최대 신청 가능 인원", example = "20", required = true)
    private Integer maxApplicants;

    @Schema(description = "모집 마감일 (상시 캠페인에서는 null)", example = "2025-06-15")
    private LocalDate recruitmentEndDate;

    @Schema(description = "썸네일 이미지 URL", example = "https://example.com/images/campaign.jpg", required = true)
    private String thumbnailUrl;

    @Schema(description = "카테고리 정보", required = true)
    private CategoryInfo category;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카테고리 정보")
    public static class CategoryInfo {
        @Schema(description = "카테고리 타입", example = "방문", required = true)
        private String type;

        @Schema(description = "카테고리 이름", example = "카페", required = true)
        private String name;
    }

    /**
     * Campaign 엔티티에서 간소화된 DTO로 변환
     */
    public static CampaignListSimpleResponse fromEntity(Campaign campaign) {
        CampaignListSimpleResponse.CampaignListSimpleResponseBuilder builder = CampaignListSimpleResponse.builder()
                .id(campaign.getId())
                .isAlwaysOpen(campaign.getIsAlwaysOpen())
                .campaignType(campaign.getCampaignType())
                .title(campaign.getTitle())
                .productShortInfo(campaign.getProductShortInfo())
                .currentApplicants(0) // 별도로 계산 필요 - 기본값은 0
                .maxApplicants(campaign.getIsAlwaysOpen() != null && campaign.getIsAlwaysOpen() ? null : campaign.getMaxApplicants()) // 상시 캠페인은 maxApplicants null 처리
                .recruitmentEndDate(campaign.getRecruitmentEndDate())
                .thumbnailUrl(campaign.getThumbnailUrl());

        // 카테고리 정보가 있으면 추가
        if (campaign.getCategory() != null) {
            CategoryInfo categoryInfo = CategoryInfo.builder()
                    .type(campaign.getCategory().getCategoryType().name())
                    .name(campaign.getCategory().getCategoryName())
                    .build();
            builder.category(categoryInfo);
        }

        return builder.build();
    }
}