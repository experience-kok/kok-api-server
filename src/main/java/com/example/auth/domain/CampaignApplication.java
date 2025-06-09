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
    private ApplicationStatus applicationStatus = ApplicationStatus.PENDING;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
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
     * 신청 상태를 변경합니다.
     */
    public void updateStatus(ApplicationStatus status) {
        this.applicationStatus = status;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 신청을 취소합니다.
     */
    public void cancel() {
        this.applicationStatus = ApplicationStatus.REJECTED;
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 신청자를 선정합니다.
     */
    public void select() {
        this.applicationStatus = ApplicationStatus.APPROVED;
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
