package com.example.auth.dto;

import com.example.auth.domain.KokPost;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "체험콕 글 목록 응답")
public class KokPostListResponse {

    @Schema(description = "글 ID", example = "1")
    private Long id;

    @Schema(description = "글 제목", example = "맛있는 치킨집 체험 후기")
    private String title;

    @Schema(description = "조회수", example = "156")
    private Long viewCount;

    @Schema(description = "캠페인 ID", example = "10")
    private Long campaignId;

    @Schema(description = "작성자 ID", example = "1")
    private Long authorId;

    @Schema(description = "작성자 이름", example = "관리자")
    private String authorName;

    @Schema(description = "연락처", example = "010-1234-5678")
    private String contactPhone;

    @Schema(description = "사업장 주소", example = "서울시 강남구")
    private String businessAddress;

    @Schema(description = "캠페인 모집 중 여부", example = "true")
    private Boolean isCampaignOpen;

    @Schema(description = "생성일시", example = "2025-08-27")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String createdAt;

    @Schema(description = "수정일시", example = "2025-08-27")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String updatedAt;

    @Builder
    private KokPostListResponse(Long id, String title, Long viewCount, Long campaignId,
                               Long authorId, String authorName, String contactPhone,
                               String businessAddress, Boolean isCampaignOpen, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.viewCount = viewCount;
        this.campaignId = campaignId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.contactPhone = contactPhone;
        this.businessAddress = businessAddress;
        this.isCampaignOpen = isCampaignOpen;
        this.createdAt = createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;
        this.updatedAt = updatedAt != null ? updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;
    }

    /**
     * 엔티티에서 DTO로 변환하는 정적 팩토리 메서드
     */
    public static KokPostListResponse fromEntity(KokPost kokPost) {
        return KokPostListResponse.builder()
                .id(kokPost.getId())
                .title(kokPost.getTitle())
                .viewCount(kokPost.getViewCount())
                .campaignId(kokPost.getCampaignId())
                .authorId(kokPost.getAuthorId())
                .authorName(kokPost.getAuthorName())
                .contactPhone(kokPost.getVisitInfo() != null ? kokPost.getVisitInfo().getContactPhone() : null)
                .businessAddress(kokPost.getVisitInfo() != null ? kokPost.getVisitInfo().getBusinessAddress() : null)
                .isCampaignOpen(true) // 기본값 또는 비즈니스 로직에 따라 설정
                .createdAt(kokPost.getCreatedAt())
                .updatedAt(kokPost.getUpdatedAt())
                .build();
    }
}
