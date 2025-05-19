// 이 파일은 더 이상 사용되지 않으므로 나중에 삭제해도 됩니다.
package com.example.auth.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 신청 상태 업데이트 요청 DTO
 * 참고: 현재 사용되지 않습니다
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 신청 상태 업데이트 요청")
public class ApplicationStatusUpdateRequest {

    @NotBlank(message = "상태는 필수입니다.")
    @Pattern(regexp = "pending|rejected|completed", message = "상태는 pending, rejected, completed 중 하나여야 합니다.")
    @Schema(description = "변경할 상태", example = "completed", allowableValues = {"pending", "rejected", "completed"}, required = true)
    private String status;
}