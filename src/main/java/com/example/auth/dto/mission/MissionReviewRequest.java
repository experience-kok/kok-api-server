package com.example.auth.dto.mission;

import com.example.auth.domain.MissionSubmission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 미션 검토 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "미션 검토 요청")
public class MissionReviewRequest {

    @NotNull(message = "검토 상태는 필수입니다")
    @Schema(description = "검토 상태", required = true, 
            allowableValues = {"APPROVED", "REVISION_REQUESTED"},
            example = "APPROVED")
    private MissionSubmission.ReviewStatus reviewStatus;

    @Size(max = 1000, message = "피드백은 1000자 이하여야 합니다")
    @Schema(description = "클라이언트 피드백", 
            example = "미션을 잘 수행해주셨습니다. 다음번에는 제품 특징을 더 강조해주세요.", 
            maxLength = 1000)
    private String clientFeedback;

    @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다")
    @Schema(description = "클라이언트 평점 (1-5점)", example = "4", minimum = "1", maximum = "5")
    private Integer clientRating;

    @Schema(description = "수정 요청 사유 (수정 요청 시 필수)", 
            example = "제품명이 정확히 표기되지 않았습니다. 필수 키워드가 누락되었습니다.")
    private String revisionReason;
}
