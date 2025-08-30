package com.example.auth.domain;

import com.example.auth.constant.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * 캠페인 신청 엔티티
 * 캠페인 신청자 수 추적을 위한 테이블
 */
@Entity
@Table(name = "campaign_applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "application_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplicationStatus applicationStatus = ApplicationStatus.APPLIED;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 신청 상태를 변경합니다.
     */
    public void updateStatus(ApplicationStatus status) {
        this.applicationStatus = status;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 신청을 취소합니다.
     * 거절/취소된 신청은 데이터를 삭제하는 방식으로 처리
     */
    @Deprecated
    public void cancel() {
        throw new UnsupportedOperationException("거절/취소된 신청은 데이터 삭제로 처리됩니다.");
    }

    /**
     * 신청자를 선정합니다.
     */
    public void select() {
        this.applicationStatus = ApplicationStatus.SELECTED;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 신청을 거절합니다.
     */
    public void reject() {
        this.applicationStatus = ApplicationStatus.REJECTED;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 거절 상태를 해제합니다.
     */
    public void unreject() {
        this.applicationStatus = ApplicationStatus.PENDING;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 거절된 신청인지 확인합니다.
     */
    public boolean isRejected() {
        return this.applicationStatus == ApplicationStatus.REJECTED;
    }

    /**
     * 하위 호환성을 위한 getStatus 메소드
     */
    public ApplicationStatus getStatus() {
        return this.applicationStatus;
    }

    /**
     * 하위 호환성을 위한 setStatus 메소드
     */
    public void setStatus(ApplicationStatus status) {
        this.applicationStatus = status;
    }
}
