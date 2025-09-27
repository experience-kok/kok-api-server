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
@Schema(description = "체험콕 글 상세 응답")
public class KokPostDetailResponse {

    @Schema(description = "글 ID", example = "1")
    private Long id;

    @Schema(description = "글 제목", example = "맛있는 치킨집 체험 후기")
    private String title;

    @Schema(description = "글 내용", example = "정말 맛있는 치킨집이었습니다. 양념이 특히 좋았고...")
    private String content;

    @Schema(description = "조회수", example = "156")
    private Long viewCount;

    @Schema(description = "캠페인 ID", example = "10")
    private Long campaignId;

    @Schema(description = "캠페인 모집 중 여부", example = "true")
    private Boolean isCampaignOpen;

    @Schema(description = "생성일시", example = "2025-01-27")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String createdAt;

    @Schema(description = "수정일시", example = "2025-01-27")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String updatedAt;

    @Schema(description = "작성자 이름", example = "관리자")
    private String authorName;

    @Schema(description = "방문 정보")
    private VisitInfo visitInfo;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Schema(description = "방문 정보")
    public static class VisitInfo {
        @Schema(description = "연락처", example = "010-1234-5678")
        private String contactPhone;

        @Schema(description = "홈페이지 주소", example = "https://example.com")
        private String homepage;

        @Schema(description = "사업장 주소", example = "서울시 강남구")
        private String businessAddress;

        @Schema(description = "사업장 상세 주소", example = "테헤란로 123, 2층")
        private String businessDetailAddress;

        @Schema(description = "위도", example = "37.5665")
        private Double lat;

        @Schema(description = "경도", example = "126.9780")
        private Double lng;

        @Builder
        private VisitInfo(String contactPhone, String homepage, String businessAddress,
                          String businessDetailAddress, Double lat, Double lng) {
            this.contactPhone = contactPhone;
            this.homepage = homepage;
            this.businessAddress = businessAddress;
            this.businessDetailAddress = businessDetailAddress;
            this.lat = lat;
            this.lng = lng;
        }
    }

    @Builder
    private KokPostDetailResponse(Long id, String title, String content, Long viewCount, Long campaignId,
                                  Boolean isCampaignOpen, LocalDateTime createdAt, LocalDateTime updatedAt,
                                  String authorName, VisitInfo visitInfo) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.campaignId = campaignId;
        this.isCampaignOpen = isCampaignOpen;
        this.createdAt = createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;
        this.updatedAt = updatedAt != null ? updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;
        this.authorName = authorName;
        this.visitInfo = visitInfo;
    }

    /**
     * 엔티티에서 DTO로 변환하는 정적 팩토리 메서드
     */
    public static KokPostDetailResponse fromEntity(KokPost kokPost, Boolean isCampaignOpen) {
        VisitInfo visitInfo = null;
        if (kokPost.getVisitInfo() != null) {
            visitInfo = VisitInfo.builder()
                    .contactPhone(kokPost.getVisitInfo().getContactPhone())
                    .homepage(kokPost.getVisitInfo().getHomepage())
                    .businessAddress(kokPost.getVisitInfo().getBusinessAddress())
                    .businessDetailAddress(kokPost.getVisitInfo().getBusinessDetailAddress())
                    .lat(kokPost.getVisitInfo().getLat())
                    .lng(kokPost.getVisitInfo().getLng())
                    .build();
        }

        return KokPostDetailResponse.builder()
                .id(kokPost.getId())
                .title(kokPost.getTitle())
                .content(kokPost.getContent())
                .viewCount(kokPost.getViewCount())
                .campaignId(kokPost.getCampaignId())
                .isCampaignOpen(isCampaignOpen)
                .createdAt(kokPost.getCreatedAt())
                .updatedAt(kokPost.getUpdatedAt())
                .authorName(kokPost.getAuthorName())
                .visitInfo(visitInfo)
                .build();
    }

    /**
     * 엔티티에서 DTO로 변환하는 정적 팩토리 메서드 (총 조회수 포함)
     */
    public static KokPostDetailResponse fromEntityWithViewCount(KokPost kokPost, Boolean isCampaignOpen, Long totalViewCount) {
        VisitInfo visitInfo = null;
        if (kokPost.getVisitInfo() != null) {
            visitInfo = VisitInfo.builder()
                    .contactPhone(kokPost.getVisitInfo().getContactPhone())
                    .homepage(kokPost.getVisitInfo().getHomepage())
                    .businessAddress(kokPost.getVisitInfo().getBusinessAddress())
                    .businessDetailAddress(kokPost.getVisitInfo().getBusinessDetailAddress())
                    .lat(kokPost.getVisitInfo().getLat())
                    .lng(kokPost.getVisitInfo().getLng())
                    .build();
        }

        return KokPostDetailResponse.builder()
                .id(kokPost.getId())
                .title(kokPost.getTitle())
                .content(kokPost.getContent())
                .viewCount(totalViewCount) // DB + Redis 합계
                .campaignId(kokPost.getCampaignId())
                .isCampaignOpen(isCampaignOpen)
                .createdAt(kokPost.getCreatedAt())
                .updatedAt(kokPost.getUpdatedAt())
                .authorName(kokPost.getAuthorName())
                .visitInfo(visitInfo)
                .build();
    }
}
