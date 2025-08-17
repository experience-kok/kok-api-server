package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "미션 링크는 필수입니다")
    @Schema(description = "미션 링크 URL", example = "https://instagram.com/p/xyz123", required = true)
    private String submissionUrl;

    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    @Schema(description = "미션 제목", example = "맛집 체험 후기 - 이탈리안 레스토랑", maxLength = 200)
    private String submissionTitle;

    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
    @Schema(description = "미션 설명", example = "파스타와 와인을 체험하고 솔직한 후기를 작성했습니다.", maxLength = 1000)
    private String submissionDescription;

    @NotBlank(message = "플랫폼 타입은 필수입니다")
    @Schema(description = "플랫폼 타입", example = "인스타그램", required = true, 
            allowableValues = {"인스타그램", "블로그", "유튜브" })
    private String platformType;
}
