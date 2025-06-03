package com.example.auth.dto.banner;

import com.example.auth.domain.BannerImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 배너 이미지 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "배너 이미지 정보")
public class BannerImageResponse {

    @Schema(description = "배너 ID", example = "1")
    private Long id;

    @Schema(description = "배너 이미지 URL", example = "https://example.com/banner.jpg")
    private String bannerUrl;

    @Schema(description = "클릭 시 이동할 URL", example = "https://example.com/promotion")
    private String redirectUrl;

    /**
     * BannerImage 엔티티로부터 BannerImageResponse를 생성합니다.
     * @param bannerImage 배너 이미지 엔티티
     * @return BannerImageResponse
     */
    public static BannerImageResponse from(BannerImage bannerImage) {
        return BannerImageResponse.builder()
                .id(bannerImage.getId())
                .bannerUrl(bannerImage.getBannerUrl())
                .redirectUrl(bannerImage.getRedirectUrl())
                .build();
    }
}
