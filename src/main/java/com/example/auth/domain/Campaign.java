package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 캠페인 엔티티
 */
@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "campaign_type", length = 50)
    private String campaignType;

    @Column(name = "product_short_info", length = 500)
    private String productShortInfo;

    @Column(name = "product_details", columnDefinition = "TEXT")
    private String productDetails;

    @Column(name = "recruitment_start_date")
    private LocalDate recruitmentStartDate;

    @Column(name = "recruitment_end_date")
    private LocalDate recruitmentEndDate;

    @Column(name = "selection_date")
    private LocalDate selectionDate;

    @Column(name = "max_applicants")
    private Integer maxApplicants;

    @Column(name = "selection_criteria", columnDefinition = "TEXT")
    private String selectionCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "is_always_open")
    @Builder.Default
    private Boolean isAlwaysOpen = false;

    @Column(name = "review_start_date")
    private LocalDate reviewStartDate;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    // 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CampaignCategory category;

    @OneToOne(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CampaignMissionInfo missionInfo;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CampaignApplication> applications = new ArrayList<>();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 캠페인 승인 상태 enum
     */
    public enum ApprovalStatus {
        PENDING("PENDING"),
        APPROVED("APPROVED"),
        REJECTED("REJECTED");

        private final String value;

        ApprovalStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ApprovalStatus fromValue(String value) {
            for (ApprovalStatus status : ApprovalStatus.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown approval status: " + value);
        }
    }

    // 비즈니스 메서드들

    /**
     * 캠페인이 현재 모집중인지 확인
     */
    public boolean isRecruitmentOpen() {
        if (isAlwaysOpen != null && isAlwaysOpen) {
            return ApprovalStatus.APPROVED.equals(approvalStatus);
        }

        if (recruitmentStartDate == null || recruitmentEndDate == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        boolean isWithinPeriod = !today.isBefore(recruitmentStartDate) && !today.isAfter(recruitmentEndDate);
        boolean isApproved = ApprovalStatus.APPROVED.equals(approvalStatus);

        return isWithinPeriod && isApproved;
    }

    /**
     * 현재 신청자 수 반환
     */
    public int getCurrentApplicantCount() {
        if (applications == null) {
            return 0;
        }
        return (int) applications.stream()
                .filter(app -> app.getApplicationStatus() == com.example.auth.constant.ApplicationStatus.APPLIED ||
                              app.getApplicationStatus() == com.example.auth.constant.ApplicationStatus.SELECTED)
                .count();
    }

    /**
     * 승인 상태 초기화 (수정 시 사용)
     */
    public void resetApprovalStatus() {
        this.approvalStatus = ApprovalStatus.PENDING;
        this.rejectionReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 캠페인 승인
     */
    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.rejectionReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 캠페인 거절
     */
    public void reject(String reason) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 승인 상태를 Enum으로 반환
     */
    public ApprovalStatus getApprovalStatusEnum() {
        return this.approvalStatus;
    }

    /**
     * 상시 캠페인 여부 확인
     */
    public Boolean getIsAlwaysOpen() {
        return isAlwaysOpen;
    }
    
    /**
     * 상시 캠페인 여부 확인 (boolean 반환)
     */
    public boolean isAlwaysOpen() {
        return isAlwaysOpen != null && isAlwaysOpen;
    }

    /**
     * 캠페인 정보 업데이트
     */
    public void updateBasicInfo(String title, String thumbnailUrl, String productShortInfo, String productDetails) {
        if (title != null) this.title = title;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
        if (productShortInfo != null) this.productShortInfo = productShortInfo;
        if (productDetails != null) this.productDetails = productDetails;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 모집 일정 업데이트
     */
    public void updateSchedule(LocalDate recruitmentStartDate, LocalDate recruitmentEndDate, LocalDate selectionDate) {
        if (recruitmentStartDate != null) this.recruitmentStartDate = recruitmentStartDate;
        if (recruitmentEndDate != null) this.recruitmentEndDate = recruitmentEndDate;
        if (selectionDate != null) this.selectionDate = selectionDate;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상시 캠페인 설정
     */
    public void setAlwaysOpen(boolean alwaysOpen) {
        this.isAlwaysOpen = alwaysOpen;
        if (alwaysOpen) {
            this.recruitmentEndDate = null;
            this.selectionDate = null;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 위치 정보가 있는지 확인 (상시 캠페인 검증용)
     */
    public boolean hasLocation() {
        // CampaignLocation과의 관계가 있다면 여기서 확인
        // 현재는 기본적으로 true로 반환
        return true;
    }
}
