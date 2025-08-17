package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 캠페인 정보를 저장하는 엔티티 클래스
 * 
 * 캠페인은 인플루언서 마케팅의 핵심 단위로, 브랜드/업체가 제공하는 제품이나 서비스를
 * 인플루언서들이 체험하고 리뷰를 작성하는 활동의 정보를 담고 있습니다.
 */
@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 캠페인 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;  // 캠페인 등록자 (필수)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;  // 업체 정보 (선택사항으로 변경)

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;  // 캠페인 썸네일 이미지 URL

    @Column(name = "campaign_type", nullable = false, length = 50)
    private String campaignType;  // 캠페인 진행 플랫폼 (인스타그램, 블로그 등)

    @Column(name = "title", length = 200, nullable = false)
    private String title;  // 캠페인 제목

    @Column(name = "product_short_info", length = 50, nullable = false)
    private String productShortInfo;  // 제공 제품/서비스에 대한 간략 정보

    @Column(name = "max_applicants", nullable = false)
    private Integer maxApplicants;  // 최대 신청 가능 인원 수

    @Column(name = "product_details", nullable = false, columnDefinition = "TEXT")
    private String productDetails;  // 제공되는 제품/서비스에 대한 상세 정보

    // 날짜 필드들 - 논리적 순서에 따라 정렬
    @Column(name = "recruitment_start_date", nullable = false)
    private LocalDate recruitmentStartDate;  // 모집 시작 날짜

    @Column(name = "recruitment_end_date", nullable = false)
    private LocalDate recruitmentEndDate;  // 모집 종료 날짜

    @Column(name = "selection_date", nullable = false)
    private LocalDate selectionDate;  // 참여자 선정 날짜

    @Column(name = "selection_criteria", columnDefinition = "TEXT")
    private String selectionCriteria;  // 선정 기준

    @Column(name = "is_always_open", nullable = false)
    @Builder.Default
    private Boolean isAlwaysOpen = false;  // 상시 등록 여부 (상시 캠페인은 방문형만 가능)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CampaignCategory category;  // 캠페인 카테고리 (필수)
    


    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CampaignApplication> applications = new ArrayList<>();  // 캠페인 신청 목록

    @OneToOne(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private CampaignLocation location;  // 캠페인 위치 정보 (1:1 관계)

    @OneToOne(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private CampaignMissionInfo missionInfo;  // 캠페인 미션 정보 (1:1 관계)

    // 관리자 승인 관련 필드들
    @Column(name = "approval_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;  // 승인 상태

    @Column(name = "approval_comment", columnDefinition = "TEXT")
    private String approvalComment;  // 승인/거절 관련 관리자 코멘트

    @Column(name = "approval_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime approvalDate;  // 승인/거절 처리 날짜

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;  // 승인한 관리자

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();  // 캠페인 생성 시간

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();  // 캠페인 정보 수정 시간

    /**
     * 승인 상태 열거형
     */
    public enum ApprovalStatus {
        PENDING("대기중"),
        APPROVED("승인됨"),
        REJECTED("거절됨");

        private final String description;

        ApprovalStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 캠페인 정보가 업데이트될 때 호출되어 수정 시간을 현재 시간으로 업데이트합니다.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
    
    /**
     * 캠페인이 처음 생성될 때 호출되어 생성 시간과 수정 시간을 설정합니다.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = ZonedDateTime.now();
        }
    }

    /**
     * 캠페인 미션 정보를 설정합니다.
     * @param missionInfo 설정할 미션 정보
     */
    public void setMissionInfo(CampaignMissionInfo missionInfo) {
        this.missionInfo = missionInfo;
        if (missionInfo != null) {
            missionInfo.setCampaign(this);
        }
    }

    /**
     * 미션 정보가 있는지 확인합니다.
     * @return 미션 정보 존재 여부
     */
    @Transient
    public boolean hasMissionInfo() {
        return missionInfo != null;
    }

    /**
     * 캠페인 신청을 추가합니다.
     * @param application 추가할 신청
     */
    public void addApplication(CampaignApplication application) {
        if (application == null) {
            return;
        }
        
        applications.add(application);
        application.setCampaign(this);
    }
    
    /**
     * 캠페인 신청을 제거합니다.
     * @param application 제거할 신청
     */
    public void removeApplication(CampaignApplication application) {
        if (application == null) {
            return;
        }
        
        applications.remove(application);
        application.setCampaign(null);
    }

    /**
     * 캠페인 위치를 설정합니다.
     * @param location 설정할 위치
     */
    public void setLocation(CampaignLocation location) {
        this.location = location;
        if (location != null) {
            location.setCampaign(this);
        }
    }

    /**
     * 위치 정보가 있는지 확인
     * @return 위치 정보 존재 여부
     */
    @Transient
    public boolean hasLocation() {
        return location != null;
    }

    /**
     * 현재 신청자 수를 반환합니다.
     * @return 현재 신청자 수
     */
    @Transient
    public int getCurrentApplicantCount() {
        return (int) applications.stream()
                .filter(app -> app.getApplicationStatus() == com.example.auth.constant.ApplicationStatus.APPLIED)
                .count();
    }

    /**
     * 캠페인을 승인합니다.
     * @param approver 승인한 관리자
     * @param comment 승인 코멘트
     */
    public void approve(User approver, String comment) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvedBy = approver;
        this.approvalComment = comment;
        this.approvalDate = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 캠페인을 거절합니다.
     * @param approver 거절한 관리자
     * @param comment 거절 사유
     */
    public void reject(User approver, String comment) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.approvedBy = approver;
        this.approvalComment = comment;
        this.approvalDate = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 승인 상태를 대기로 변경합니다.
     */
    public void resetApprovalStatus() {
        this.approvalStatus = ApprovalStatus.PENDING;
        this.approvedBy = null;
        this.approvalComment = null;
        this.approvalDate = null;
        this.updatedAt = ZonedDateTime.now();
    }
}
