package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 미션 수정 요청 이력 엔티티
 * 클라이언트의 미션 수정 요청과 인플루언서의 수정 내역을 관리
 */
@Entity
@Table(name = "mission_revisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_submission_id", nullable = false)
    private MissionSubmission missionSubmission;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy; // 수정을 요청한 클라이언트

    @Column(name = "revision_reason", nullable = false, columnDefinition = "TEXT")
    private String revisionReason;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private ZonedDateTime requestedAt = ZonedDateTime.now();

    @Column(name = "revised_url", columnDefinition = "TEXT")
    private String revisedUrl;

    @Column(name = "revised_at")
    private ZonedDateTime revisedAt;

    @Column(name = "revision_note", columnDefinition = "TEXT")
    private String revisionNote;

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
        if (this.requestedAt == null) {
            this.requestedAt = ZonedDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 수정 완료 처리
     */
    public void completeRevision(String revisedUrl, String revisionNote) {
        this.revisedUrl = revisedUrl;
        this.revisionNote = revisionNote;
        this.revisedAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 수정 완료 여부 확인
     */
    @Transient
    public boolean isCompleted() {
        return revisedUrl != null && revisedAt != null;
    }

    /**
     * 수정 요청 후 경과 시간(일) 계산
     */
    @Transient
    public long getDaysSinceRequested() {
        return ChronoUnit.DAYS.between(requestedAt, ZonedDateTime.now());
    }

    /**
     * 수정 완료까지 소요 시간(일) 계산
     */
    @Transient
    public Long getDaysToComplete() {
        if (revisedAt == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(requestedAt, revisedAt);
    }
}
