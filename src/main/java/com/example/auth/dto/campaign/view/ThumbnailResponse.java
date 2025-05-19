package com.example.auth.dto.campaign.view;

import com.example.auth.domain.Campaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 썸네일 응답")
public class ThumbnailResponse {
    @Schema(description = "캠페인 썸네일 이미지 URL", example = "https://example.com/images/campaign.jpg")
    private String thumbnailUrl;
    
    public static ThumbnailResponse fromEntity(Campaign campaign) {
        return ThumbnailResponse.builder()
                .thumbnailUrl(campaign.getThumbnailUrl())
                .build();
    }
}
