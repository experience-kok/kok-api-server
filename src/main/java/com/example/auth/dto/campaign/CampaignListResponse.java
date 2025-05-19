package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.VisitLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 캠페인 목록 조회 응답 DTO
 * 각 캠페인에 대한 종합적인 정보를 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 목록 항목 (종합 정보)")
public class CampaignListResponse {
    // 기본 정보
    @Schema(description = "캠페인 ID", example = "1", required = true)
    private Long id;
    
    @Schema(description = "썸네일 이미지 URL", example = "https://example.com/images/campaign.jpg", required = true)
    private String thumbnailUrl;
    
    @Schema(description = "캠페인 타입", example = "인스타그램", required = true)
    private String campaignType;
    
    @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집", required = true)
    private String title;
    
    @Schema(description = "최대 신청 가능 인원", example = "20", required = true)
    private Integer maxApplicants;
    
    @Schema(description = "현재 신청 인원", example = "12", required = true)
    private Integer currentApplicants;
    
    @Schema(description = "신청 마감 날짜", example = "2025-06-15", required = true)
    private LocalDate applicationDeadlineDate;
    
    // 카테고리 정보
    @Schema(description = "카테고리 타입", example = "방문", required = true)
    private String categoryType;
    
    @Schema(description = "카테고리 이름", example = "카페", required = true)
    private String categoryName;
    
    // 제품 및 일정 정보
    @Schema(description = "제공 제품/서비스에 대한 간략 정보", example = "시그니처 음료 2잔 무료 제공")
    private String productShortInfo;
    
    @Schema(description = "제공되는 제품/서비스에 대한 상세 정보", example = "인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.")
    private String productDetails;
    
    @Schema(description = "모집 시작 날짜", example = "2025-05-01")
    private LocalDate recruitmentStartDate;
    
    @Schema(description = "모집 종료 날짜", example = "2025-05-15")
    private LocalDate recruitmentEndDate;
    
    @Schema(description = "참여자 선정 날짜", example = "2025-05-16")
    private LocalDate selectionDate;
    
    @Schema(description = "리뷰 제출 마감일", example = "2025-05-30")
    private LocalDate reviewDeadlineDate;
    
    // 업체 정보
    @Schema(description = "업체/브랜드 정보", example = "2020년에 오픈한 강남 소재의 프리미엄 디저트 카페")
    private String companyInfo;
    
    @Schema(description = "캠페인 등록자 닉네임", example = "브랜드매니저")
    private String creatorNickname;
    
    // 미션 정보
    @Schema(description = "미션 가이드", example = "1. 카페 방문 시 직원에게 체험단임을 알려주세요.\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.")
    private String missionGuide;
    
    @Schema(description = "미션 키워드", example = "[\"카페추천\", \"디저트맛집\", \"강남카페\"]")
    private String[] missionKeywords;
    
    // 위치 정보
    @Schema(description = "방문 위치 정보")
    private List<VisitLocationDTO> visitLocations;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "방문 위치 정보")
    public static class VisitLocationDTO {
        @Schema(description = "방문 위치 ID", example = "1")
        private Long id;
        
        @Schema(description = "방문 장소 주소", example = "서울특별시 강남구 테헤란로 123")
        private String address;
        
        @Schema(description = "위도 좌표", example = "37.498095")
        private BigDecimal latitude;
        
        @Schema(description = "경도 좌표", example = "127.027610")
        private BigDecimal longitude;
        
        @Schema(description = "추가 장소 정보", example = "영업시간: 10:00-22:00, 주차 가능")
        private String additionalInfo;
    }
    
    /**
     * Campaign 엔티티에서 종합 DTO로 변환
     */
    public static CampaignListResponse fromEntity(Campaign campaign) {
        // 기본 정보 설정
        var builder = CampaignListResponse.builder()
                .id(campaign.getId())
                .thumbnailUrl(campaign.getThumbnailUrl())
                .campaignType(campaign.getCampaignType())
                .title(campaign.getTitle())
                .maxApplicants(campaign.getMaxApplicants())
                .currentApplicants(0) // 별도로 계산 필요 - 기본값은 0
                .applicationDeadlineDate(campaign.getApplicationDeadlineDate());
                
        // 카테고리 정보 설정
        if (campaign.getCategory() != null) {
            builder.categoryType(campaign.getCategory().getCategoryType())
                   .categoryName(campaign.getCategory().getCategoryName());
        }
        
        // 제품 및 일정 정보 설정
        builder.productShortInfo(campaign.getProductShortInfo())
               .productDetails(campaign.getProductDetails())
               .recruitmentStartDate(campaign.getRecruitmentStartDate())
               .recruitmentEndDate(campaign.getRecruitmentEndDate())
               .selectionDate(campaign.getSelectionDate())
               .reviewDeadlineDate(campaign.getReviewDeadlineDate());
        
        // 업체 정보 설정
        builder.companyInfo(campaign.getCompanyInfo());
        if (campaign.getCreator() != null) {
            builder.creatorNickname(campaign.getCreator().getNickname());
        }
        
        // 미션 정보 설정
        builder.missionGuide(campaign.getMissionGuide())
               .missionKeywords(campaign.getMissionKeywords());
        
        // 위치 정보 설정
        List<VisitLocationDTO> visitLocationDTOs = new ArrayList<>();
        if (campaign.getVisitLocations() != null && !campaign.getVisitLocations().isEmpty()) {
            visitLocationDTOs = campaign.getVisitLocations().stream()
                    .map(location -> convertToVisitLocationDTO(location))
                    .collect(Collectors.toList());
        }
        builder.visitLocations(visitLocationDTOs);
        
        return builder.build();
    }
    
    /**
     * VisitLocation 엔티티를 DTO로 변환
     */
    private static VisitLocationDTO convertToVisitLocationDTO(VisitLocation location) {
        return VisitLocationDTO.builder()
                .id(location.getId())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .additionalInfo(location.getAdditionalInfo())
                .build();
    }
    
    /**
     * Campaign 엔티티 리스트에서 DTO 리스트로 변환
     */
    public static List<CampaignListResponse> fromEntities(List<Campaign> campaigns) {
        return campaigns.stream()
                .map(CampaignListResponse::fromEntity)
                .collect(Collectors.toList());
    }
}