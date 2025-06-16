package com.example.auth.dto.autocomplete;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 캐시 상태 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "자동완성 캐시 상태 정보")
public class CacheStatusResponse {

    @Schema(description = "캐시된 캠페인 제목 개수", example = "1250")
    private long titlesCount;

    @Schema(description = "캐시된 인기 키워드 개수", example = "89")
    private long keywordsCount;

    @Schema(description = "캐시 상태", example = "HEALTHY")
    private CacheHealth status;

    @Schema(description = "상태 확인 시간")
    private LocalDateTime checkedAt;

    @Schema(description = "다음 갱신 예정 시간 정보")
    private String nextRefreshInfo;

    /**
     * 캐시 상태 enum
     */
    public enum CacheHealth {
        HEALTHY,    // 정상
        WARNING,    // 경고 (데이터 부족)
        ERROR       // 오류 (캐시 없음)
    }

    /**
     * 캐시 상태를 분석하여 응답 생성
     */
    public static CacheStatusResponse from(long titlesCount, long keywordsCount) {
        CacheHealth status;
        
        if (titlesCount == 0 && keywordsCount == 0) {
            status = CacheHealth.ERROR;
        } else if (titlesCount < 10 || keywordsCount < 5) {
            status = CacheHealth.WARNING;
        } else {
            status = CacheHealth.HEALTHY;
        }

        return CacheStatusResponse.builder()
                .titlesCount(titlesCount)
                .keywordsCount(keywordsCount)
                .status(status)
                .checkedAt(LocalDateTime.now())
                .nextRefreshInfo("제목: 10분마다, 키워드: 30분마다 자동 갱신")
                .build();
    }
}
