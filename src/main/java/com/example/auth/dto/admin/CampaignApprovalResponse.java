package com.example.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 캠페인 승인/거절 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "캠페인 승인/거절 응답")
public class CampaignApprovalResponse {

    @Schema(description = "캠페인 ID", example = "123")
    private Long campaignId;

    @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집")
    private String title;

    @Schema(description = "승인 상태", example = "APPROVED", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
    private String approvalStatus;

    @Schema(description = "승인/거절 코멘트", example = "모든 조건을 만족하여 승인합니다.")
    private String approvalComment;

    @Schema(description = "승인 처리 일시", example = "2025-07-14T15:30:00Z")
    private ZonedDateTime approvalDate;

    @Schema(description = "승인한 관리자 ID", example = "1")
    private Long approvedBy;

    @Schema(description = "승인한 관리자 이름", example = "김관리자")
    private String approverName;
}
