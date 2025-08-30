package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 유저 미션 이력 목록 응답 래퍼 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "유저 미션 이력 목록 응답 래퍼")
public class UserMissionHistoryListResponseWrapper {

    @Schema(description = "미션 이력 목록 정보")
    private MissionHistoryInfo mission;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "미션 이력 목록 정보")
    public static class MissionHistoryInfo {
        
        @Schema(description = "미션 이력 목록")
        private List<UserMissionHistoryResponse> history;
        
        @Schema(description = "총 완료 미션 수", example = "15")
        private int totalCount;
        
        @Schema(description = "평균 평점", example = "4.2")
        private Double averageRating;
    }

    /**
     * 미션 이력 목록으로부터 래퍼 생성
     */
    public static UserMissionHistoryListResponseWrapper from(List<UserMissionHistoryResponse> history) {
        return UserMissionHistoryListResponseWrapper.builder()
                .mission(MissionHistoryInfo.builder()
                        .history(history)
                        .totalCount(history.size())
                        .averageRating(0.0) // 평점 기능 제거로 인해 0.0 고정
                        .build())
                .build();
    }
}
