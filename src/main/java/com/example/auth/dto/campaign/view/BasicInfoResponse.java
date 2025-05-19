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
@Schema(description = "캠페인 기본 정보 응답")
public class BasicInfoResponse {
    @Schema(description = "캠페인 고유 식별자", example = "1")
    private Long id;
    
    @Schema(description = "캠페인 진행 플랫폼", example = "인스타그램")
    private String campaignType;
    
    @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집")
    private String title;
    
    @Schema(description = "최대 신청 가능 인원 수", example = "10")
    private Integer maxApplicants;
    
    @Schema(description = "현재 신청 인원 수", example = "3")
    private Integer currentApplicants; // 현재 신청 인원 (별도 쿼리 필요)
    
    @Schema(description = "신청 마감 날짜", example = "2025-05-14")
    private LocalDate applicationDeadlineDate;
    
    public static BasicInfoResponse fromEntity(Campaign campaign) {
        return BasicInfoResponse.builder()
                .id(campaign.getId())
                .campaignType(campaign.getCampaignType())
                .title(campaign.getTitle())
                .maxApplicants(campaign.getMaxApplicants())
                .currentApplicants(0) // 임시로 0 설정 (실제로는 별도 쿼리 필요)
                .applicationDeadlineDate(campaign.getApplicationDeadlineDate())
                .build();
    }
}
