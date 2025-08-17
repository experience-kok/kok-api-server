package com.example.auth.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 캠페인 신청자 선정 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        title = "캠페인 신청자 선정 응답",
        description = "캠페인 신청자 선정 완료 후 응답 정보"
)
public class CampaignSelectionResponse {

    /**
     * 캠페인 ID
     */
    @Schema(description = "캠페인 ID", example = "42")
    private Long campaignId;

    /**
     * 캠페인 제목
     */
    @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
    private String campaignTitle;

    /**
     * 선정된 신청자 정보 목록
     */
    @Schema(description = "선정된 신청자 정보 목록")
    private List<SelectedApplicantInfo> selectedApplicants;

    /**
     * 선정 처리 시간
     */
    @Schema(description = "선정 처리 시간", example = "2025-01-29T15:30:00+09:00")
    private ZonedDateTime selectionProcessedAt;

    /**
     * 알림 전송 여부
     */
    @Schema(description = "알림 전송 여부", example = "true")
    private boolean notificationSent;

    /**
     * 선정된 신청자 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선정된 신청자 정보")
    public static class SelectedApplicantInfo {
        /**
         * 신청 ID
         */
        @Schema(description = "신청 ID", example = "101")
        private Long applicationId;

        /**
         * 사용자 ID
         */
        @Schema(description = "사용자 ID", example = "5")
        private Long userId;

        /**
         * 사용자 닉네임
         */
        @Schema(description = "사용자 닉네임", example = "인플루언서1")
        private String nickname;

        /**
         * 사용자 이메일
         */
        @Schema(description = "사용자 이메일", example = "influencer1@example.com")
        private String email;

        /**
         * 선정 시간
         */
        @Schema(description = "선정 시간", example = "2025-01-29T15:30:00+09:00")
        private ZonedDateTime selectedAt;
    }
}
