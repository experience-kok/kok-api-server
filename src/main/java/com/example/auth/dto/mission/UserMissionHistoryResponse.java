package com.example.auth.dto.mission;

import com.example.auth.domain.UserMissionHistory;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 인플루언서 미션 이력 응답 DTO (상세 정보 포함)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    description = "인플루언서 미션 이력 응답 (상세 정보 포함)",
    example = """
        {
          "id": 1,
          "campaign": {
            "title": "이탈리안 레스토랑 신메뉴 체험단",
            "category": "맛집"
          },
          "mission": {
            "missionUrl": "https://instagram.com/p/abc123",
            "isCompleted": true,
            "completionDate": "2024-03-16T10:30:00Z",
            "clientReview": "미션을 잘 수행해주셨습니다.",
            "revisionReason": null
          }
        }
        """
)
public class UserMissionHistoryResponse {

    @Schema(description = "미션 이력 ID", example = "1", required = true)
    private Long id;

    @Schema(description = "캠페인 정보", required = true)
    private CampaignInfo campaign;

    @Schema(description = "미션 정보", required = true)
    private InfluencerMissionInfo mission;

    /**
     * 캠페인 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캠페인 정보")
    public static class CampaignInfo {
        @Schema(description = "캠페인 제목", example = "이탈리안 레스토랑 신메뉴 체험단", required = true)
        private String title;

        @Schema(description = "캠페인 카테고리", example = "맛집", required = true)
        private String category;
    }

    /**
     * 인플루언서용 미션 정보 DTO (상세 정보 포함)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인플루언서용 미션 정보 (상세 - isCompleted, completionDate, clientReview, revisionReason 포함)")
    public static class InfluencerMissionInfo {
        @Schema(description = "미션 URL", example = "https://instagram.com/p/abc123", required = true)
        private String missionUrl;

        @Schema(description = "완료 여부", example = "true", required = true)
        private Boolean isCompleted; // 완료/미완료를 boolean으로 표시

        @Schema(description = "완료 일시", example = "2024-03-16T10:30:00Z", nullable = true)
        private ZonedDateTime completionDate;

        @Schema(description = "클라이언트 리뷰", example = "미션을 잘 수행해주셨습니다.", nullable = true)
        private String clientReview;

        @Schema(description = "수정 요청 사유", example = "제품명이 정확히 표기되지 않았습니다.", nullable = true)
        private String revisionReason;
    }

    public static List<UserMissionHistoryResponse> fromEntities(List<UserMissionHistory> entities) {
        return entities.stream()
                .map(UserMissionHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public static UserMissionHistoryResponse fromEntity(UserMissionHistory entity) {
        if (entity == null) {
            return null;
        }

        return UserMissionHistoryResponse.builder()
                .id(entity.getId())
                .campaign(CampaignInfo.builder()
                        .title(entity.getCampaignTitle())
                        .category(entity.getCampaignCategory())
                        .build())
                .mission(InfluencerMissionInfo.builder()
                        .missionUrl(entity.getSubmissionUrl())
                        .isCompleted(entity.getCompletionDate() != null) // boolean으로 완료 여부 표시
                        .completionDate(entity.getCompletionDate())
                        .clientReview(entity.getClientReview())
                        .revisionReason(null) // 승인된 미션은 수정 요청 사유 없음
                        .build())
                .build();
    }

    /**
     * MissionSubmission에서 응답 DTO 생성 (진행 중인 미션용)
     */
    public static UserMissionHistoryResponse fromMissionSubmission(com.example.auth.domain.MissionSubmission submission) {
        if (submission == null) {
            return null;
        }

        // 가장 최근 수정 요청 사유 조회
        String latestRevisionReason = null;
        if (!submission.getRevisions().isEmpty()) {
            latestRevisionReason = submission.getRevisions().stream()
                    .sorted((r1, r2) -> r2.getRequestedAt().compareTo(r1.getRequestedAt()))
                    .findFirst()
                    .map(com.example.auth.domain.MissionRevision::getRevisionReason)
                    .orElse(null);
        }

        return UserMissionHistoryResponse.builder()
                .id(submission.getId())
                .campaign(CampaignInfo.builder()
                        .title(submission.getCampaignApplication().getCampaign().getTitle())
                        .category(submission.getCampaignApplication().getCampaign().getCategory().getCategoryName())
                        .build())
                .mission(InfluencerMissionInfo.builder()
                        .missionUrl(submission.getSubmissionUrl())
                        .isCompleted(submission.isCompleted())
                        .completionDate(submission.isCompleted() ? submission.getReviewedAt() : null)
                        .clientReview(submission.getClientFeedback())
                        .revisionReason(latestRevisionReason) // 수정 요청 받은 경우에만 표시
                        .build())
                .build();
    }
}
