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

    @Column(name = "platform_type", nullable = false, length = 50)
    private String platformType;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private ZonedDateTime submittedAt = ZonedDateTime.now();

    @Column(name = "reviewed_at")
    private ZonedDateTime reviewedAt;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "client_feedback", columnDefinition = "TEXT")
    private String clientFeedback;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @OneToMany(mappedBy = "missionSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MissionRevision> revisions = new ArrayList<>();

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
    public void approve(String feedback, Integer rating) {
        this.isCompleted = true;
        this.reviewedAt = ZonedDateTime.now();
        this.clientFeedback = feedback;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 미션을 승인합니다 (평점 없이)
     */
    public void approve(String feedback) {
        approve(feedback, null);
    }

    /**
     * 미션 수정을 요청합니다
     */
    public void requestRevision(String feedback) {
        this.isCompleted = false;
        this.reviewedAt = ZonedDateTime.now();
        this.clientFeedback = feedback;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 수정된 미션을 재제출합니다 (URL만)
     */
    public void resubmit(String newUrl) {
        this.submissionUrl = newUrl;
        this.isCompleted = false;
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
     * 미션 완료 여부 확인
     */
    @Transient
    public boolean isApproved() {
        return this.isCompleted != null && this.isCompleted;
    }

    /**
     * 수정 요청 중인지 확인 (완료되지 않았고 검토 완료된 상태)
     */
    @Transient
    public boolean isRevisionRequested() {
        return !isCompleted() && reviewedAt != null;
    }

    /**
     * 검토 대기 중인지 확인 (아직 검토되지 않은 상태)
     */
    @Transient
    public boolean isPending() {
        return reviewedAt == null && !isCompleted();
    }

    /**
     * 현재 미션 상태를 문자열로 반환
     */
    @Transient
    public String getStatusDescription() {
        if (isCompleted()) {
            return "완료됨";
        } else if (isRevisionRequested()) {
            return "수정 요청";
        } else {
            return "검토 대기";
        }
    }

    /**
     * 미션 완료 여부 확인 헬퍼 메서드
     */
    @Transient
    public boolean isCompleted() {
        return this.isCompleted != null && this.isCompleted;
    }
}
