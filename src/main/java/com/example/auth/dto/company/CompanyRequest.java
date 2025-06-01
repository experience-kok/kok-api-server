package com.example.auth.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 업체 정보 등록/수정 요청 DTO (내부 사용용)
 * 캠페인 생성 시에만 사용됨
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRequest {

    @NotBlank(message = "업체명은 필수입니다.")
    @Size(max = 100, message = "업체명은 100자를 초과할 수 없습니다.")
    private String companyName;

    @Size(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
    private String businessRegistrationNumber;

    @Size(max = 50, message = "담당자명은 50자를 초과할 수 없습니다.")
    private String contactPerson;

    @Size(max = 20, message = "연락처는 20자를 초과할 수 없습니다.")
    private String phoneNumber;

    /**
     * CreateCampaignRequest.CompanyInfo에서 CompanyRequest로 변환
     */
    public static CompanyRequest fromCompanyInfo(com.example.auth.dto.campaign.CreateCampaignRequest.CompanyInfo companyInfo) {
        if (companyInfo == null) {
            return null;
        }
        
        return CompanyRequest.builder()
                .companyName(companyInfo.getCompanyName())
                .businessRegistrationNumber(companyInfo.getBusinessRegistrationNumber())
                .contactPerson(companyInfo.getContactPerson())
                .phoneNumber(companyInfo.getPhoneNumber())
                .build();
    }
}
