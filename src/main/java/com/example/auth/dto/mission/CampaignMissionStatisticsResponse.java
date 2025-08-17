package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 미션 통계 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "캠페인 미션 통계 응답")
public class CampaignMissionStatisticsResponse {

    @Schema(description = "캠페인 ID", example = "1")
    private Long campaignId;

    @Schema(description = "캠페인 제목", example = "맛집 체험단")
    private String campaignTitle;

    @Schema(description = "총 미션 제출 수", example = "10")
    private Long totalSubmissions;

    @Schema(description = "승인된 미션 수", example = "8")
    private Long approvedSubmissions;

    @Schema(description = "검토 대기 미션 수", example = "1")
    private Long pendingSubmissions;

    @Schema(description = "수정 요청된 미션 수", example = "1")
    private Long revisionRequested;

    @Schema(description = "평균 평점", example = "4.2")
    private Double averageRating;
}
