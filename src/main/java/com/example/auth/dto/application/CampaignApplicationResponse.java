package com.example.auth.dto.application;

import com.example.auth.domain.CampaignApplication;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 캠페인 신청 응답 DTO
 * 캠페인 신청 정보를 클라이언트에게 제공하기 위한 응답 객체입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "캠페인 신청 정보 응답",
    title = "CampaignApplicationResponse"
)
public class CampaignApplicationResponse {

    @Schema(
        description = "신청 정보의 고유 식별자", 
        example = "15",
        title = "신청 ID"
    )
    private Long id;

    @Schema(
        description = "신청한 캠페인의 고유 식별자", 
        example = "42",
        title = "캠페인 ID"
    )
    private Long campaignId;

    @Schema(
        description = "캠페인 제목", 
        example = "신상 음료 체험단 모집",
        title = "캠페인 제목"
    )
    private String campaignTitle;

    @Schema(
        description = "신청한 사용자의 고유 식별자", 
        example = "5",
        title = "사용자 ID"
    )
    private Long userId;

    @Schema(
        description = "신청한 사용자의 닉네임", 
        example = "인플루언서닉네임",
        title = "사용자 닉네임"
    )
    private String userNickname;

    @Schema(
        description = "신청 상태 (pending: 대기중, approved: 선정됨, rejected: 거절됨, completed: 완료됨)", 
        example = "pending",
        allowableValues = {"pending", "approved", "rejected", "completed"},
        title = "신청 상태"
    )
    private String status;

    @Schema(
        description = "신청 생성 시간", 
        example = "2023-05-17T14:30:15",
        title = "생성 시간",
        format = "date-time"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "신청 정보 마지막 수정 시간", 
        example = "2023-05-17T14:30:15",
        title = "수정 시간",
        format = "date-time"
    )
    private LocalDateTime updatedAt;

    /**
     * 엔티티를 DTO로 변환합니다.
     * @param application 변환할 신청 엔티티
     * @return 변환된 DTO
     */
    public static CampaignApplicationResponse fromEntity(CampaignApplication application) {
        return CampaignApplicationResponse.builder()
                .id(application.getId())
                .campaignId(application.getCampaign().getId())
                .campaignTitle(application.getCampaign().getTitle())
                .userId(application.getUser().getId())
                .userNickname(application.getUser().getNickname())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}