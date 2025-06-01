package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import com.example.auth.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCompanyInfoResponse {
    private Long id;
    private String companyInfo;
    private UserDTO creator;
    
    public static CampaignCompanyInfoResponse fromEntity(Campaign campaign) {
        return CampaignCompanyInfoResponse.builder()
                .id(campaign.getId())
                .companyInfo(campaign.getCompany() != null ? 
                    campaign.getCompany().getCompanyName() : null)
                .creator(UserDTO.fromEntity(campaign.getCreator()))
                .build();
    }
}
