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
 * 클라이언트용 유저 미션 이력 응답 DTO (간소화 버전)
 * - completionDate만 포함 (submittedAt, reviewedAt, clientFeedback 제외)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "클라이언트용 유저 미션 이력 응답 (간소화 - completionDate만 포함)")
public class ClientUserMissionHistoryResponse {

    @Schema(description = "미션 이력 ID", example = "1", required = true)
    private Long id;

    @Schema(description = "캠페인 정보", required = true)
    private CampaignInfo campaign;

    @Schema(description = "미션 정보", required = true)
    private ClientMissionInfo mission;

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
     * 클라이언트용 미션 정보 DTO (간소화)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "클라이언트용 미션 정보 (간소화 - missionUrl, completionDate, isCompleted)")
    public static class ClientMissionInfo {
        @Schema(description = "미션 URL", example = "https://instagram.com/p/abc123", required = true)
        private String missionUrl;

        @Schema(description = "완료 일시", example = "2024-03-16T10:30:00Z", nullable = true)
        private ZonedDateTime completionDate;

        @Schema(description = "미션 완료 여부", example = "true", required = true)
        private Boolean isCompleted;
    }

    public static List<ClientUserMissionHistoryResponse> fromEntities(List<UserMissionHistory> entities) {
        return entities.stream()
                .map(ClientUserMissionHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public static ClientUserMissionHistoryResponse fromEntity(UserMissionHistory entity) {
        if (entity == null) {
            return null;
        }

        return ClientUserMissionHistoryResponse.builder()
                .id(entity.getId())
                .campaign(CampaignInfo.builder()
                        .title(entity.getCampaignTitle())
                        .category(entity.getCampaignCategory())
                        .build())
                .mission(ClientMissionInfo.builder()
                        .missionUrl(entity.getSubmissionUrl())
                        .completionDate(entity.getCompletionDate())
                        .isCompleted(entity.getCompletionDate() != null)
                        .build())
                .build();
    }

    /**
     * MissionSubmission에서 응답 DTO 생성 (진행 중인 미션용)
     */
    public static ClientUserMissionHistoryResponse fromMissionSubmission(com.example.auth.domain.MissionSubmission submission) {
        if (submission == null) {
            return null;
        }

        return ClientUserMissionHistoryResponse.builder()
                .id(submission.getId())
                .campaign(CampaignInfo.builder()
                        .title(submission.getCampaignApplication().getCampaign().getTitle())
                        .category(submission.getCampaignApplication().getCampaign().getCategory().getCategoryName())
                        .build())
                .mission(ClientMissionInfo.builder()
                        .missionUrl(submission.getSubmissionUrl())
                        .completionDate(submission.isCompleted() ? submission.getReviewedAt() : null)
                        .isCompleted(submission.isCompleted())
                        .build())
                .build();
    }
}
