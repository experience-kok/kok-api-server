package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
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
        private Long id;
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

}