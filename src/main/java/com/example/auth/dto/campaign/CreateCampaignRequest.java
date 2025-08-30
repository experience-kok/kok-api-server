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
 * 캠페인의 기본 정보, 제품 정보, 일정 정보, 미션 정보, 담당자 정보, 방문 정보 등
 * 캠페인 등록에 필요한 모든 정보를 포함하고 있습니다.
 * 
 * CLIENT 권한 사용자는 이미 사업자 정보(업체명, 사업자번호)가 등록되어 있으므로
 * 담당자 정보(contactPerson, phoneNumber)만 입력받습니다.
 * 
 * 방문형 캠페인일 경우 visitInfo가 저장되며,
 * 배송형 캠페인일 경우 visitInfo는 무시됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 등록 요청", 
        example = """
                {
                  "isAlwaysOpen": false,
                  "thumbnailUrl": "https://example.com/images/campaign.jpg",
                  "campaignType": "인스타그램",
                  "title": "인스타 감성 카페 체험단 모집",
                  "productShortInfo": "시그니처 음료 2잔 무료 제공",
                  "maxApplicants": 10,
                  "productDetails": "인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.",
                  "recruitmentStartDate": "2025-06-01",
                  "recruitmentEndDate": "2025-06-15",
                  "selectionDate": "2025-06-16",
                  "selectionCriteria": "인스타그램 팔로워 1000명 이상, 카페 리뷰 경험이 있는 분",
                  "missionKeywords": ["카페추천", "디저트맛집", "강남카페"],
                  "missionInfo": {
                    "titleKeywords": ["카페추천", "인스타감성"],
                    "bodyKeywords": ["맛있다", "분위기좋다", "추천"],
                    "numberOfVideo": 1,
                    "numberOfImage": 5,
                    "numberOfText": 300,
                    "isMap": true,
                    "missionGuide": "1. 카페 방문 시 직원에게 체험단임을 알려주세요.\\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.\\n3. 인스타그램에 위치 태그와 함께 솔직한 후기를 작성해주세요.",
                    "missionStartDate": "2025-06-17",
                    "missionDeadlineDate": "2025-06-30"
                  },
                  "category": {
                    "type": "방문",
                    "name": "카페"
                  },
                  "companyInfo": {
                    "contactPerson": "김담당",
                    "phoneNumber": "010-1234-5678"
                  },
                  "visitInfo": {
                    "homepage": "https://delicious-cafe.com",
                    "contactPhone": "02-123-4567",
                    "visitAndReservationInfo": "평일 10시-22시 방문 가능, 사전 예약 필수",
                    "businessAddress": "서울특별시 강남구 테헤란로 123",
                    "businessDetailAddress": "123빌딩 5층",
                    "lat": 37.5665,
                    "lng": 126.9780
                  }
                }
                """)
public class CreateCampaignRequest {

    @Schema(description = "상시 등록 여부 - true일 경우 상시 캠페인으로 등록 (방문형만 가능)", 
            example = "false")
    @Builder.Default
    private Boolean isAlwaysOpen = false;

    @Schema(description = "캠페인 썸네일 이미지 URL - 캠페인 목록에서 표시될 대표 이미지", 
            example = "https://example.com/images/campaign.jpg")
    private String thumbnailUrl;

    @NotBlank(message = "캠페인 타입은 필수예요.")
    @Size(max = 50, message = "캠페인 타입은 최대 50자까지 입력 가능해요.")
    @Schema(description = "캠페인 진행 플랫폼 - 인플루언서가 리뷰를 게시할 SNS 플랫폼", 
            example = "인스타그램", 
            allowableValues = {"인스타그램", "블로그", "유튜브", "틱톡"}, 
            required = true)
    private String campaignType;

    @NotBlank(message = "제목은 필수예요.")
    @Size(max = 200, message = "제목은 최대 200자까지 입력 가능해요.")
    @Schema(description = "캠페인 제목 - 인플루언서들에게 노출될 캠페인 이름", 
            example = "인스타 감성 카페 체험단 모집", 
            required = true)
    private String title;

    @NotBlank(message = "제품 요약 정보는 필수예요.")
    @Size(max = 50, message = "제품 요약 정보는 최대 50자까지 입력 가능해요.")
    @Schema(description = "제공 제품/서비스 간략 정보 - 캠페인에서 제공하는 혜택을 간단히 요약 (10~20글자 권장)", 
            example = "시그니처 음료 2잔 무료 제공", 
            required = true)
    private String productShortInfo;

    @Schema(description = "최대 신청 가능 인원 수 - 이 캠페인에 신청할 수 있는 인플루언서의 최대 수", 
            example = "10")
    @Min(value = 1, message = "최대 신청 인원은 1명 이상이어야 해요.")
    private Integer maxApplicants;

    @NotBlank(message = "제품 상세 정보는 필수예요.")
    @Schema(description = "제공 제품/서비스 상세 정보 - 캠페인에서 제공하는 혜택과 체험 내용에 대한 자세한 설명", 
            example = "인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.", 
            required = true)
    private String productDetails;

    @NotNull(message = "모집 시작일은 필수예요.")
    @Schema(description = "모집 시작일 - 캠페인이 공개되어 인플루언서들이 신청을 시작할 수 있는 날짜", 
            example = "2025-06-01", 
            required = true)
    private LocalDate recruitmentStartDate;

    @Schema(description = "모집 종료일 - 캠페인 모집이 마감되는 날짜 (상시 캠페인에서는 null 가능)", 
            example = "2025-06-15")
    private LocalDate recruitmentEndDate;

    @Schema(description = "참여자 선정일 - 신청자 중에서 최종 참여자를 선정하여 발표하는 날짜 (상시 캠페인에서는 null 가능)", 
            example = "2025-06-16")
    private LocalDate selectionDate;

    @Schema(description = "선정 기준 - 인플루언서 선정 시 고려할 기준 (팔로워 수, 전문성, 활동 이력 등)", 
            example = "인스타그램 팔로워 1000명 이상, 카페 리뷰 경험이 있는 분")
    private String selectionCriteria;

    @Schema(description = "미션 키워드 - 리뷰 작성 시 포함해야 할 키워드 목록", 
            example = "[\"카페추천\", \"디저트맛집\", \"강남카페\"]")
    private List<String> missionKeywords;

    @Valid
    @Schema(description = "미션 정보 - 선정된 인플루언서가 수행해야 할 미션 관련 정보")
    private MissionInfo missionInfo;

    @Valid
    @NotNull(message = "카테고리 정보는 필수예요.")
    @Schema(description = "캠페인 카테고리 정보 - ID 대신 직관적인 타입과 이름으로 지정",
            example = "{\"type\": \"방문\", \"name\": \"카페\"}",
            required = true)
    private CategoryInfo category;

    @Valid
    @Schema(description = "업체 담당자 정보 - 캠페인 문의 시 연락할 담당자 정보")
    private CompanyInfo companyInfo;

    @Valid
    @Schema(description = "방문 정보 - 방문형 캠페인에서만 사용되는 정보")
    private VisitInfo visitInfo;

    /**
     * 미션 정보 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "캠페인 미션 정보")
    public static class MissionInfo {
        @Schema(description = "제목에 사용할 키워드 목록 (선택사항)", example = "[\"신제품\", \"체험\", \"리뷰\"]")
        private List<String> titleKeywords;

        @Schema(description = "본문에 사용할 키워드 목록", example = "[\"맛있다\", \"추천\", \"만족\"]")
        private List<String> bodyKeywords;

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
        @Builder.Default
        private Boolean isMap = false;

        @Schema(description = "미션 가이드 본문", 
                example = "1. 카페 방문 시 직원에게 체험단임을 알려주세요.\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.\n3. 인스타그램에 사진과 함께 솔직한 후기를 작성해주세요.")
        private String missionGuide;

        @Schema(description = "미션 시작일 (상시 캠페인에서는 선택사항)", example = "2025-06-17")
        private LocalDate missionStartDate;

        @Schema(description = "미션 종료일 (상시 캠페인에서는 선택사항)", example = "2025-06-30")
        private LocalDate missionDeadlineDate;
    }

    /**
     * 카테고리 정보 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "캠페인 카테고리 정보")
    public static class CategoryInfo {
        @NotBlank(message = "카테고리 타입은 필수예요.")
        @Schema(description = "카테고리 타입 - 캠페인 진행 방식을 구분하는 대분류 (상시 캠페인은 방문형만 가능)",
                example = "방문",
                allowableValues = {"방문", "배송"},
                required = true)
        private String type;

        @NotBlank(message = "카테고리명은 필수예요.")
        @Schema(description = "카테고리명 - 제품/서비스 분야를 나타내는 세부 분류\n" +
                              "• 방문형: 맛집, 카페, 뷰티, 숙박\n" +
                              "• 배송형: 식품, 화장품, 생활용품, 패션, 잡화\n" +
                              "• 상시 캠페인: 카페, 맛집, 뷰티, 숙박만 가능",
                example = "카페",
                required = true)
        private String name;
    }

    /**
     * 업체 담당자 정보 DTO (간소화)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "업체 담당자 정보")
    public static class CompanyInfo {
        @Size(max = 50, message = "담당자명은 50자를 초과할 수 없어요.")
        @Schema(description = "담당자명 - 캠페인 관련 문의 시 연락할 담당자 이름", 
                example = "김담당")
        private String contactPerson;

        @Size(max = 20, message = "연락처는 20자를 초과할 수 없어요.")
        @Schema(description = "연락처 - 담당자의 전화번호 또는 연락 가능한 번호", 
                example = "010-1234-5678")
        private String phoneNumber;
    }

    /**
     * 방문 정보 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "방문 정보 - 방문형 캠페인에서만 사용")
    public static class VisitInfo {
        @Schema(description = "공식 홈페이지 주소 (선택사항)", example = "https://example.com")
        private String homepage;
        
        @Schema(description = "일반 유저에게 공개되는 연락처", example = "02-123-4567")
        private String contactPhone;
        
        @Schema(description = "방문 및 예약 안내", example = "평일 10시-22시 방문 가능, 사전 예약 필수")
        private String visitAndReservationInfo;
        
        @Schema(description = "사업장 주소", example = "서울특별시 강남구 테헤란로 123")
        private String businessAddress;
        
        @Schema(description = "사업장 상세 주소", example = "123빌딩 5층")
        private String businessDetailAddress;
        
        @Schema(description = "위도", example = "37.5665")
        private Double lat;
        
        @Schema(description = "경도", example = "126.9780")
        private Double lng;
    }

    /**
     * 요청 DTO를 Campaign 엔티티로 변환
     *
     * @param creator  캠페인 생성자
     * @param category 캠페인 카테고리
     * @return Campaign 엔티티
     */
    public Campaign toEntity(User creator, CampaignCategory category) {
        // 상시 캠페인인 경우 maxApplicants를 null로 처리
        Integer finalMaxApplicants = (this.isAlwaysOpen != null && this.isAlwaysOpen) ? null : this.maxApplicants;
        
        return Campaign.builder()
                .creator(creator)
                .company(null) // 업체 정보 없이 생성
                .thumbnailUrl(this.thumbnailUrl)
                .campaignType(this.campaignType)
                .title(this.title)
                .productShortInfo(this.productShortInfo)
                .maxApplicants(finalMaxApplicants)  // 상시 캠페인은 null로 처리
                .productDetails(this.productDetails)
                .recruitmentStartDate(this.recruitmentStartDate)
                .recruitmentEndDate(this.recruitmentEndDate)
                .selectionDate(this.selectionDate)
                .selectionCriteria(this.selectionCriteria)
                .isAlwaysOpen(this.isAlwaysOpen != null ? this.isAlwaysOpen : false)
                .category(category)
                .approvalStatus(Campaign.ApprovalStatus.PENDING)
                .applications(new ArrayList<>())
                .build();
    }
}
