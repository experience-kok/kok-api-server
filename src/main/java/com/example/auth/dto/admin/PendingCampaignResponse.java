package com.example.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * 승인 대기 캠페인 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "승인 대기 캠페인 정보")
public class PendingCampaignResponse {

    @Schema(description = "캠페인 ID", example = "123")
    private Long id;

    @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집")
    private String title;

    @Schema(description = "캠페인 타입", example = "인스타그램")
    private String campaignType;

    @Schema(description = "썸네일 이미지 URL", example = "https://example.com/images/cafe.jpg")
    private String thumbnailUrl;

    @Schema(description = "제품 간단 정보", example = "시그니처 음료 2잔 + 디저트 1개 무료 제공")
    private String productShortInfo;

    @Schema(description = "최대 신청자 수", example = "10")
    private Integer maxApplicants;

    @Schema(description = "모집 시작일", example = "2025-08-01")
    private LocalDate recruitmentStartDate;

    @Schema(description = "모집 종료일", example = "2025-08-15")
    private LocalDate recruitmentEndDate;

    @Schema(description = "승인 상태", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "캠페인 생성 일시", example = "2025-07-14T15:30:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "캠페인 생성자 정보")
    private CreatorInfo creator;

    @Schema(description = "카테고리 정보")
    private CategoryInfo category;

    @Schema(description = "업체 정보")
    private CompanyInfo company;

    /**
     * 캠페인 생성자 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "캠페인 생성자 정보")
    public static class CreatorInfo {
        @Schema(description = "사용자 ID", example = "1")
        private Long id;

        @Schema(description = "사용자 이름", example = "김클라이언트")
        private String name;

        @Schema(description = "이메일", example = "client@example.com")
        private String email;

        @Schema(description = "계정 타입", example = "KAKAO")
        private String accountType;
    }

    /**
     * 카테고리 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "캠페인 카테고리 정보")
    public static class CategoryInfo {
        @Schema(description = "카테고리 ID", example = "1")
        private Long id;

        @Schema(description = "카테고리 타입", example = "방문")
        private String categoryType;

        @Schema(description = "카테고리 이름", example = "카페")
        private String categoryName;
    }

    /**
     * 업체 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "업체 정보")
    public static class CompanyInfo {
        @Schema(description = "업체 ID", example = "1")
        private Long id;

        @Schema(description = "업체명", example = "맛있는 카페")
        private String companyName;

        @Schema(description = "사업자등록번호", example = "123-45-67890")
        private String businessRegistrationNumber;

        @Schema(description = "담당자명", example = "김담당")
        private String contactPerson;

        @Schema(description = "연락처", example = "010-1234-5678")
        private String phoneNumber;
    }
}
