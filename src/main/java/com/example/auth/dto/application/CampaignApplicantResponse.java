package com.example.auth.dto.application;

import com.example.auth.domain.CampaignApplication;
import com.example.auth.domain.UserSnsPlatform;
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

    @Schema(
        description = "신청 상태 (pending: 대기중, approved: 선정됨, rejected: 거절됨, completed: 완료됨)", 
        example = "pending",
        allowableValues = {"pending", "approved", "rejected", "completed"},
        title = "신청 상태"
    )
    private String applicationStatus;

    @Schema(description = "신청자 정보")
    private UserInfo user;

    @Schema(description = "신청자 SNS 플랫폼 목록")
    private List<SnsInfo> snsPlatforms;

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
        private String accountUrl;
        
        @Schema(description = "팔로워 수", example = "10000")
        private Integer followerCount;
    }

    /**
     * CampaignApplication 엔티티에서 DTO로 변환
     * @param application 캠페인 신청 엔티티
     * @param snsPlatforms 사용자의 SNS 플랫폼 목록
     * @return 변환된 DTO
     */
    public static CampaignApplicantResponse fromEntity(CampaignApplication application, List<UserSnsPlatform> snsPlatforms) {
        return CampaignApplicantResponse.builder()
                .applicationId(application.getId())
                .applicationStatus(application.getApplicationStatus().name().toLowerCase())
                .user(UserInfo.builder()
                        .id(application.getUser().getId())
                        .nickname(application.getUser().getNickname())
                        .phone(application.getUser().getPhone())
                        .build())
                .snsPlatforms(snsPlatforms.stream()
                        .map(sns -> SnsInfo.builder()
                                .platformType(sns.getPlatformType())
                                .accountUrl(sns.getAccountUrl())
                                .followerCount(sns.getFollowerCount())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
