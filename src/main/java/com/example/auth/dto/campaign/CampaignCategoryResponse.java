package com.example.auth.dto.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캠페인 카테고리 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCategoryResponse {
    
    /**
     * 카테고리 타입
     */
    private String type;
    
    /**
     * 카테고리 이름
     */
    private String name;
}
