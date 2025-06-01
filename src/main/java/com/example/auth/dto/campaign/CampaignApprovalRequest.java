package com.example.auth.dto.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 승인/거절 요청")
public class CampaignApprovalRequest {
    
    @NotNull(message = "승인 상태는 필수입니다")
    @Schema(description = "승인 상태 (APPROVED/REJECTED)", example = "APPROVED", required = true)
    private String approvalStatus;
    
    @Schema(description = "승인/거절 사유", example = "캠페인 내용이 적절합니다.")
    private String reason;
}
