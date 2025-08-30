package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 미션 제출 목록 응답 래퍼 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "미션 제출 목록 응답 래퍼")
public class MissionSubmissionListResponseWrapper {

    @Schema(description = "미션 제출 목록 정보")
    private MissionInfo mission;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "미션 제출 목록 정보")
    public static class MissionInfo {
        
        @Schema(description = "미션 제출 목록")
        private List<MissionSubmissionResponse> submissions;
        
        @Schema(description = "총 제출 수", example = "5")
        private int totalCount;
    }

    /**
     * 미션 제출 목록으로부터 래퍼 생성
     */
    public static MissionSubmissionListResponseWrapper from(List<MissionSubmissionResponse> submissions) {
        return MissionSubmissionListResponseWrapper.builder()
                .mission(MissionInfo.builder()
                        .submissions(submissions)
                        .totalCount(submissions.size())
                        .build())
                .build();
    }
}
