package com.example.auth.dto.mission;

import com.example.auth.domain.MissionSubmission;
import com.example.auth.dto.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 미션 제출 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "미션 제출 응답")
public class MissionSubmissionResponse {

    @Schema(description = "미션 제출 ID", example = "1")
    private Long id;

    @Schema(description = "미션 링크 URL", example = "https://instagram.com/p/xyz123")
    private String submissionUrl;

    @Schema(description = "미션 제목", example = "맛집 체험 후기 - 이탈리안 레스토랑")
    private String submissionTitle;

    @Schema(description = "미션 설명", example = "파스타와 와인을 체험하고 솔직한 후기를 작성했습니다.")
    private String submissionDescription;

    @Schema(description = "플랫폼 타입", example = "인스타그램")
    private String platformType;

    @Schema(description = "제출 일시", example = "2024-03-15T14:30:00Z")
    private ZonedDateTime submittedAt;

    @Schema(description = "검토 일시", example = "2024-03-16T10:30:00Z")
    private ZonedDateTime reviewedAt;

    @Schema(description = "검토 상태", example = "PENDING")
    private MissionSubmission.ReviewStatus reviewStatus;

    @Schema(description = "클라이언트 피드백", example = "미션을 잘 수행해주셨습니다.")
    private String clientFeedback;

    @Schema(description = "클라이언트 평점", example = "4")
    private Integer clientRating;

    @Schema(description = "수정 요청 횟수", example = "1")
    private Integer revisionCount;

    @Schema(description = "인플루언서 정보")
    private UserSummaryDto influencer;

    @Schema(description = "캠페인 정보")
    private CampaignSummaryDto campaign;

    /**
     * 엔티티에서 DTO로 변환
     */
    public static MissionSubmissionResponse fromEntity(MissionSubmission entity) {
        if (entity == null) {
            return null;
        }

        return MissionSubmissionResponse.builder()
                .id(entity.getId())
                .submissionUrl(entity.getSubmissionUrl())
                .submissionTitle(entity.getSubmissionTitle())
                .submissionDescription(entity.getSubmissionDescription())
                .platformType(entity.getPlatformType())
                .submittedAt(entity.getSubmittedAt())
                .reviewedAt(entity.getReviewedAt())
                .reviewStatus(entity.getReviewStatus())
                .clientFeedback(entity.getClientFeedback())
                .revisionCount(entity.getRevisionCount())
                .influencer(UserSummaryDto.fromEntity(entity.getCampaignApplication().getUser()))
                .campaign(CampaignSummaryDto.fromEntity(entity.getCampaignApplication().getCampaign()))
                .build();
    }

    /**
     * 인플루언서 요약 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인플루언서 요약 정보")
    public static class UserSummaryDto {
        @Schema(description = "유저 ID", example = "1")
        private Long id;

        @Schema(description = "닉네임", example = "맛집탐험가")
        private String nickname;

        @Schema(description = "프로필 이미지 URL")
        private String profileImageUrl;

        public static UserSummaryDto fromEntity(com.example.auth.domain.User user) {
            if (user == null) {
                return null;
            }
            return UserSummaryDto.builder()
                    .id(user.getId())
                    .nickname(user.getNickname())
                    .profileImageUrl(user.getProfileImg()) // profileImg 필드 사용
                    .build();
        }
    }

    /**
     * 캠페인 요약 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캠페인 요약 정보")
    public static class CampaignSummaryDto {
        @Schema(description = "캠페인 ID", example = "1")
        private Long id;

        @Schema(description = "캠페인 제목", example = "이탈리안 레스토랑 신메뉴 체험단")
        private String title;

        @Schema(description = "캠페인 타입", example = "인스타그램")
        private String campaignType;

        public static CampaignSummaryDto fromEntity(com.example.auth.domain.Campaign campaign) {
            if (campaign == null) {
                return null;
            }
            return CampaignSummaryDto.builder()
                    .id(campaign.getId())
                    .title(campaign.getTitle())
                    .campaignType(campaign.getCampaignType())
                    .build();
        }
    }
}
