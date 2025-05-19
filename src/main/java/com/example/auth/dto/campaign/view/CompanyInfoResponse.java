package com.example.auth.dto.campaign.view;

import com.example.auth.domain.Campaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 업체 정보 응답 DTO
 * 
 * 업체/브랜드 정보와 캠페인 등록 사용자 닉네임을 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 업체 정보 응답")
public class CompanyInfoResponse {
    @Schema(description = "업체/브랜드 정보", example = "2020년에 오픈한 강남 소재의 프리미엄 디저트 카페로, 유기농 재료만을 사용한 건강한 음료를 제공합니다.")
    private String companyInfo;
    
    @Schema(description = "캠페인 등록 사용자 닉네임", example = "브랜드매니저")
    private String userNickname;
    
    /**
     * 캠페인 엔티티로부터 업체 정보와 사용자 닉네임을 추출하여 응답 객체를 생성합니다.
     * 
     * @param campaign 캠페인 엔티티
     * @return 업체 정보와 사용자 닉네임이 포함된 응답 객체
     */
    public static CompanyInfoResponse fromEntity(Campaign campaign) {
        return CompanyInfoResponse.builder()
                .companyInfo(campaign.getCompanyInfo())
                .userNickname(campaign.getCreator() != null ? campaign.getCreator().getNickname() : null)
                .build();
    }
}
