package com.example.auth.dto.brandzone;

import com.example.auth.domain.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 브랜드 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandInfoResponse {
    
    /**
     * 브랜드(회사) ID
     */
    private Long brandId;
    
    /**
     * 브랜드명(회사명)
     */
    private String brandName;
    
    /**
     * 담당자명
     */
    private String contactPerson;
    
    /**
     * 연락처
     */
    private String phoneNumber;
    

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
    public static BrandInfoResponse fromCompany(Company company, long totalCampaigns, long activeCampaigns) {
        return BrandInfoResponse.builder()
                .brandId(company.getId())
                .brandName(company.getCompanyName())
                .contactPerson(company.getContactPerson())
                .phoneNumber(company.getPhoneNumber())
                .totalCampaigns(totalCampaigns)
                .activeCampaigns(activeCampaigns)
                .build();
    }
}
