package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "다중 인플루언서 선정 결과")
public class MultipleSelectionResponse {
    
    @Schema(description = "총 요청된 선정 수", example = "3")
    private int totalRequested;
    
    @Schema(description = "성공한 선정 수", example = "2")
    private int successCount;
    
    @Schema(description = "실패한 선정 수", example = "1")
    private int failCount;
    
    @Schema(description = "성공적으로 선정된 신청 ID 목록", example = "[1, 3]")
    private List<Long> successfulSelections;
    
    @Schema(description = "실패한 선정 목록")
    private List<SelectionFailure> failedSelections;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선정 실패 정보")
    public static class SelectionFailure {
        @Schema(description = "실패한 신청 ID", example = "2")
        private Long applicationId;
        
        @Schema(description = "실패 사유", example = "이미 선정된 신청입니다.")
        private String reason;
    }
}
