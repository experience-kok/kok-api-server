package com.example.auth.dto.location;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캠페인 위치 정보 요청 DTO (간소화)
 * 위치명은 campaigns 테이블에서 관리
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "캠페인 위치 정보 요청",
    description = "캠페인의 위치 정보 등록/수정 요청"
)
public class CampaignLocationRequest {

    @Schema(
        description = "위도 (선택사항, 지도 표시용)",
        example = "37.5665"
    )
    private Double latitude;

    @Schema(
        description = "경도 (선택사항, 지도 표시용)",
        example = "126.9780"
    )
    private Double longitude;

    @Schema(
        description = "공식 홈페이지 주소 (선택사항)",
        example = "https://delicious-cafe.com"
    )
    private String homepage;

    @Schema(
        description = "일반 유저에게 공개되는 연락처 (선택사항)",
        example = "02-123-4567"
    )
    private String contactPhone;

    @Schema(
        description = "방문 및 예약 안내 (선택사항)",
        example = "평일 10시-22시 방문 가능, 사전 예약 필수"
    )
    private String visitAndReservationInfo;

    /**
     * 좌표 유효성 검증
     */
    public boolean hasValidCoordinates() {
        if (latitude == null && longitude == null) {
            return true; // 둘 다 null이면 OK
        }
        if (latitude == null || longitude == null) {
            return false; // 하나만 null이면 안됨
        }
        // 위도/경도 범위 체크
        return latitude >= -90 && latitude <= 90 && 
               longitude >= -180 && longitude <= 180;
    }
}
