package com.example.auth.dto.campaign.view;

import com.example.auth.domain.Campaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 제품 및 일정 정보 응답")
public class ProductAndScheduleResponse {
    // 제품 정보
    @Schema(description = "제공 제품/서비스에 대한 간략 정보 (10~20글자 내외)", example = "시그니처 음료 2잔 무료 제공")
    private String productShortInfo;
    
    @Schema(description = "제공되는 제품/서비스에 대한 상세 정보", example = "인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.")
    private String productDetails;
    
    // 일정 정보
    @Schema(description = "모집 시작 날짜", example = "2025-05-01")
    private LocalDate recruitmentStartDate;
    
    @Schema(description = "모집 종료 날짜", example = "2025-05-15")
    private LocalDate recruitmentEndDate;
    
    @Schema(description = "참여자 선정 날짜", example = "2025-05-16")
    private LocalDate selectionDate;
    
    @Schema(description = "리뷰 제출 마감일", example = "2025-05-30")
    private LocalDate reviewDeadlineDate;
    
    public static ProductAndScheduleResponse fromEntity(Campaign campaign) {
        return ProductAndScheduleResponse.builder()
                .productShortInfo(campaign.getProductShortInfo())
                .productDetails(campaign.getProductDetails())
                .recruitmentStartDate(campaign.getRecruitmentStartDate())
                .recruitmentEndDate(campaign.getRecruitmentEndDate())
                .selectionDate(campaign.getSelectionDate())
                .reviewDeadlineDate(campaign.getMissionInfo() != null ? campaign.getMissionInfo().getMissionDeadlineDate() : null)
                .build();
    }
}
