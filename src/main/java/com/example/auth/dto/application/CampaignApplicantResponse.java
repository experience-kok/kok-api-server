package com.example.auth.dto.application;

import com.example.auth.domain.CampaignApplication;
import com.example.auth.domain.UserSnsPlatform;
import com.example.auth.constant.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 캠페인 신청자 정보 응답 DTO
 * 캠페인에 신청한 사용자의 상세 정보를 제공합니다.
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description = "캠페인 신청자 정보 응답",
        title = "CampaignApplicantResponse"
)
public class CampaignApplicantResponse {

    @Schema(
            description = "신청 정보의 고유 식별자",
            example = "15",
            title = "신청 ID"
    )
    private Long applicationId;

    @Schema(description = "신청자 정보")
    private UserInfo user;

    @Schema(description = "신청자의 모든 SNS 플랫폼 정보")
    private List<SnsInfo> allSnsUrls;

    @Schema(
            description = "미션 정보 - 인플루언서의 미션 제출 상태와 URL을 포함합니다", 
            nullable = true,
            implementation = MissionInfo.class
    )
    private MissionInfo mission;

    /**
     * 미션 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            description = "미션 정보 - 인플루언서의 미션 상태와 제출 URL",
            name = "MissionInfo"
    )
    public static class MissionInfo {
        @Schema(
                description = "미션 ID - 미션 제출 고유 식별자", 
                example = "123",
                nullable = true
        )
        private Long missionId;
        
        @Schema(
                description = "미션 상태", 
                example = "SUBMITTED",
                required = true,
                allowableValues = {"NOT_SUBMITTED", "SUBMITTED", "REVISION_REQUESTED", "COMPLETED"}
        )
        private String missionStatus;
        
        @Schema(
                description = "미션 URL - 인플루언서가 제출한 SNS 포스트 URL", 
                example = "https://instagram.com/p/abc123", 
                nullable = true
        )
        private String missionUrl;
    }

    /**
     * SNS 플랫폼 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "SNS 플랫폼 정보")
    public static class SnsInfo {
        @Schema(description = "플랫폼 타입", example = "INSTAGRAM")
        private String platformType;

        @Schema(description = "계정 URL", example = "https://instagram.com/username")
        private String snsUrl;
    }

    /**
     * 신청자 기본 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "신청자 기본 정보")
    public static class UserInfo {
        @Schema(description = "사용자 ID", example = "5")
        private Long id;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private String profileImage;

        @Schema(description = "사용자 닉네임", example = "인플루언서닉네임")
        private String nickname;

        @Schema(description = "전화번호", example = "010-1234-5678")
        private String phone;

        @Schema(description = "성별 (MALE: 남성, FEMALE: 여성, UNKNOWN: 미설정)", example = "FEMALE")
        private String gender;
    }

    /**
     * CampaignApplication 엔티티에서 DTO로 변환 (호환성 메서드)
     *
     * @param application  캠페인 신청 엔티티
     * @param snsPlatforms 사용자의 SNS 플랫폼 목록
     * @return 변환된 DTO (미션 정보는 NOT_SUBMITTED 상태)
     */
    public static CampaignApplicantResponse fromEntity(CampaignApplication application, List<UserSnsPlatform> snsPlatforms) {
        return fromEntity(application, snsPlatforms, null);
    }

    /**
     * CampaignApplication 엔티티에서 DTO로 변환
     *
     * @param application  캠페인 신청 엔티티
     * @param snsPlatforms 사용자의 SNS 플랫폼 목록
     * @param missionSubmission 미션 제출 정보 (nullable)
     * @return 변환된 DTO
     */
    public static CampaignApplicantResponse fromEntity(CampaignApplication application, List<UserSnsPlatform> snsPlatforms, com.example.auth.domain.MissionSubmission missionSubmission) {
        // 모든 SNS 플랫폼 정보 변환
        List<SnsInfo> allSnsUrls = snsPlatforms.stream()
                .map(sns -> SnsInfo.builder()
                        .platformType(sns.getPlatformType().toUpperCase())
                        .snsUrl(sns.getAccountUrl())
                        .build())
                .collect(Collectors.toList());
        
        // 미션 상태 계산
        MissionInfo missionInfo = calculateMissionInfo(missionSubmission);
        
        log.debug("SNS 플랫폼 정보 변환 - userId: {}, 총 플랫폼 수: {}, 미션상태: {}", 
                application.getUser().getId(), allSnsUrls.size(), 
                missionInfo != null ? missionInfo.getMissionStatus() : "NULL");

        return CampaignApplicantResponse.builder()
                .applicationId(application.getId())
                .user(UserInfo.builder()
                        .id(application.getUser().getId())
                        .profileImage(application.getUser().getProfileImg())
                        .nickname(application.getUser().getNickname())
                        .phone(application.getUser().getPhone())
                        .gender(application.getUser().getGender() != null ?
                                application.getUser().getGender().name() :
                                Gender.UNKNOWN.name())
                        .build())
                .allSnsUrls(allSnsUrls)
                .mission(missionInfo)
                .build();
    }

    /**
     * 미션 상태 계산
     */
    private static MissionInfo calculateMissionInfo(com.example.auth.domain.MissionSubmission submission) {
        if (submission == null) {
            return MissionInfo.builder()
                    .missionId(null)
                    .missionStatus(com.example.auth.constant.MissionStatus.NOT_SUBMITTED.name())
                    .missionUrl(null)
                    .build();
        }
        
        String missionStatus;
        if (submission.getReviewedAt() != null && submission.isCompleted()) {
            missionStatus = com.example.auth.constant.MissionStatus.COMPLETED.name();
        } else if (submission.getReviewedAt() != null && !submission.isCompleted()) {
            missionStatus = com.example.auth.constant.MissionStatus.REVISION_REQUESTED.name();
        } else if (submission.getSubmissionUrl() != null) {
            missionStatus = com.example.auth.constant.MissionStatus.SUBMITTED.name();
        } else {
            missionStatus = com.example.auth.constant.MissionStatus.NOT_SUBMITTED.name();
        }
        
        return MissionInfo.builder()
                .missionId(submission.getId())
                .missionStatus(missionStatus)
                .missionUrl(submission.getSubmissionUrl())
                .build();
    }


}
