package com.example.auth.dto.mission;

import com.example.auth.domain.CampaignMissionInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 캠페인 미션 정보 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "캠페인 미션 정보 응답")
public class CampaignMissionInfoResponse {

    @Schema(description = "미션 정보 ID", example = "1")
    private Long id;

    @Schema(description = "캠페인 ID", example = "42")
    private Long campaignId;

    @Schema(description = "제목에 사용할 키워드 목록", example = "[\"신제품\", \"체험\", \"리뷰\"]")
    private List<String> titleKeywords;

    @Schema(description = "본문에 사용할 키워드 목록", example = "[\"맛있다\", \"추천\", \"만족\"]")
    private List<String> bodyKeywords;

    @Schema(description = "지역 키워드", example = "[\"강남구\", \"신사동\"]")
    private List<String> locationKeywords;

    @Schema(description = "본문에 포함해야 할 영상 개수", example = "1")
    private Integer numberOfVideo;

    @Schema(description = "본문에 포함해야 할 이미지 개수", example = "3")
    private Integer numberOfImage;

    @Schema(description = "본문에 작성해야 할 글자 수", example = "500")
    private Integer numberOfText;

    @Schema(description = "본문에 지도 포함 여부", example = "false")
    private Boolean isMap;

    @Schema(description = "미션 가이드 본문", example = "상품을 실제로 사용해보시고 솔직한 후기를 작성해주세요.")
    private String missionGuide;

    @Schema(description = "미션 시작일", example = "2024-02-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate missionStartDate;

    @Schema(description = "미션 종료일", example = "2024-02-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate missionDeadlineDate;

    @Schema(description = "생성일시", example = "2024-01-15T10:00:00+09:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private ZonedDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-15T10:00:00+09:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private ZonedDateTime updatedAt;

    // 추가 계산 필드들
    @Schema(description = "미션 유효성 여부", example = "true")
    private Boolean isValidMission;

    @Schema(description = "콘텐츠 요구사항 존재 여부", example = "true")
    private Boolean hasContentRequirements;

    @Schema(description = "키워드 설정 여부", example = "true")
    private Boolean hasKeywords;

    @Schema(description = "미션 진행 상태", example = "UPCOMING")
    private MissionStatus missionStatus;

    @Schema(description = "총 미션 기간 (일)", example = "14")
    private Long missionDurationDays;

    /**
     * 미션 진행 상태 열거형
     */
    public enum MissionStatus {
        @Schema(description = "시작 예정")
        UPCOMING,
        @Schema(description = "진행 중")
        ACTIVE,
        @Schema(description = "종료됨")
        COMPLETED,
        @Schema(description = "날짜 미설정")
        NOT_SCHEDULED
    }

    /**
     * 엔티티로부터 DTO 생성
     */
    public static CampaignMissionInfoResponse fromEntity(CampaignMissionInfo entity) {
        if (entity == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        MissionStatus status = calculateMissionStatus(entity.getMissionStartDate(), 
                                                     entity.getMissionDeadlineDate(), 
                                                     today);
        
        Long durationDays = calculateDurationDays(entity.getMissionStartDate(), 
                                                 entity.getMissionDeadlineDate());

        return CampaignMissionInfoResponse.builder()
                .id(entity.getId())
                .campaignId(entity.getCampaign() != null ? entity.getCampaign().getId() : null)
                .titleKeywords(entity.getTitleKeywords() != null ? 
                              Arrays.asList(entity.getTitleKeywords()) : null)
                .bodyKeywords(entity.getBodyKeywords() != null ? 
                             Arrays.asList(entity.getBodyKeywords()) : null)
                .numberOfVideo(entity.getNumberOfVideo())
                .numberOfImage(entity.getNumberOfImage())
                .numberOfText(entity.getNumberOfText())
                .isMap(entity.getIsMap())
                .missionGuide(entity.getMissionGuide())
                .missionStartDate(entity.getMissionStartDate())
                .missionDeadlineDate(entity.getMissionDeadlineDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isValidMission(entity.isValidMission())
                .hasContentRequirements(entity.hasContentRequirements())
                .hasKeywords(entity.hasKeywords())
                .missionStatus(status)
                .missionDurationDays(durationDays)
                .build();
    }

    /**
     * 미션 상태 계산
     */
    private static MissionStatus calculateMissionStatus(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate == null || endDate == null) {
            return MissionStatus.NOT_SCHEDULED;
        }
        
        if (today.isBefore(startDate)) {
            return MissionStatus.UPCOMING;
        } else if (today.isAfter(endDate)) {
            return MissionStatus.COMPLETED;
        } else {
            return MissionStatus.ACTIVE;
        }
    }

    /**
     * 미션 기간 계산 (일 단위)
     */
    private static Long calculateDurationDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1; // 시작일 포함
    }

    /**
     * 요약 정보용 DTO (간소화된 버전)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "캠페인 미션 정보 요약")
    public static class Summary {
        
        @Schema(description = "미션 정보 ID", example = "1")
        private Long id;

        @Schema(description = "미션 시작일", example = "2024-02-01")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate missionStartDate;

        @Schema(description = "미션 종료일", example = "2024-02-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate missionDeadlineDate;

        @Schema(description = "미션 진행 상태", example = "ACTIVE")
        private MissionStatus missionStatus;

        @Schema(description = "총 콘텐츠 요구사항 수", example = "3")
        private Integer totalContentRequirements;

        @Schema(description = "총 키워드 수", example = "5")
        private Integer totalKeywords;

        public static Summary fromEntity(CampaignMissionInfo entity) {
            if (entity == null) {
                return null;
            }

            LocalDate today = LocalDate.now();
            MissionStatus status = calculateMissionStatus(entity.getMissionStartDate(), 
                                                         entity.getMissionDeadlineDate(), 
                                                         today);

            int totalContent = 0;
            if (entity.getNumberOfVideo() != null && entity.getNumberOfVideo() > 0) totalContent++;
            if (entity.getNumberOfImage() != null && entity.getNumberOfImage() > 0) totalContent++;
            if (entity.getNumberOfText() != null && entity.getNumberOfText() > 0) totalContent++;
            if (entity.getIsMap() != null && entity.getIsMap()) totalContent++;

            int totalKeywords = 0;
            if (entity.getTitleKeywords() != null) totalKeywords += entity.getTitleKeywords().length;
            if (entity.getBodyKeywords() != null) totalKeywords += entity.getBodyKeywords().length;

            return Summary.builder()
                    .id(entity.getId())
                    .missionStartDate(entity.getMissionStartDate())
                    .missionDeadlineDate(entity.getMissionDeadlineDate())
                    .missionStatus(status)
                    .totalContentRequirements(totalContent)
                    .totalKeywords(totalKeywords)
                    .build();
        }
    }
}
