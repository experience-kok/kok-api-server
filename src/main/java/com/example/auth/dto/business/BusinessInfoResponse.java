package com.example.auth.dto.business;

import com.example.auth.domain.Company;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사업자 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사업자 정보 응답")
public class BusinessInfoResponse {

    @Schema(description = "업체명", example = "맛있는 카페")
    private String companyName;

    @Schema(description = "사업자등록번호", example = "123-45-67890")
    private String businessRegistrationNumber;

    @Schema(description = "약관 동의 여부", example = "true")
    private Boolean termsAgreed;

    @Schema(description = "약관 동의 일시", example = "2025-01-27T10:30:00+09:00")
    private String termsAgreedAt;

    @Schema(description = "사업자 정보 등록 여부", example = "true")
    private Boolean hasBusinessInfo;

    /**
     * Company 엔티티에서 BusinessInfoResponse로 변환
     */
    public static BusinessInfoResponse fromCompany(Company company) {
        if (company == null) {
            return BusinessInfoResponse.builder()
                    .companyName(null)
                    .businessRegistrationNumber(null)
                    .termsAgreed(null)
                    .termsAgreedAt(null)
                    .hasBusinessInfo(false)
                    .build();
        }
        
        return BusinessInfoResponse.builder()
                .companyName(company.getCompanyName())
                .businessRegistrationNumber(company.getBusinessRegistrationNumber())
                .termsAgreed(company.getTermsAgreed())
                .termsAgreedAt(company.getTermsAgreedAt() != null ? company.getTermsAgreedAt().toString() : null)
                .hasBusinessInfo(true)
                .build();
    }
}
