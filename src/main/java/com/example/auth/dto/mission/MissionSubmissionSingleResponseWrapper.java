package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 단일 미션 제출 응답 래퍼 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "단일 미션 제출 응답 래퍼")
public class MissionSubmissionSingleResponseWrapper {

    @Schema(description = "미션 제출 ID", example = "456", required = true)
    private Long id;

    @Schema(description = "미션 정보")
    private InfluencerMissionSubmissionResponse.MissionInfo mission;

    /**
     * InfluencerMissionSubmissionResponse로부터 래퍼 생성
     */
    public static MissionSubmissionSingleResponseWrapper from(InfluencerMissionSubmissionResponse submission) {
        return MissionSubmissionSingleResponseWrapper.builder()
                .id(submission.getId())
                .mission(submission.getMission())
                .build();
    }
}
