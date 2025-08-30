package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "공지사항 페이지 응답")
public class NoticePageResponse {

    @Schema(description = "공지사항 목록")
    private List<NoticeListResponse> notices;

    @Schema(description = "페이지네이션 정보")
    private PaginationInfo pagination;

    @Builder
    private NoticePageResponse(List<NoticeListResponse> notices, PaginationInfo pagination) {
        this.notices = notices;
        this.pagination = pagination;
    }

    @Getter
    @Builder
    @Schema(description = "페이지네이션 정보")
    public static class PaginationInfo {
        @Schema(description = "현재 페이지 번호 (1부터 시작)", example = "1")
        private int pageNumber;

        @Schema(description = "페이지당 크기", example = "10")
        private int pageSize;

        @Schema(description = "전체 페이지 수", example = "5")
        private int totalPages;

        @Schema(description = "전체 요소 수", example = "48")
        private long totalElements;

        @Schema(description = "첫 번째 페이지 여부", example = "true")
        private boolean first;

        @Schema(description = "마지막 페이지 여부", example = "false")
        private boolean last;
    }

    public static NoticePageResponse from(Page<NoticeListResponse> page) {
        return NoticePageResponse.builder()
                .notices(page.getContent())
                .pagination(PaginationInfo.builder()
                        .pageNumber(page.getNumber() + 1)  // 1부터 시작하도록 +1
                        .pageSize(page.getSize())
                        .totalPages(page.getTotalPages())
                        .totalElements(page.getTotalElements())
                        .first(page.isFirst())
                        .last(page.isLast())
                        .build())
                .build();
    }
}
