package com.example.auth.dto.campaign.view;

import com.example.auth.domain.Campaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 위치 정보 응답")
public class LocationInfoResponse {
    @Schema(description = "카테고리 정보")
    private CategoryDTO category;
    
    @Schema(description = "방문 위치 정보 목록")
    private List<VisitLocationDTO> visitLocations;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "카테고리 정보")
    public static class CategoryDTO {
        @Schema(description = "카테고리 ID", example = "1")
        private Integer id;
        
        @Schema(description = "카테고리 유형", example = "카페")
        private String categoryType;
        
        @Schema(description = "카테고리 이름", example = "디저트")
        private String categoryName;
    }

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
    
    public static LocationInfoResponse fromEntity(Campaign campaign) {
        // 카테고리 정보 변환
        CategoryDTO categoryDTO = CategoryDTO.builder()
                .id(campaign.getCategory().getId())
                .categoryType(campaign.getCategory().getCategoryType())
                .categoryName(campaign.getCategory().getCategoryName())
                .build();
                
        // 방문 위치 정보 변환
        List<VisitLocationDTO> visitLocationDTOs = campaign.getVisitLocations().stream()
                .map(location -> VisitLocationDTO.builder()
                        .id(location.getId())
                        .address(location.getAddress())
                        .latitude(location.getLatitude())
                        .longitude(location.getLongitude())
                        .additionalInfo(location.getAdditionalInfo())
                        .build())
                .collect(Collectors.toList());
                
        return LocationInfoResponse.builder()
                .category(categoryDTO)
                .visitLocations(visitLocationDTOs)
                .build();
    }
}
