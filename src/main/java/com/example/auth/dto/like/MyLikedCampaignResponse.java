package com.example.auth.dto.like;

import com.example.auth.domain.Campaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 내가 좋아요한 캠페인 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내가 좋아요한 캠페인 정보")
public class MyLikedCampaignResponse {

    @Schema(description = "캠페인 ID", example = "123")
    private Long campaignId;

    @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
    private String title;

    @Schema(description = "캠페인 타입", example = "인스타그램")
    private String campaignType;

    @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumbnail.jpg")
    private String thumbnailUrl;

    @Schema(description = "현재 신청자 수", example = "8")
    private int currentApplicants;

    @Schema(description = "최대 신청자 수", example = "15")
    private int maxApplicants;

    @Schema(description = "모집 마감일", example = "2027-12-12")
    private String recruitmentEndDate;

    @Schema(description = "총 좋아요 수", example = "42")
    private long likeCount;

    @Schema(description = "좋아요한 시간", example = "2025-07-29T10:30:00")
    private LocalDateTime likedAt;

    @Schema(description = "카테고리 정보")
    private CategoryInfo category;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카테고리 정보")
    public static class CategoryInfo {
        @Schema(description = "카테고리 타입", example = "방문")
        private String type;

        @Schema(description = "카테고리명", example = "맛집")
        private String name;
    }

    /**
     * Campaign 엔티티로부터 MyLikedCampaignResponse 생성
     */
    public static MyLikedCampaignResponse fromCampaign(Campaign campaign, long likeCount, LocalDateTime likedAt) {
        return MyLikedCampaignResponse.builder()
                .campaignId(campaign.getId())
                .title(campaign.getTitle())
                .campaignType(campaign.getCampaignType())
                .thumbnailUrl(campaign.getThumbnailUrl())
                .currentApplicants(campaign.getCurrentApplicantCount()) // 수정된 메서드명
                .maxApplicants(campaign.getMaxApplicants())
                .recruitmentEndDate(campaign.getRecruitmentEndDate() != null ? 
                        campaign.getRecruitmentEndDate().toString() : null)
                .likeCount(likeCount)
                .likedAt(likedAt)
                .category(CategoryInfo.builder()
                        .type(campaign.getCategory() != null ? campaign.getCategory().getCategoryType().name() : null)
                        .name(campaign.getCategory() != null ? campaign.getCategory().getCategoryName() : null)
                        .build())
                .build();
    }
}
