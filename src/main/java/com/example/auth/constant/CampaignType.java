package com.example.auth.constant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 캠페인 타입 열거형 (OAS 표준 준수)
 * 
 * 기존 CampaignCategory.CategoryType은 한글 enum 값을 사용하여 
 * OAS 표준을 위반하므로, 새로운 API에서는 이 영문 enum을 사용합니다.
 * 
 * 기존 시스템과의 호환성을 위해 변환 메서드를 제공합니다.
 */
@Schema(enumAsRef = true, description = "캠페인 타입 (OAS 표준 준수)")
public enum CampaignType {
    
    @Schema(description = "방문형 캠페인 - 인플루언서가 직접 매장/장소를 방문하여 체험")
    VISIT("방문", "Visit"),
    
    @Schema(description = "배송형 캠페인 - 제품을 배송받아 체험 후 리뷰")
    DELIVERY("배송", "Delivery");
    
    private final String koreanName;
    private final String description;
    
    CampaignType(String koreanName, String description) {
        this.koreanName = koreanName;
        this.description = description;
    }
    
    /**
     * 한글 이름 반환 (UI 표시용)
     */
    public String getKoreanName() {
        return koreanName;
    }
    
    /**
     * 영문 설명 반환
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 한글 이름으로부터 CampaignType 찾기
     * 
     * @param koreanName 한글 이름 ("방문" 또는 "배송")
     * @return 해당하는 CampaignType
     * @throws IllegalArgumentException 유효하지 않은 한글 이름인 경우
     */
    public static CampaignType fromKoreanName(String koreanName) {
        for (CampaignType type : CampaignType.values()) {
            if (type.koreanName.equals(koreanName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown campaign type: " + koreanName + 
            ". Valid values: 방문, 배송");
    }
    
    /**
     * 기존 CampaignCategory.CategoryType으로부터 변환
     * 
     * @param legacyType 기존 카테고리 타입
     * @return 새로운 CampaignType
     */
    public static CampaignType fromLegacyType(com.example.auth.domain.CampaignCategory.CategoryType legacyType) {
        return fromKoreanName(legacyType.getDisplayName());
    }
    
    /**
     * 기존 CampaignCategory.CategoryType으로 변환
     * 
     * @return 기존 카테고리 타입
     */
    public com.example.auth.domain.CampaignCategory.CategoryType toLegacyType() {
        return com.example.auth.domain.CampaignCategory.CategoryType.fromDisplayName(this.koreanName);
    }
    
    /**
     * JSON 직렬화 시 영문 enum 이름 사용
     */
    @Override
    public String toString() {
        return this.name();
    }
}