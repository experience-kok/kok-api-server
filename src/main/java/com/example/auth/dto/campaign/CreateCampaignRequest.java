package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignCategory;
import com.example.auth.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 캠페인 등록 요청 DTO 클래스
 * 
 * 클라이언트가 새로운 캠페인을 등록할 때 사용하는 데이터 구조입니다.
 * 캠페인의 기본 정보, 제품 정보, 일정 정보, 미션 정보, 방문 정보 등
 * 캠페인 등록에 필요한 모든 정보를 포함하고 있습니다.
 * 필수 필드는 유효성 검사를 통해 확인합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 등록 요청")
public class CreateCampaignRequest {

    @Schema(description = "캠페인 썸네일 이미지 URL", example = "https://example.com/images/campaign.jpg")
    private String thumbnailUrl;

    @NotBlank(message = "캠페인 타입은 필수입니다.")
    @Size(max = 50, message = "캠페인 타입은 최대 50자까지 입력 가능합니다.")
    @Schema(description = "캠페인 진행 플랫폼", example = "인스타그램", required = true)
    private String campaignType;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 최대 200자까지 입력 가능합니다.")
    @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집", required = true)
    private String title;

    @NotBlank(message = "제품 요약 정보는 필수입니다.")
    @Size(max = 50, message = "제품 요약 정보는 최대 50자까지 입력 가능합니다.")
    @Schema(description = "제공 제품/서비스에 대한 간략 정보 (10~20글자 내외)", example = "시그니처 음료 2잔 무료 제공", required = true)
    private String productShortInfo;

    @NotNull(message = "최대 신청 인원은 필수입니다.")
    @Min(value = 1, message = "최대 신청 인원은 1명 이상이어야 합니다.")
    @Schema(description = "최대 신청 가능 인원 수", example = "10", required = true)
    private Integer maxApplicants;

    @NotBlank(message = "제품 상세 정보는 필수입니다.")
    @Schema(description = "제공되는 제품/서비스에 대한 상세 정보", example = "인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.", required = true)
    private String productDetails;

    @NotNull(message = "모집 시작일은 필수입니다.")
    @Schema(description = "모집 시작 날짜", example = "2025-05-01", required = true)
    private LocalDate recruitmentStartDate;

    @NotNull(message = "모집 종료일은 필수입니다.")
    @Schema(description = "모집 종료 날짜", example = "2025-05-15", required = true)
    private LocalDate recruitmentEndDate;

    @NotNull(message = "선정일은 필수입니다.")
    @Schema(description = "참여자 선정 날짜", example = "2025-05-16", required = true)
    private LocalDate selectionDate;

    @NotNull(message = "리뷰 마감일은 필수입니다.")
    @Schema(description = "리뷰 제출 마감일", example = "2025-05-30", required = true)
    private LocalDate reviewDeadlineDate;

    @Schema(description = "업체/브랜드 정보", example = "2020년에 오픈한 강남 소재의 프리미엄 디저트 카페로, 유기농 재료만을 사용한 건강한 음료를 제공합니다.")
    private String companyInfo;

    @Schema(description = "리뷰어 미션 가이드 (마크다운 형식)", example = "1. 카페 방문 시 직원에게 체험단임을 알려주세요.\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.\n3. 인스타그램에 사진과 함께 솔직한 후기를 작성해주세요.")
    private String missionGuide;

    @Schema(description = "리뷰 콘텐츠에 포함되어야 하는 키워드 (배열 형태)", example = "[\"카페추천\", \"디저트맛집\", \"강남카페\"]")
    private String[] missionKeywords;

    @NotNull(message = "신청 마감일은 필수입니다.")
    @Schema(description = "신청 마감 날짜", example = "2025-05-14", required = true)
    private LocalDate applicationDeadlineDate;

    @Valid
    @NotNull(message = "카테고리 정보는 필수입니다.")
    @Schema(description = "카테고리 정보")
    private CategoryInfo category;

    /**
     * 카테고리 정보 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "카테고리 정보")
    public static class CategoryInfo {
        @NotBlank(message = "카테고리 타입은 필수입니다.")
        @Size(max = 20, message = "카테고리 타입은 최대 20자까지 입력 가능합니다.")
        @Schema(description = "카테고리 타입", example = "방문", required = true, 
               allowableValues = {"방문", "배송"})
        private String type;

        @NotBlank(message = "카테고리는 필수입니다.")
        @Size(max = 50, message = "카테고리 이름은 최대 50자까지 입력 가능합니다.")
        @Schema(description = "카테고리 이름", example = "카페", required = true, 
               allowableValues = {"맛집", "카페", "뷰티", "숙박", "식품", "화장품", "생활용품", "패션", "잡화"})
        private String name;
    }

    @Valid
    @Schema(description = "방문 정보 (위치 정보)")
    private List<VisitLocationRequest> visitLocations;

    /**
     * 방문 위치 요청 DTO
     * 
     * 방문형 캠페인의 경우 체험단이 방문해야 하는 장소 정보를 담고 있습니다.
     * 주소, 좌표, 추가 정보 등 방문에 필요한 정보를 포함합니다.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "방문 위치 정보")
    public static class VisitLocationRequest {
        @NotBlank(message = "방문 주소는 필수입니다.")
        @Schema(description = "방문 장소 주소", example = "서울특별시 강남구 테헤란로 123", required = true)
        private String address;
        
        @Schema(description = "위도 좌표 (지도 API 연동용)", example = "37.498095")
        private java.math.BigDecimal latitude;
        
        @Schema(description = "경도 좌표 (지도 API 연동용)", example = "127.027610")
        private java.math.BigDecimal longitude;
        
        @Schema(description = "추가 장소 정보 (영업시간, 주차 정보 등)", example = "영업시간: 10:00-22:00, 주차 가능")
        private String additionalInfo;
    }
    
    /**
     * 요청 DTO를 Campaign 엔티티로 변환 (사용자와 카테고리 정보 포함)
     * 
     * 클라이언트 요청 데이터를 데이터베이스에 저장 가능한 엔티티 객체로 변환합니다.
     * 생성자(User)와 카테고리(CampaignCategory) 정보는 서비스 계층에서 주입받습니다.
     * 
     * @param creator 캠페인 생성자 (클라이언트 권한을 가진 사용자)
     * @param category 선택된 캠페인 카테고리
     * @return 저장 가능한 Campaign 엔티티 객체
     */
    public Campaign toEntity(User creator, CampaignCategory category) {
        return Campaign.builder()
                .thumbnailUrl(this.thumbnailUrl)
                .campaignType(this.campaignType)
                .title(this.title)
                .productShortInfo(this.productShortInfo)
                .maxApplicants(this.maxApplicants)
                .productDetails(this.productDetails)
                .recruitmentStartDate(this.recruitmentStartDate)
                .recruitmentEndDate(this.recruitmentEndDate)
                .selectionDate(this.selectionDate)
                .reviewDeadlineDate(this.reviewDeadlineDate)
                .companyInfo(this.companyInfo)
                .missionGuide(this.missionGuide)
                .missionKeywords(this.missionKeywords)
                .applicationDeadlineDate(this.applicationDeadlineDate)
                .category(category)
                .creator(creator)
                .approvalStatus("PENDING")  // 기본값: 대기 상태로 설정
                .visitLocations(new ArrayList<>())
                .build();
    }
}
