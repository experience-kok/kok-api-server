package com.example.auth.dto;

import com.example.auth.domain.Notice;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "공지사항 목록 응답")
public class NoticeListResponse {

    @Schema(description = "공지사항 ID", example = "1")
    private Long id;

    @Schema(description = "공지사항 제목", example = "중요한 공지사항입니다")
    private String title;

    @Schema(description = "조회수", example = "156")
    private Long viewCount;

    @Schema(description = "필독 여부", example = "true")
    private Boolean isMustRead;

    @Schema(description = "작성자 ID", example = "1")
    private Long authorId;

    @Schema(description = "작성자 이름", example = "관리자")
    private String authorName;

    @Schema(description = "생성일시", example = "2025-08-27T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-08-27T15:45:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Builder
    private NoticeListResponse(Long id, String title, Long viewCount, Boolean isMustRead,
                              Long authorId, String authorName, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.viewCount = viewCount;
        this.isMustRead = isMustRead;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NoticeListResponse from(Notice notice) {
        return NoticeListResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .viewCount(notice.getViewCount())
                .isMustRead(notice.getIsMustRead())
                .authorId(notice.getAuthorId())
                .authorName(notice.getAuthorName())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
