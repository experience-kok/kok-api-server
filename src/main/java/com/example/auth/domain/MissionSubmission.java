package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 미션 제출 엔티티
 * 인플루언서가 제출한 미션 링크와 관련 정보를 관리
 */
@Entity
@Table(name = "mission_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_application_id", nullable = false)
    private CampaignApplication campaignApplication;

    @Column(name = "submission_url", nullable = false, columnDefinition = "TEXT")
    private String submissionUrl;

    @Column(name = "submission_title", length = 200)
    private String submissionTitle;

    @Column(name = "submission_description", columnDefinition = "TEXT")
    private String submissionDescription;

    @Column(name = "platform_type", nullable = false, length = 50)
    private String platformType;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private ZonedDateTime submittedAt = ZonedDateTime.now();

    @Column(name = "reviewed_at")
    private ZonedDateTime reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "client_feedback", columnDefinition = "TEXT")
    private String clientFeedback;

    @Column(name = "revision_count", nullable = false)
    @Builder.Default
    private Integer revisionCount = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @OneToMany(mappedBy = "missionSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MissionRevision> revisions = new ArrayList<>();

    /**
     * 미션 검토 상태 열거형
     */
    public enum ReviewStatus {
        PENDING("검토 대기"),
        APPROVED("승인됨"),
        REVISION_REQUESTED("수정 요청");

        private final String description;

        ReviewStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = ZonedDateTime.now();
        }
        if (this.submittedAt == null) {
            this.submittedAt = ZonedDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 미션을 승인합니다
     */
    public void approve(String feedback) {
        this.reviewStatus = ReviewStatus.APPROVED;
        this.reviewedAt = ZonedDateTime.now();
        this.clientFeedback = feedback;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 미션 수정을 요청합니다
     */
    public void requestRevision(String feedback) {
        this.reviewStatus = ReviewStatus.REVISION_REQUESTED;
        this.reviewedAt = ZonedDateTime.now();
        this.clientFeedback = feedback;
        this.revisionCount++;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 수정된 미션을 재제출합니다
     */
    public void resubmit(String newUrl, String newTitle, String newDescription) {
        this.submissionUrl = newUrl;
        this.submissionTitle = newTitle;
        this.submissionDescription = newDescription;
        this.reviewStatus = ReviewStatus.PENDING;
        this.submittedAt = ZonedDateTime.now();
        this.reviewedAt = null;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 수정 요청 이력을 추가합니다
     */
    public void addRevision(MissionRevision revision) {
        if (revision == null) {
            return;
        }
        revisions.add(revision);
        revision.setMissionSubmission(this);
    }

    /**
     * 수정 요청 이력을 제거합니다
     */
    public void removeRevision(MissionRevision revision) {
        if (revision == null) {
            return;
        }
        revisions.remove(revision);
        revision.setMissionSubmission(null);
    }

    /**
     * 최종 승인 여부 확인
     */
    @Transient
    public boolean isApproved() {
        return reviewStatus == ReviewStatus.APPROVED;
    }

    /**
     * 수정 요청 중인지 확인
     */
    @Transient
    public boolean isRevisionRequested() {
        return reviewStatus == ReviewStatus.REVISION_REQUESTED;
    }

    /**
     * 검토 대기 중인지 확인
     */
    @Transient
    public boolean isPending() {
        return reviewStatus == ReviewStatus.PENDING;
    }
}
