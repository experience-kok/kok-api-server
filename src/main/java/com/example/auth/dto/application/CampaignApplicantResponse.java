package com.example.auth.dto.application;

import com.example.auth.domain.CampaignApplication;
import com.example.auth.domain.UserSnsPlatform;
import com.example.auth.constant.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 캠페인 신청자 정보 응답 DTO
 * 캠페인에 신청한 사용자의 상세 정보를 제공합니다.
 */
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

    @Schema(description = "캠페인 타입에 맞는 SNS 플랫폼 주소")
    private String snsUrl;

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

        @Schema(description = "사용자 닉네임", example = "인플루언서닉네임")
        private String nickname;

        @Schema(description = "전화번호", example = "010-1234-5678")
        private String phone;

        @Schema(description = "성별 (MALE: 남성, FEMALE: 여성, UNKNOWN: 미설정)", example = "FEMALE")
        private String gender;
    }

    /**
     * CampaignApplication 엔티티에서 DTO로 변환
     *
     * @param application  캠페인 신청 엔티티
     * @param snsPlatforms 사용자의 SNS 플랫폼 목록
     * @return 변환된 DTO
     */
    public static CampaignApplicantResponse fromEntity(CampaignApplication application, List<UserSnsPlatform> snsPlatforms) {
        // 캠페인 타입에 맞는 SNS URL 찾기
        String campaignType = application.getCampaign().getCampaignType();
        String snsUrl = findMatchingSnsUrl(campaignType, snsPlatforms);

        return CampaignApplicantResponse.builder()
                .applicationId(application.getId())
                .user(UserInfo.builder()
                        .id(application.getUser().getId())
                        .nickname(application.getUser().getNickname())
                        .phone(application.getUser().getPhone())
                        .gender(application.getUser().getGender() != null ?
                                application.getUser().getGender().name() :
                                Gender.UNKNOWN.name())
                        .build())
                .snsUrl(snsUrl)
                .build();
    }

    /**
     * 캠페인 타입에 맞는 SNS URL 찾기
     *
     * @param campaignType 캠페인 타입 (INSTAGRAM, YOUTUBE, BLOG 등)
     * @param snsPlatforms 사용자의 SNS 플랫폼 목록
     * @return 해당 플랫폼의 URL, 없으면 null
     */
    private static String findMatchingSnsUrl(String campaignType, List<UserSnsPlatform> snsPlatforms) {
        if (campaignType == null || snsPlatforms == null || snsPlatforms.isEmpty()) {
            return null;
        }

        // 캠페인 타입을 대문자로 변환하여 매칭
        String upperCaseType = campaignType.toUpperCase();

        return snsPlatforms.stream()
                .filter(sns -> upperCaseType.equals(sns.getPlatformType()))
                .map(UserSnsPlatform::getAccountUrl)
                .findFirst()
                .orElse(null);
    }
}
