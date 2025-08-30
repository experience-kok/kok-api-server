package com.example.auth.dto.campaign;

import com.example.auth.constant.CampaignProgressStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 진행 상태 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "캠페인 진행 상태 응답")
public class CampaignProgressResponse {

    @Schema(description = "캠페인 ID", example = "123")
    private Long campaignId;

    @Schema(description = "캠페인 제목", example = "이탈리안 레스토랑 신메뉴 체험단")
    private String campaignTitle;

    @Schema(description = "상시 캠페인 여부", example = "false")
    @JsonProperty("isAlwaysOpen")
    private boolean isAlwaysOpen;

    @Schema(description = "진행 상태 정보")
    private ProgressInfo progress;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "진행 상태 정보")
    public static class ProgressInfo {

        @Schema(description = "현재 진행 상태", example = "MISSION_IN_PROGRESS")
        private CampaignProgressStatus status;

        @Schema(description = "진행 상태 메시지", example = "미션 진행중")
        private String message;
    }

    // 기존 빌더 패턴 호환을 위한 헬퍼 메서드
    public static CampaignProgressResponse of(Long campaignId, String campaignTitle,
                                              boolean isAlwaysOpen, CampaignProgressStatus progressStatus,
                                              String progressMessage) {
        return CampaignProgressResponse.builder()
                .campaignId(campaignId)
                .campaignTitle(campaignTitle)
                .isAlwaysOpen(isAlwaysOpen)
                .progress(ProgressInfo.builder()
                        .status(progressStatus)
                        .message(progressMessage)
                        .build())
                .build();
    }
}