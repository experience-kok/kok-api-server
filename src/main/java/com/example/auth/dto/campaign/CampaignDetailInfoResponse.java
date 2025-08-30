package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 캠페인 상세 정보 조회 응답 DTO
 * (제품/서비스 상세정보, 선정기준, 미션 시작일, 리뷰제출 마감일, 참가자 선정일)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 상세 정보 응답")
public class CampaignDetailInfoResponse {

    @Schema(description = "캠페인 ID", example = "32", required = true)
    private Long campaignId;

    @Schema(description = "제공 제품/서비스 간단 정보", example = "아이섀도우 팔레트 + 립 제품 세트", required = true)
    private String productShortInfo;

    @Schema(description = "제공 제품/서비스 상세 정보", example = "18색 아이섀도우 팔레트와 립스틱, 립글로스로 구성된 메이크업 세트를 제공합니다. 다양한 룩을 연출하며 발색과 지속력을 테스트해보세요.", required = true)
    private String productDetails;

    @Schema(description = "선정 기준", example = "메이크업 관련 블로그 운영, 뷰티 포스팅 경험 필수")
    private String selectionCriteria;

    @Schema(description = "참가자 선정일 - 최종 참여자가 발표되는 날짜", example = "2027-12-13")
    private LocalDate selectionDate;

    @JsonProperty("isAlwaysOpen")
    @Schema(description = "상시 캠페인 여부", example = "false", required = true)
    private boolean isAlwaysOpen; // 상시 캠페인 여부


    public static CampaignDetailInfoResponse fromEntity(Campaign campaign) {
        return CampaignDetailInfoResponse.builder()
                .campaignId(campaign.getId())
                .productShortInfo(campaign.getProductShortInfo())
                .productDetails(campaign.getProductDetails())
                .selectionCriteria(campaign.getSelectionCriteria())
                .selectionDate(campaign.getSelectionDate())
                .isAlwaysOpen(campaign.getIsAlwaysOpen())
                .build();
    }
}
