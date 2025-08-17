package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 미션 이력 공개/비공개 설정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "미션 이력 공개/비공개 설정 요청")
public class VisibilityUpdateRequest {

    @NotNull(message = "공개 여부는 필수입니다.")
    @Schema(description = "공개 여부", example = "true", required = true)
    private Boolean isPublic;
}
