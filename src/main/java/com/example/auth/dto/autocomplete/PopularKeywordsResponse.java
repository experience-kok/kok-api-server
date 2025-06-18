package com.example.auth.dto.autocomplete;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 인기 키워드 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인기 키워드 응답")
public class PopularKeywordsResponse {

    @Schema(description = "인기 키워드 목록", example = "[\"맛집\", \"카페\", \"뷰티\", \"체험단\"]")
    private List<String> keywords;

    @Schema(description = "키워드 개수", example = "4")
    private int count;

    @Schema(description = "갱신 시간 정보", example = "30분마다 자동 갱신")
    private String updateInfo;

    /**
     * 키워드 리스트를 인기 키워드 응답으로 변환
     */
    public static PopularKeywordsResponse from(List<String> keywords) {
        return PopularKeywordsResponse.builder()
                .keywords(keywords)
                .count(keywords.size())
                .updateInfo("30분마다 자동 갱신")
                .build();
    }
}
