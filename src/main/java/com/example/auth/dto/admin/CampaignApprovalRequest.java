package com.example.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 승인/거절 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "캠페인 승인/거절 요청")
public class CampaignApprovalRequest {

    @Schema(
            description = "승인 상태", 
            example = "APPROVED",
            allowableValues = {"APPROVED", "REJECTED"}
    )
    @NotNull(message = "승인 상태는 필수입니다")
    private String approvalStatus;

    @Schema(
            description = "승인/거절 사유 또는 코멘트",
            example = "모든 조건을 만족하여 승인합니다."
    )
    @Size(max = 500, message = "코멘트는 500자 이하로 입력해주세요")
    private String comment;
}
