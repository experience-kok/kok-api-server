package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 유저 미션 통계 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "유저 미션 통계 응답")
public class UserMissionStatisticsResponse {

    @Schema(description = "유저 ID", example = "1")
    private Long userId;

    @Schema(description = "총 완료 미션 수", example = "15")
    private Long totalCompletedMissions;

    @Schema(description = "평균 평점", example = "4.5")
    private Double averageRating;

    @Schema(description = "공개 미션 수", example = "12")
    private Long publicMissions;

    @Schema(description = "카테고리별 통계 (카테고리명, 미션수)")
    private List<Object[]> categoryStats;

    @Schema(description = "플랫폼별 통계 (플랫폼명, 미션수)")
    private List<Object[]> platformStats;
}
