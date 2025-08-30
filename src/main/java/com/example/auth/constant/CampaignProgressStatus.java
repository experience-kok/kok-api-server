package com.example.auth.constant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 캠페인 진행 상태를 나타내는 열거형
 * 클라이언트가 캠페인의 현재 진행 단계를 확인할 수 있습니다.
 */
@Schema(description = "캠페인 진행 상태")
public enum CampaignProgressStatus {

    @Schema(description = "모집중")
    RECRUITING("모집중"),

    @Schema(description = "지원자 모집 완료")
    RECRUITMENT_COMPLETED("지원자 모집 완료"),

    @Schema(description = "참가자 선정 완료")
    SELECTION_COMPLETED("참가자 선정 완료"),

    @Schema(description = "미션 진행중")
    MISSION_IN_PROGRESS("미션 진행중"),

    @Schema(description = "콘텐츠 검토 대기")
    CONTENT_REVIEW_PENDING("콘텐츠 검토 대기"),

    @Schema(description = "상시 캠페인")
    ALWAYS_OPEN("상시 캠페인은 진행 단계가 표시되지 않습니다");

    private final String description;

    CampaignProgressStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return this.description;
    }
}