package com.example.auth.dto.autocomplete;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "자동완성 제안 응답 DTO")
public class AutoCompleteResponseDTO {

    @Schema(description = "검색 키워드", example = "맛집")
    private String keyword;

    @Schema(description = "자동완성 제안 목록", example = "[\"맛집 체험단 모집\", \"맛집 블로거 선정\"]")
    private List<String> suggestions;

    @Schema(description = "총 제안 개수", example = "5")
    private int totalCount;

    @Schema(description = "요청한 최대 개수", example = "10")
    private int requestedLimit;

    @Schema(description = "응답 생성 시간", example = "2025-06-11T13:49:24.017")
    private LocalDateTime timestamp;

    // 생성자
    public AutoCompleteResponseDTO(String keyword, List<String> suggestions, int requestedLimit) {
        this.keyword = keyword;
        this.suggestions = suggestions;
        this.totalCount = suggestions.size();
        this.requestedLimit = requestedLimit;
        this.timestamp = LocalDateTime.now();
    }
}
