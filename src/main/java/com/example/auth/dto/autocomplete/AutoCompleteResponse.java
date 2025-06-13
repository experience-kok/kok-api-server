package com.example.auth.dto.autocomplete;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 자동완성 응답 래퍼 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "자동완성 응답")
public class AutoCompleteResponse {
    
    @Schema(description = "검색 제안 목록", example = "[\"맛집 체험단 모집\", \"맛집 블로거 선정 이벤트\"]")
    private List<String> suggestions;
    
    /**
     * 정적 팩토리 메소드
     * @param suggestions 제안 목록
     * @return AutoCompleteResponse 인스턴스
     */
    public static AutoCompleteResponse of(List<String> suggestions) {
        return AutoCompleteResponse.builder()
                .suggestions(suggestions)
                .build();
    }
}
