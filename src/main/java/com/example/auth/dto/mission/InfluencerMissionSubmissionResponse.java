package com.example.auth.dto.mission;

import com.example.auth.domain.MissionSubmission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 미션 제출 응답 DTO (인플루언서용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "미션 제출 응답")
public class InfluencerMissionSubmissionResponse {

    @Schema(description = "미션 제출 ID", example = "456", required = true)
    private Long id;

    @Schema(description = "미션 정보", required = true)
    private MissionInfo mission;

    /**
     * 미션 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "미션 정보")
    public static class MissionInfo {
        @Schema(description = "미션 URL", example = "https://instagram.com/p/Cxy789abc", required = true)
        private String missionUrl;

        @Schema(description = "제출 일시", example = "2024-03-15T14:30:00Z", required = true)
        private ZonedDateTime submittedAt;

        @Schema(description = "클라이언트 피드백", nullable = true)
        private String clientFeedback; // 제출 직후에는 항상 null
    }

    public static InfluencerMissionSubmissionResponse fromEntity(MissionSubmission entity) {
        if (entity == null) {
            return null;
        }

        return InfluencerMissionSubmissionResponse.builder()
                .id(entity.getId())
                .mission(MissionInfo.builder()
                        .missionUrl(entity.getSubmissionUrl())
                        .submittedAt(entity.getSubmittedAt())
                        .clientFeedback(null) // 제출 직후에는 항상 null
                        .build())
                .build();
    }
}
