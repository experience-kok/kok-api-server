package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.VisitLocation;
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
public class CampaignLocationInfoResponse {
    private Long id;
    private CategoryDTO category;
    private List<VisitLocationDTO> visitLocations;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryDTO {
        private Integer id;  // Long에서 Integer로 변경
        private String categoryType;
        private String categoryName;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisitLocationDTO {
        private Long id;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String additionalInfo;
    }
    
    public static CampaignLocationInfoResponse fromEntity(Campaign campaign) {
        List<VisitLocationDTO> visitLocationDTOs = campaign.getVisitLocations().stream()
                .map(location -> VisitLocationDTO.builder()
                        .id(location.getId())
                        .address(location.getAddress())
                        .latitude(location.getLatitude())
                        .longitude(location.getLongitude())
                        .additionalInfo(location.getAdditionalInfo())
                        .build())
                .collect(Collectors.toList());
        
        return CampaignLocationInfoResponse.builder()
                .id(campaign.getId())
                .category(CategoryDTO.builder()
                        .id(campaign.getCategory().getId())  // 이미 Integer이므로 변환 불필요
                        .categoryType(campaign.getCategory().getCategoryType())
                        .categoryName(campaign.getCategory().getCategoryName())
                        .build())
                .visitLocations(visitLocationDTOs)
                .build();
    }
}