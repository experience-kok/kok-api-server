package com.example.auth.dto.like;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 좋아요 토글 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "좋아요 토글 응답")
public class LikeResponse {

    @Schema(description = "좋아요 여부", example = "true")
    private boolean liked;

    @Schema(description = "총 좋아요 수", example = "42")
    private long totalCount;

    @Schema(description = "캠페인 ID", example = "123")
    private Long campaignId;
}
