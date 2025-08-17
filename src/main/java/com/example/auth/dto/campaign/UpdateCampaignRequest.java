package com.example.auth.dto.campaign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 캠페인 수정 요청 DTO
 * 
 * 권한 기반 수정을 위한 DTO로, 모든 필드가 선택적(Optional)입니다.
 * 수정 권한에 따라 필드별로 다른 검증 로직이 적용됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 수정 요청")
public class UpdateCampaignRequest {

    // ===== 상시 수정 가능한 필드들 =====

    @Schema(description = "상시 등록 여부 - true일 경우 상시 캠페인으로 등록 (방문형만 가능)", 
            example = "false")
    private Boolean isAlwaysOpen;

    @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumbnail.jpg")
    private String thumbnailUrl;

    @Schema(description = "캠페인 제목", example = "신제품 체험단 모집")
    @Size(max = 200, message = "캠페인 제목은 200자 이하여야 합니다.")
    private String title;

    @Schema(description = "최대 지원 가능 인원 수", example = "10")
    @Min(value = 1, message = "최대 지원 가능 인원은 1명 이상이어야 합니다.")
    private Integer maxApplicants;

    // ===== 신청자 없을 때만 수정 가능한 필드들 =====

    @Schema(description = "캠페인 진행 플랫폼", example = "인스타그램")
    private String campaignType;

    @Schema(description = "카테고리 정보")
    @Valid
    private CategoryInfo category;

    @Schema(description = "제품/서비스 간략 정보", example = "신제품 체험 키트")
    @Size(max = 50, message = "제품 간략 정보는 50자 이하여야 합니다.")
    private String productShortInfo;

    @Schema(description = "모집 시작 날짜", example = "2024-01-01")
    private LocalDate recruitmentStartDate;

    @Schema(description = "모집 종료 날짜", example = "2024-01-15")
    private LocalDate recruitmentEndDate;

    @Schema(description = "참가자 선정일 (발표일)", example = "2024-01-20")
    private LocalDate selectionDate;

    @Schema(description = "제공되는 제품/서비스 상세 정보")
    private String productDetails;

    @Schema(description = "선정 기준")
    private String selectionCriteria;

    @Schema(description = "미션 정보")
    @Valid
    private MissionInfo missionInfo;

    // ===== 방문 정보 (상시 수정 가능) =====

    @Schema(description = "방문 정보 (방문형 캠페인만 해당)")
    @Valid
    private VisitInfo visitInfo;

    // ===== 내부 클래스들 =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카테고리 정보")
    public static class CategoryInfo {
        @Schema(description = "카테고리 타입", example = "방문", allowableValues = {"방문", "배송"})
        private String type;

        @Schema(description = "카테고리명", example = "맛집")
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "미션 정보")
    public static class MissionInfo {
        @Schema(description = "제목에 사용할 키워드 목록 (선택사항)", example = "[\"신제품\", \"체험\"]")
        private java.util.List<String> titleKeywords;

        @Schema(description = "본문에 사용할 키워드 목록", example = "[\"맛있다\", \"추천\"]")
        private java.util.List<String> bodyKeywords;

        @Schema(description = "본문에 포함해야 할 영상 개수", example = "1")
        @Min(value = 0, message = "영상 개수는 0 이상이어야 합니다")
        private Integer numberOfVideo;

        @Schema(description = "본문에 포함해야 할 이미지 개수", example = "3")
        @Min(value = 0, message = "이미지 개수는 0 이상이어야 합니다")
        private Integer numberOfImage;

        @Schema(description = "본문에 작성해야 할 글자 수", example = "500")
        @Min(value = 0, message = "글자 수는 0 이상이어야 합니다")
        private Integer numberOfText;

        @Schema(description = "본문에 지도 포함 여부", example = "false")
        private Boolean isMap;

        @Schema(description = "미션 가이드 본문")
        private String missionGuide;

        @Schema(description = "미션 시작일 (상시 캠페인에서는 선택사항)", example = "2024-02-01")
        private LocalDate missionStartDate;

        @Schema(description = "미션 종료일 (상시 캠페인에서는 선택사항)", example = "2024-02-15")
        private LocalDate missionDeadlineDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "방문 정보")
    public static class VisitInfo {
        @Schema(description = "공식 홈페이지 주소", example = "https://restaurant.com")
        private String officialWebsite;

        @Schema(description = "연락처 (공개됨)", example = "02-1234-5678")
        private String contactNumber;

        @Schema(description = "방문 및 예약 안내")
        private String visitReservationInfo;

        // 주의: 위치 정보는 수정 불가하므로 포함하지 않음
    }

    // ===== 검증 메서드들 =====

    /**
     * 날짜 유효성을 사전 검증합니다.
     * @return 유효하면 true
     */
    public boolean hasValidDates() {
        if (recruitmentStartDate == null || recruitmentEndDate == null || selectionDate == null) {
            return true; // 일부 날짜만 수정하는 경우는 서비스에서 검증
        }

        boolean campaignDatesValid = !recruitmentStartDate.isAfter(recruitmentEndDate) &&
                                   !selectionDate.isBefore(recruitmentEndDate);

        // 미션 날짜 검증
        if (missionInfo != null && missionInfo.getMissionStartDate() != null && missionInfo.getMissionDeadlineDate() != null) {
            boolean missionDatesValid = !missionInfo.getMissionStartDate().isAfter(missionInfo.getMissionDeadlineDate()) &&
                                      !missionInfo.getMissionStartDate().isBefore(selectionDate);
            return campaignDatesValid && missionDatesValid;
        }

        return campaignDatesValid;
    }

    /**
     * 수정 요청에 실제 변경사항이 있는지 확인
     * @return 변경사항이 있으면 true
     */
    public boolean hasAnyChanges() {
        return isAlwaysOpen != null || thumbnailUrl != null || title != null || maxApplicants != null ||
               campaignType != null || category != null || productShortInfo != null ||
               recruitmentStartDate != null || recruitmentEndDate != null ||
               selectionDate != null || productDetails != null || 
               selectionCriteria != null || missionInfo != null || visitInfo != null;
    }

    /**
     * 상시 수정 가능한 필드만 있는지 확인
     * @return 상시 수정 가능한 필드만 있으면 true
     */
    public boolean hasOnlyAlwaysEditableFields() {
        return (isAlwaysOpen != null || thumbnailUrl != null || title != null || maxApplicants != null || 
                hasEditableVisitInfo()) &&
               campaignType == null && category == null && productShortInfo == null &&
               recruitmentStartDate == null && recruitmentEndDate == null &&
               selectionDate == null && productDetails == null && 
               selectionCriteria == null && missionInfo == null;
    }

    /**
     * 방문 정보가 편집 가능한 필드만 포함하는지 확인
     */
    private boolean hasEditableVisitInfo() {
        return visitInfo != null && 
               (visitInfo.getOfficialWebsite() != null || 
                visitInfo.getContactNumber() != null || 
                visitInfo.getVisitReservationInfo() != null);
    }
}
