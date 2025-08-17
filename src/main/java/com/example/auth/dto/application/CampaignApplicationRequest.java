package com.example.auth.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 신청 요청 DTO
 * 인플루언서가 캠페인 참여를 신청할 때 사용하는 요청 객체입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 신청 요청 - 인플루언서가 특정 캠페인에 참여를 신청할 때 사용하는 데이터", example = """
    {
      "campaignId": 42
    }
    """)
public class CampaignApplicationRequest {

    @NotNull(message = "캠페인 ID는 필수입니다.")
    @Schema(description = "신청할 캠페인의 고유 식별자 - 캠페인 목록이나 상세 페이지에서 확인 가능한 캠페인 ID", example = "42", required = true)
    private Long campaignId;
    
    // 향후 캠페인 지원 시 추가 필드가 필요할 경우 아래에 추가
    // 예: 지원 동기, 이전 경험, SNS 링크 등
    
    /*
    @Schema(description = "인플루언서 자기소개 (선택사항)",
            example = "뷰티 제품 리뷰 경험이 풍부하며, 화장품 관련 콘텐츠를 주로 제작합니다.")
    private String introduction;
    
    @Schema(description = "이전 관련 경험 (선택사항)",
            example = "화장품 브랜드 A의 체험단 활동을 통해 립스틱 리뷰 콘텐츠를 제작한 경험이 있습니다.")
    private String previousExperience;
    */
}
