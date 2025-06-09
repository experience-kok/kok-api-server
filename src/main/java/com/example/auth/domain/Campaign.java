package com.example.auth.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private Company company;  // 업체 정보 (선택)

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

    @Column(name = "application_deadline_date", nullable = false)
    private LocalDate applicationDeadlineDate;  // 신청 마감 날짜

    @Column(name = "selection_date", nullable = false)
    private LocalDate selectionDate;  // 참여자 선정 날짜

    @Column(name = "review_deadline_date", nullable = false)
    private LocalDate reviewDeadlineDate;  // 리뷰 제출 마감일

    @Column(name = "selection_criteria", columnDefinition = "TEXT")
    private String selectionCriteria;  // 선정 기준

    @Column(name = "mission_guide", columnDefinition = "TEXT")
    private String missionGuide;  // 리뷰어 미션 가이드 (마크다운 형식)

    // PostgreSQL의 TEXT[] 타입에 맞게 설정된 미션 키워드 배열
    @Column(name = "mission_keywords", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] missionKeywords;  // 리뷰 콘텐츠에 포함되어야 하는 키워드 배열

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CampaignCategory category;  // 캠페인 카테고리 (필수)
    


    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CampaignApplication> applications = new ArrayList<>();  // 캠페인 신청 목록

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

    // 키워드 관련 헬퍼 메소드

    /**
     * 미션 키워드 목록을 가져옵니다.
     * @return 키워드 목록 (null인 경우 빈 배열 반환)
     */
    public String[] getMissionKeywords() {
        return missionKeywords != null ? missionKeywords : new String[0];
    }

    /**
     * 미션 키워드 목록을 설정합니다.
     * @param keywords 키워드 배열
     */
    public void setMissionKeywords(String[] keywords) {
        this.missionKeywords = keywords;
    }

    /**
     * 미션 키워드 목록을 문자열 리스트로 변환하여 반환합니다.
     * @return 키워드 리스트
     */
    @Transient // DB에 저장되지 않는 변환 메소드
    public List<String> getMissionKeywordsList() {
        List<String> keywordList = new ArrayList<>();
        if (missionKeywords != null) {
            java.util.Collections.addAll(keywordList, missionKeywords);
        }
        return keywordList;
    }

    /**
     * 문자열 리스트로부터 미션 키워드를 설정합니다.
     * @param keywordList 키워드 리스트
     */
    public void setMissionKeywordsFromList(List<String> keywordList) {
        if (keywordList == null) {
            this.missionKeywords = null;
            return;
        }
        this.missionKeywords = keywordList.toArray(new String[0]);
    }

    /**
     * 미션 키워드를 추가합니다.
     * @param keyword 추가할 키워드
     */
    public void addMissionKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        List<String> currentKeywords = getMissionKeywordsList();
        if (!currentKeywords.contains(keyword)) {
            currentKeywords.add(keyword);
            setMissionKeywordsFromList(currentKeywords);
        }
    }

    /**
     * 미션 키워드를 제거합니다.
     * @param keyword 제거할 키워드
     */
    public void removeMissionKeyword(String keyword) {
        if (keyword == null || missionKeywords == null) {
            return;
        }

        List<String> currentKeywords = getMissionKeywordsList();
        if (currentKeywords.remove(keyword)) {
            setMissionKeywordsFromList(currentKeywords);
        }
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
     * 현재 신청자 수를 반환합니다.
     * @return 현재 신청자 수
     */
    @Transient
    public int getCurrentApplicantCount() {
        return (int) applications.stream()
                .filter(app -> app.getApplicationStatus() == com.example.auth.constant.ApplicationStatus.PENDING)
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
