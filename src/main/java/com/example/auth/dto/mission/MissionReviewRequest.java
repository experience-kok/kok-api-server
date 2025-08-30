package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "클라이언트 피드백 (승인시)", example = "미션을 잘 수행해주셨습니다.", nullable = true)
    @Size(max = 500, message = "피드백은 500자 이내로 입력해주세요")
    private String clientFeedback;

    @Schema(description = "수정 요청 사유 (수정 요청시)", example = "제품명이 정확히 표기되지 않았습니다.", nullable = true)
    @Size(max = 500, message = "수정 요청 사유는 500자 이내로 입력해주세요")
    private String revisionReason;
}
