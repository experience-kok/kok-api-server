package com.example.auth.dto.location;

import com.example.auth.domain.CampaignLocation;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캠페인 위치 정보 응답 DTO (간소화)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "캠페인 위치 정보 응답",
    description = "캠페인의 위치 정보"
)
public class CampaignLocationResponse {

    @Schema(description = "위치 ID", example = "1")
    private Long id;

    @Schema(description = "캠페인 ID", example = "42")
    private Long campaignId;

    @Schema(description = "위도", example = "37.5665")
    @JsonProperty("lat")
    private Double latitude;

    @Schema(description = "경도", example = "126.9780")
    @JsonProperty("lng")
    private Double longitude;

    @Schema(description = "공식 홈페이지 주소", example = "https://example.com")
    private String homepage;

    @Schema(description = "일반 유저에게 공개되는 연락처", example = "02-123-4567")
    private String contactPhone;

    @Schema(description = "방문 및 예약 안내", example = "평일 10시-22시 방문 가능, 사전 예약 필수")
    private String visitAndReservationInfo;

    @Schema(description = "사업장 주소", example = "서울특별시 강남구 테헤란로 123")
    private String businessAddress;

    @Schema(description = "사업장 상세 주소", example = "123빌딩 5층")
    private String businessDetailAddress;

    /**
     * CampaignLocation 엔티티에서 DTO로 변환
     * @param location 캠페인 위치 엔티티
     * @return 변환된 DTO
     */
    public static CampaignLocationResponse fromEntity(CampaignLocation location) {
        if (location == null) {
            return null;
        }

        return CampaignLocationResponse.builder()
                .id(location.getId())
                .campaignId(location.getCampaign().getId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .homepage(location.getHomepage())
                .contactPhone(location.getContactPhone())
                .visitAndReservationInfo(location.getVisitAndReservationInfo())
                .businessAddress(location.getBusinessAddress())
                .businessDetailAddress(location.getBusinessDetailAddress())
                .build();
    }
}
