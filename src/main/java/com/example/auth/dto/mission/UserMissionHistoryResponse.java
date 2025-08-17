package com.example.auth.dto.mission;

import com.example.auth.domain.UserMissionHistory;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 유저 미션 이력 응답 DTO (포트폴리오용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "유저 미션 이력 응답 (포트폴리오)")
public class UserMissionHistoryResponse {

    @Schema(description = "미션 이력 ID", example = "1")
    private Long id;

    @Schema(description = "캠페인 제목", example = "이탈리안 레스토랑 신메뉴 체험단")
    private String campaignTitle;

    @Schema(description = "캠페인 카테고리", example = "맛집")
    private String campaignCategory;

    @Schema(description = "플랫폼 타입", example = "인스타그램")
    private String platformType;

    @Schema(description = "미션 링크 URL", example = "https://instagram.com/p/xyz123")
    private String submissionUrl;

    @Schema(description = "완료 일시", example = "2024-03-15T14:30:00Z")
    private ZonedDateTime completionDate;

    @Schema(description = "클라이언트 평점 (1-5점)", example = "4")
    private Integer clientRating;

    @Schema(description = "클라이언트 리뷰", example = "미션을 성실히 수행해주셨습니다.")
    private String clientReview;

    /**
     * 엔티티에서 DTO로 변환
     */
    public static UserMissionHistoryResponse fromEntity(UserMissionHistory entity) {
        if (entity == null) {
            return null;
        }

        return UserMissionHistoryResponse.builder()
                .id(entity.getId())
                .campaignTitle(entity.getCampaignTitle())
                .campaignCategory(entity.getCampaignCategory())
                .platformType(entity.getPlatformType())
                .submissionUrl(entity.getSubmissionUrl())
                .completionDate(entity.getCompletionDate())
                .clientRating(entity.getClientRating())
                .clientReview(entity.getClientReview())
                .build();
    }
}
