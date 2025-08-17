package com.example.auth.dto.brandzone;

import com.example.auth.domain.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브랜드 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandListResponse {
    
    /**
     * 브랜드(회사) ID
     */
    private Long brandId;
    
    /**
     * 브랜드명(회사명)
     */
    private String brandName;
    
    /**
     * 총 캠페인 수
     */
    private long totalCampaigns;
    
    /**
     * 활성 캠페인 수 (모집 중)
     */
    private long activeCampaigns;
    
    /**
     * Company 엔티티로부터 DTO 생성
     */
    public static BrandListResponse fromCompany(Company company, long totalCampaigns, long activeCampaigns) {
        return BrandListResponse.builder()
                .brandId(company.getId())
                .brandName(company.getCompanyName())
                .totalCampaigns(totalCampaigns)
                .activeCampaigns(activeCampaigns)
                .build();
    }
}
