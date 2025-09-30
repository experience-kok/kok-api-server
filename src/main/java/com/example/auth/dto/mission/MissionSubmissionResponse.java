package com.example.auth.dto.mission;

import com.example.auth.domain.MissionSubmission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 미션 목록 응답 DTO (클라이언트용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "미션 목록 응답")
public class MissionSubmissionResponse {

    @Schema(description = "미션 제출 ID", example = "1", required = true)
    private Long id;

    @Schema(description = "인플루언서 정보", required = true)
    private InfluencerInfo  user;

    @Schema(description = "캠페인 정보", required = true)
    private CampaignInfo campaign;

    @Schema(description = "미션 정보", required = true)
    private MissionInfo mission;

    /**
     * 인플루언서 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인플루언서 정보")
    public static class InfluencerInfo  {
        @Schema(description = "유저 ID", example = "45", required = true)
        private Long id;

        @Schema(description = "닉네임", example = "맛집탐험가", required = true)
        private String nickname;

        @Schema(description = "성별", example = "MALE", required = true)
        private String gender;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", nullable = true)
        private String profileImage;
    }

    /**
     * 캠페인 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캠페인 정보")
    public static class CampaignInfo {
        @Schema(description = "캠페인 ID", example = "123", required = true)
        private Long id;

        @Schema(description = "캠페인 제목", example = "이탈리안 레스토랑 신메뉴 체험단", required = true)
        private String title;

        @Schema(description = "캠페인 타입", example = "인스타그램", required = true)
        private String campaignType;
    }

    /**
     * 미션 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "미션 정보")
    public static class MissionInfo {
        @Schema(description = "미션 URL", example = "https://instagram.com/p/abc123", required = true)
        private String missionUrl;

        @Schema(description = "제출 일시", example = "2024-03-15T14:30:00Z", required = true)
        private ZonedDateTime submittedAt;

        @Schema(description = "검토 일시", example = "2024-03-16T10:30:00Z", nullable = true)
        private ZonedDateTime reviewedAt;

        @Schema(description = "클라이언트 피드백", example = "미션을 잘 수행해주셨습니다.", nullable = true)
        private String clientFeedback;
    }

    public static List<MissionSubmissionResponse> fromEntities(List<MissionSubmission> entities) {
        return entities.stream()
                .map(MissionSubmissionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public static MissionSubmissionResponse fromEntity(MissionSubmission entity) {
        if (entity == null) {
            return null;
        }

        return MissionSubmissionResponse.builder()
                .id(entity.getId())
                .user(InfluencerInfo.builder()
                        .id(entity.getCampaignApplication().getUser().getId())
                        .nickname(entity.getCampaignApplication().getUser().getNickname())
                        .gender(entity.getCampaignApplication().getUser().getGender() != null ?
                                entity.getCampaignApplication().getUser().getGender().name() :
                                "UNKNOWN")
                        .profileImage(entity.getCampaignApplication().getUser().getProfileImg())
                        .build())
                .campaign(CampaignInfo.builder()
                        .id(entity.getCampaignApplication().getCampaign().getId())
                        .title(entity.getCampaignApplication().getCampaign().getTitle())
                        .campaignType(entity.getCampaignApplication().getCampaign().getCampaignType())
                        .build())
                .mission(MissionInfo.builder()
                        .missionUrl(entity.getSubmissionUrl())
                        .submittedAt(entity.getSubmittedAt())
                        .reviewedAt(entity.getReviewedAt())
                        .clientFeedback(entity.getClientFeedback())
                        .build())
                .build();
    }
}
