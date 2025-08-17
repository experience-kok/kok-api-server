package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "다중 인플루언서 선정 요청")
public class MultipleSelectionRequest {
    
    @NotEmpty(message = "선정할 신청 ID 목록은 비어있을 수 없습니다.")
    @Schema(description = "선정할 신청 ID 목록", example = "[1, 2, 3]")
    private List<Long> applicationIds;
}
