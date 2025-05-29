package com.example.auth.dto.campaign;

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
public class CampaignBasicInfoResponse {
    @Schema(description = "캠페인 ID", example = "1")
    private Long id;
    
    @Schema(description = "캠페인 썸네일 이미지 URL", example = "https://example.com/images/campaign.jpg")
    private String thumbnailUrl;
    
    @Schema(description = "캠페인 진행 플랫폼", example = "인스타그램")
    private String campaignType;
    
    @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집")
    private String title;
    
    @Schema(description = "최대 신청 가능 인원 수", example = "10")
    private Integer maxApplicants;
    
    @Schema(description = "현재 신청 인원", example = "5")
    private Integer currentApplicants;  // 임시로 0으로 설정
    
    @Schema(description = "신청 마감 날짜", example = "2025-05-14")
    private LocalDate applicationDeadlineDate;
    
    @Schema(description = "카테고리 정보")
    private CategoryInfo category;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카테고리 정보")
    public static class CategoryInfo {
        @Schema(description = "카테고리 타입", example = "방문", required = true)
        private String type;
        
        @Schema(description = "카테고리 이름", example = "카페", required = true)
        private String name;
    }
    
    public static CampaignBasicInfoResponse fromEntity(Campaign campaign) {
        CampaignBasicInfoResponse.CampaignBasicInfoResponseBuilder builder = CampaignBasicInfoResponse.builder()
                .id(campaign.getId())
                .thumbnailUrl(campaign.getThumbnailUrl())
                .campaignType(campaign.getCampaignType())
                .title(campaign.getTitle())
                .maxApplicants(campaign.getMaxApplicants())
                .currentApplicants(0)  // 임시 값
                .applicationDeadlineDate(campaign.getApplicationDeadlineDate());
        
        // 카테고리 정보가 있으면 추가
        if (campaign.getCategory() != null) {
            CategoryInfo categoryInfo = CategoryInfo.builder()
                    .type(campaign.getCategory().getCategoryType())
                    .name(campaign.getCategory().getCategoryName())
                    .build();
            builder.category(categoryInfo);
        }
        
        return builder.build();
    }
}
