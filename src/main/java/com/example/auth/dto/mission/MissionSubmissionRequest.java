package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 미션 제출 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "미션 제출 요청")
public class MissionSubmissionRequest {

    @Schema(description = "미션 URL", example = "https://instagram.com/p/Cxy789abc", required = true)
    @NotBlank(message = "미션 URL은 필수입니다")
    @Pattern(regexp = "https?://.*", message = "유효한 URL 형식이어야 합니다")
    private String missionUrl;
}
