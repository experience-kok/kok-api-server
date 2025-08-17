package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 캠페인 미션 정보 생성/수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "캠페인 미션 정보 요청")
public class CampaignMissionInfoRequest {

    @Schema(description = "제목에 사용할 키워드 목록", example = "[\"신제품\", \"체험\", \"리뷰\"]")
    private List<String> titleKeywords;

    @Schema(description = "본문에 사용할 키워드 목록", example = "[\"맛있다\", \"추천\", \"만족\"]")
    private List<String> bodyKeywords;

    @Schema(description = "지역 키워드 (방문 캠페인일 때만 사용)", example = "[\"강남구\", \"신사동\"]")
    private List<String> locationKeywords;

    @Schema(description = "본문에 포함해야 할 영상 개수", example = "1", minimum = "0")
    @Min(value = 0, message = "영상 개수는 0 이상이어야 합니다")
    @Max(value = 10, message = "영상 개수는 10개를 초과할 수 없습니다")
    private Integer numberOfVideo;

    @Schema(description = "본문에 포함해야 할 이미지 개수", example = "3", minimum = "0")
    @Min(value = 0, message = "이미지 개수는 0 이상이어야 합니다")
    @Max(value = 20, message = "이미지 개수는 20개를 초과할 수 없습니다")
    private Integer numberOfImage;

    @Schema(description = "본문에 작성해야 할 글자 수", example = "500", minimum = "0")
    @Min(value = 0, message = "글자 수는 0 이상이어야 합니다")
    @Max(value = 10000, message = "글자 수는 10,000자를 초과할 수 없습니다")
    private Integer numberOfText;

    @Schema(description = "본문에 지도 포함 여부", example = "false")
    @Builder.Default
    private Boolean isMap = false;

    @Schema(description = "미션 가이드 본문", example = "상품을 실제로 사용해보시고 솔직한 후기를 작성해주세요.")
    @Size(max = 5000, message = "미션 가이드는 5000자를 초과할 수 없습니다")
    private String missionGuide;

    @Schema(description = "미션 시작일", example = "2024-02-01")
    @NotNull(message = "미션 시작일은 필수입니다")
    @FutureOrPresent(message = "미션 시작일은 오늘 이후여야 합니다")
    private LocalDate missionStartDate;

    @Schema(description = "미션 종료일", example = "2024-02-15")
    @NotNull(message = "미션 종료일은 필수입니다")
    @Future(message = "미션 종료일은 미래 날짜여야 합니다")
    private LocalDate missionDeadlineDate;

    /**
     * 날짜 유효성 검증
     */
    @AssertTrue(message = "미션 종료일은 시작일보다 늦어야 합니다")
    private boolean isValidDateRange() {
        if (missionStartDate == null || missionDeadlineDate == null) {
            return true; // null 체크는 @NotNull에서 처리
        }
        return !missionDeadlineDate.isBefore(missionStartDate);
    }

    /**
     * 콘텐츠 요구사항 유효성 검증
     */
    @AssertTrue(message = "최소 하나 이상의 콘텐츠 요구사항을 설정해야 합니다")
    private boolean hasContentRequirements() {
        return (numberOfVideo != null && numberOfVideo > 0) ||
               (numberOfImage != null && numberOfImage > 0) ||
               (numberOfText != null && numberOfText > 0) ||
               (isMap != null && isMap);
    }

    /**
     * 키워드 목록의 중복 제거 및 정리
     */
    public void cleanKeywords() {
        if (titleKeywords != null) {
            titleKeywords = titleKeywords.stream()
                    .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();
        }

        if (bodyKeywords != null) {
            bodyKeywords = bodyKeywords.stream()
                    .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();
        }

        if (locationKeywords != null) {
            locationKeywords = locationKeywords.stream()
                    .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();
        }
    }

    /**
     * 기본값 설정
     */
    public void setDefaults() {
        if (numberOfVideo == null) numberOfVideo = 0;
        if (numberOfImage == null) numberOfImage = 0;
        if (numberOfText == null) numberOfText = 0;
        if (isMap == null) isMap = false;
    }
}
