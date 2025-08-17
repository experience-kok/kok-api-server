package com.example.auth.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * 유저 미션 이력 엔티티 (포트폴리오용)
 * 완료된 미션들의 이력을 관리하여 다른 클라이언트가 유저의 경험을 확인할 수 있도록 함
 */
@Entity
@Table(name = "user_mission_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMissionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "campaign_title", nullable = false, length = 200)
    private String campaignTitle;

    @Column(name = "campaign_category", nullable = false, length = 100)
    private String campaignCategory;

    @Column(name = "platform_type", nullable = false, length = 50)
    private String platformType;

    @Column(name = "submission_url", nullable = false, columnDefinition = "TEXT")
    private String submissionUrl;

    // JSON 형태로 성과 지표 저장
    @Column(name = "engagement_metrics", columnDefinition = "TEXT")
    private String engagementMetricsJson;

    @Column(name = "completion_date", nullable = false)
    private ZonedDateTime completionDate;

    @Column(name = "client_rating")
    private Integer clientRating; // 1-5점

    @Column(name = "client_review", columnDefinition = "TEXT")
    private String clientReview;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = ZonedDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 공개/비공개 설정 변경
     */
    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 대표 작품 설정 변경
     */
    public void updateFeatured(boolean isFeatured) {
        this.isFeatured = isFeatured;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 클라이언트 평가 업데이트
     */
    public void updateClientReview(Integer rating, String review) {
        this.clientRating = rating;
        this.clientReview = review;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 성과 지표 업데이트 (JSON 형태)
     */
    public void updateEngagementMetrics(String metricsJson) {
        this.engagementMetricsJson = metricsJson;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 캠페인 완료 후 경과 시간(일) 계산
     */
    @Transient
    public long getDaysSinceCompletion() {
        return java.time.temporal.ChronoUnit.DAYS.between(completionDate, ZonedDateTime.now());
    }

    /**
     * 평점이 있는지 확인
     */
    @Transient
    public boolean hasRating() {
        return clientRating != null && clientRating > 0;
    }

    /**
     * 리뷰가 있는지 확인
     */
    @Transient
    public boolean hasReview() {
        return clientReview != null && !clientReview.trim().isEmpty();
    }

    /**
     * 성과 지표가 있는지 확인
     */
    @Transient
    public boolean hasEngagementMetrics() {
        return engagementMetricsJson != null && !engagementMetricsJson.trim().isEmpty();
    }

    /**
     * 포트폴리오 품질 점수 계산 (0-100)
     */
    @Transient
    public int getPortfolioQualityScore() {
        int score = 50; // 기본 점수

        // 평점이 있으면 가산점
        if (hasRating()) {
            score += clientRating * 5; // 최대 25점
        }

        // 리뷰가 있으면 가산점
        if (hasReview()) {
            score += 15;
        }

        // 성과 지표가 있으면 가산점
        if (hasEngagementMetrics()) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    /**
     * 성과 지표 DTO 클래스 (JSON 파싱용)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EngagementMetrics {
        @JsonProperty("views")
        private Long views;      // 조회수

        @JsonProperty("likes")
        private Long likes;      // 좋아요

        @JsonProperty("comments")
        private Long comments;   // 댓글수

        @JsonProperty("shares")
        private Long shares;     // 공유수

        @JsonProperty("engagement_rate")
        private Double engagementRate; // 참여율

        @JsonProperty("reach")
        private Long reach;      // 도달수

        @JsonProperty("impressions")
        private Long impressions; // 노출수

        /**
         * 전체 참여도 계산
         */
        public Long getTotalEngagement() {
            long total = 0;
            if (likes != null) total += likes;
            if (comments != null) total += comments;
            if (shares != null) total += shares;
            return total;
        }

        /**
         * 참여율 계산 (조회수 대비)
         */
        public Double calculateEngagementRate() {
            if (views == null || views == 0) {
                return 0.0;
            }
            long totalEngagement = getTotalEngagement();
            return (double) totalEngagement / views * 100;
        }
    }
}
