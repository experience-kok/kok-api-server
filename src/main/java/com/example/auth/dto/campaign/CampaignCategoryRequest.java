package com.example.auth.dto.campaign;

import com.example.auth.constant.CampaignType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 카테고리 요청 DTO (OAS 표준 준수 버전)
 * 
 * 기존 CreateCampaignRequest.CategoryInfo는 한글 enum을 사용하므로
 * 새로운 API에서는 이 클래스를 사용하여 OAS 표준을 준수합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "캠페인 카테고리 정보 (OAS 표준 준수)")
public class CampaignCategoryRequest {
    
    @NotNull(message = "캠페인 타입은 필수입니다.")
    @Schema(description = "캠페인 타입 - 영문 enum 값 사용으로 OAS 표준 준수",
            example = "VISIT",
            allowableValues = {"VISIT", "DELIVERY"},
            required = true)
    private CampaignType type;
    
    @NotBlank(message = "카테고리명은 필수입니다.")
    @Schema(description = "카테고리명 - 제품/서비스 분야를 나타내는 세부 분류\n" +
                          "• VISIT: 맛집, 카페, 뷰티, 숙박\n" +
                          "• DELIVERY: 식품, 화장품, 생활용품, 패션, 잡화",
            example = "카페",
            required = true)
    private String name;
    
    /**
     * 기존 CategoryInfo로 변환 (하위 호환성)
     * 
     * @return 기존 시스템과 호환되는 CategoryInfo
     */
    public CreateCampaignRequest.CategoryInfo toLegacyCategoryInfo() {
        return CreateCampaignRequest.CategoryInfo.builder()
                .type(this.type.getKoreanName())
                .name(this.name)
                .build();
    }
    
    /**
     * 기존 CategoryInfo로부터 생성 (마이그레이션 용도)
     * 
     * @param legacyInfo 기존 CategoryInfo
     * @return 새로운 CampaignCategoryRequest
     */
    public static CampaignCategoryRequest fromLegacyCategoryInfo(CreateCampaignRequest.CategoryInfo legacyInfo) {
        return CampaignCategoryRequest.builder()
                .type(CampaignType.fromKoreanName(legacyInfo.getType()))
                .name(legacyInfo.getName())
                .build();
    }
}
