package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignCategory;
import com.example.auth.domain.Company;
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
 * <p>
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

    @Schema(description = "캠페인 썸네일 이미지 URL - 캠페인 목록에서 표시될 대표 이미지", 
            example = "https://example.com/images/campaign.jpg")
    private String thumbnailUrl;

    @NotBlank(message = "캠페인 타입은 필수입니다.")
    @Size(max = 50, message = "캠페인 타입은 최대 50자까지 입력 가능합니다.")
    @Schema(description = "캠페인 진행 플랫폼 - 인플루언서가 리뷰를 게시할 SNS 플랫폼", 
            example = "인스타그램", 
            allowableValues = {"인스타그램", "블로그", "유튜브", "틱톡"}, 
            required = true)
    private String campaignType;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 최대 200자까지 입력 가능합니다.")
    @Schema(description = "캠페인 제목 - 인플루언서들에게 노출될 캠페인 이름", 
            example = "인스타 감성 카페 체험단 모집", 
            required = true)
    private String title;

    @NotBlank(message = "제품 요약 정보는 필수입니다.")
    @Size(max = 50, message = "제품 요약 정보는 최대 50자까지 입력 가능합니다.")
    @Schema(description = "제공 제품/서비스 간략 정보 - 캠페인에서 제공하는 혜택을 간단히 요약 (10~20글자 권장)", 
            example = "시그니처 음료 2잔 무료 제공", 
            required = true)
    private String productShortInfo;

    @NotNull(message = "최대 신청 인원은 필수입니다.")
    @Min(value = 1, message = "최대 신청 인원은 1명 이상이어야 합니다.")
    @Schema(description = "최대 신청 가능 인원 수 - 이 캠페인에 신청할 수 있는 인플루언서의 최대 수", 
            example = "10", 
            required = true)
    private Integer maxApplicants;

    @NotBlank(message = "제품 상세 정보는 필수입니다.")
    @Schema(description = "제공 제품/서비스 상세 정보 - 캠페인에서 제공하는 혜택과 체험 내용에 대한 자세한 설명", 
            example = "인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.", 
            required = true)
    private String productDetails;

    @NotNull(message = "모집 시작일은 필수입니다.")
    @Schema(description = "📅 모집 시작일 - 캠페인이 공개되어 인플루언서들이 신청을 시작할 수 있는 날짜", 
            example = "2025-06-01", 
            required = true)
    private LocalDate recruitmentStartDate;

    @NotNull(message = "모집 종료일은 필수입니다.")
    @Schema(description = "📅 모집 종료일 - 캠페인 모집이 마감되는 날짜 (이후 신청 불가)", 
            example = "2025-06-15", 
            required = true)
    private LocalDate recruitmentEndDate;

    @NotNull(message = "신청 마감일은 필수입니다.")
    @Schema(description = "📅 신청 마감일 - 인플루언서들이 캠페인에 신청할 수 있는 최종 날짜 (모집 시작일 이후여야 함)", 
            example = "2025-06-14", 
            required = true)
    private LocalDate applicationDeadlineDate;

    @NotNull(message = "선정일은 필수입니다.")
    @Schema(description = "📅 참여자 선정일 - 신청자 중에서 최종 참여자를 선정하여 발표하는 날짜 (모집 종료일 이후여야 함)", 
            example = "2025-06-16", 
            required = true)
    private LocalDate selectionDate;

    @NotNull(message = "리뷰 마감일은 필수입니다.")
    @Schema(description = "📅 리뷰 제출 마감일 - 선정된 인플루언서들이 체험 후 리뷰를 완료해야 하는 최종 날짜 (선정일 이후여야 함)", 
            example = "2025-06-30", 
            required = true)
    private LocalDate reviewDeadlineDate;

    @Schema(description = "🎯 선정 기준 - 인플루언서 선정 시 고려할 기준 (팔로워 수, 전문성, 활동 이력 등)", 
            example = "인스타그램 팔로워 1000명 이상, 카페 리뷰 경험이 있는 분")
    private String selectionCriteria;

    @Schema(description = "📋 리뷰어 미션 가이드 - 선정된 인플루언서가 수행해야 할 구체적인 미션 내용 (마크다운 형식 지원)", 
            example = "1. 카페 방문 시 직원에게 체험단임을 알려주세요.\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.\n3. 인스타그램에 사진과 함께 솔직한 후기를 작성해주세요.")
    private String missionGuide;

    @Schema(description = "🏷️ 미션 키워드 - 리뷰 콘텐츠에 반드시 포함되어야 하는 해시태그나 키워드 목록", 
            example = "[\"카페추천\", \"디저트맛집\", \"강남카페\"]")
    private String[] missionKeywords;

    @Valid
    @NotNull(message = "카테고리 정보는 필수입니다.")
    @Schema(description = "🏷️ 캠페인 카테고리 정보 - ID 대신 직관적인 타입과 이름으로 지정",
            example = "{\"type\": \"방문\", \"name\": \"카페\"}",
            required = true)
    private CategoryInfo category;

    @Valid
    @Schema(description = "🏢 업체 정보 - 캠페인을 주최하는 업체/브랜드의 상세 정보")
    private CompanyInfo companyInfo;

    @Valid
    @Schema(description = "📍 방문 위치 정보 - 방문형 캠페인의 경우 인플루언서가 방문해야 할 장소들의 상세 정보")
    private List<VisitLocationRequest> visitLocations;

    /**
     * 카테고리 정보 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "🏷️ 캠페인 카테고리 정보")
    public static class CategoryInfo {
        @NotBlank(message = "카테고리 타입은 필수입니다.")
        @Schema(description = "카테고리 타입 - 캠페인 진행 방식을 구분하는 대분류",
                example = "방문",
                allowableValues = {"방문", "배송"},
                required = true)
        private String type;

        @NotBlank(message = "카테고리명은 필수입니다.")
        @Schema(description = "카테고리명 - 제품/서비스 분야를 나타내는 세부 분류\n" +
                              "• 방문형: 맛집, 카페, 뷰티, 숙박\n" +
                              "• 배송형: 식품, 화장품, 생활용품, 패션, 잡화",
                example = "카페",
                required = true)
        private String name;
    }

    /**
     * 업체 정보 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "🏢 캠페인 주최 업체 정보")
    public static class CompanyInfo {
        @NotBlank(message = "업체명은 필수입니다.")
        @Size(max = 100, message = "업체명은 100자를 초과할 수 없습니다.")
        @Schema(description = "업체명 - 캠페인을 주최하는 회사/브랜드 이름", 
                example = "맛있는 카페", 
                required = true)
        private String companyName;

        @Size(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
        @Schema(description = "사업자등록번호 - 업체의 공식 등록번호 (선택사항)", 
                example = "123-45-67890")
        private String businessRegistrationNumber;

        @Size(max = 50, message = "담당자명은 50자를 초과할 수 없습니다.")
        @Schema(description = "담당자명 - 캠페인 관련 문의 시 연락할 담당자 이름", 
                example = "김담당")
        private String contactPerson;

        @Size(max = 20, message = "연락처는 20자를 초과할 수 없습니다.")
        @Schema(description = "연락처 - 담당자의 전화번호 또는 연락 가능한 번호", 
                example = "010-1234-5678")
        private String phoneNumber;
    }

    /**
     * 방문 위치 요청 DTO
     * <p>
     * 방문형 캠페인의 경우 체험단이 방문해야 하는 장소 정보를 담고 있습니다.
     * 주소, 좌표, 운영 정보 등 방문에 필요한 정보를 포함합니다.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "📍 방문 위치 상세 정보 (방문형 캠페인에만 해당)")
    public static class VisitLocationRequest {
        @NotBlank(message = "방문 주소는 필수입니다.")
        @Schema(description = "방문 장소 주소 - 인플루언서가 방문해야 할 정확한 주소", 
                example = "서울특별시 강남구 테헤란로 123", 
                required = true)
        private String address;

        @Schema(description = "위도 좌표 - 지도 표시 및 위치 확인용 (소수점 6자리까지)", 
                example = "37.498095")
        private java.math.BigDecimal latitude;

        @Schema(description = "경도 좌표 - 지도 표시 및 위치 확인용 (소수점 6자리까지)", 
                example = "127.027610")
        private java.math.BigDecimal longitude;

        @Schema(description = "운영시간 - 인플루언서가 방문 가능한 시간대", 
                example = "09:00 - 22:00")
        private String operatingHours;

        @Schema(description = "휴무일 - 방문하면 안 되는 날짜 정보", 
                example = "매주 월요일")
        private String closedDays;

        @Schema(description = "주차정보 - 방문 시 참고할 주차 관련 안내사항", 
                example = "발렛파킹 가능, 2시간 무료")
        private String parkingInfo;

        @Schema(description = "기타 추가 정보 - 방문 시 알아야 할 특별한 안내사항이나 주의사항", 
                example = "2층 카운터에서 체험단임을 알려주세요")
        private String additionalInfo;
    }

    /**
     * 요청 DTO를 Campaign 엔티티로 변환
     *
     * @param creator  캠페인 생성자
     * @param category 캠페인 카테고리
     * @param company  업체 정보 (요청에서 생성된 업체)
     * @return Campaign 엔티티
     */
    public Campaign toEntity(User creator, CampaignCategory category, Company company) {
        return Campaign.builder()
                .creator(creator)
                .company(company)
                .thumbnailUrl(this.thumbnailUrl)
                .campaignType(this.campaignType)
                .title(this.title)
                .productShortInfo(this.productShortInfo)
                .maxApplicants(this.maxApplicants)
                .productDetails(this.productDetails)
                .recruitmentStartDate(this.recruitmentStartDate)
                .recruitmentEndDate(this.recruitmentEndDate)
                .applicationDeadlineDate(this.applicationDeadlineDate)
                .selectionDate(this.selectionDate)
                .reviewDeadlineDate(this.reviewDeadlineDate)
                .selectionCriteria(this.selectionCriteria)
                .missionGuide(this.missionGuide)
                .missionKeywords(this.missionKeywords)
                .category(category)
                .approvalStatus(Campaign.ApprovalStatus.PENDING)
                .applications(new ArrayList<>())
                .build();
    }
}
