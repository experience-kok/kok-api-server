package com.example.auth.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 캠페인 신청자 선정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    title = "캠페인 신청자 선정 요청",
    description = "캠페인 신청자를 선정하기 위한 요청 정보"
)
public class CampaignSelectionRequest {
    
    /**
     * 선정할 신청 ID 목록
     */
    @NotNull(message = "선정할 신청 목록은 필수입니다")
    @NotEmpty(message = "최소 1명 이상의 신청자를 선정해야 합니다")
    @Schema(
        description = "선정할 신청 ID 목록 (신청자 목록 조회 API에서 applicationId 값들을 사용)",
        example = "[101, 102, 103]",
        required = true
    )
    private List<Long> selectedApplicationIds;
    
    /**
     * 선정 사유 (선택사항)
     */
    @Schema(
        description = "선정 사유 (선택사항, 내부 기록용)",
        example = "팔로워 수와 콘텐츠 품질을 고려한 선정"
    )
    private String selectionReason;
    
    /**
     * 미선정자에게 알림 전송 여부 (기본값: true)
     */
    @Builder.Default
    @Schema(
        description = "미선정자에게 알림 전송 여부",
        example = "true",
        defaultValue = "true"
    )
    private Boolean notifyUnselected = true;
    
    /**
     * 선정자에게 전송할 추가 메시지 (선택사항)
     */
    @Schema(
        description = "선정자에게 전송할 추가 메시지 (선택사항)",
        example = "축하드립니다! 체험 제품은 3일 내 발송 예정입니다."
    )
    private String messageToSelected;
    
    /**
     * 미선정자에게 전송할 추가 메시지 (선택사항)
     */
    @Schema(
        description = "미선정자에게 전송할 추가 메시지 (선택사항)",
        example = "이번에는 아쉽게 선정되지 않았지만, 다음 기회에 꼭 함께해요!"
    )
    private String messageToUnselected;
}
