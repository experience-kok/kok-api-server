package com.example.auth.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * 캠페인 미션 정보를 저장하는 엔티티 클래스
 * 
 * 캠페인의 미션 관련 상세 정보를 별도로 관리합니다.
 * 인플루언서가 수행해야 할 미션의 구체적인 요구사항들을 포함합니다.
 */
@Entity
@Table(name = "campaign_mission_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignMissionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 미션 정보 고유 식별자

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;  // 연관된 캠페인

    // 키워드 관련 필드들 (PostgreSQL 배열 타입)
    @Column(name = "title_keywords", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] titleKeywords;  // 제목에 사용할 키워드 목록 (선택사항)

    @Column(name = "body_keywords", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] bodyKeywords;  // 본문에 사용할 키워드 목록

    // 콘텐츠 요구사항
    @Column(name = "number_of_video")
    @Builder.Default
    private Integer numberOfVideo = 0;  // 본문에 포함해야할 영상 개수

    @Column(name = "number_of_image")
    @Builder.Default
    private Integer numberOfImage = 0;  // 본문에 포함해야할 이미지 개수

    @Column(name = "number_of_text")
    @Builder.Default
    private Integer numberOfText = 0;  // 본문에 작성해야할 글자 수

    @Column(name = "is_map")
    @Builder.Default
    private Boolean isMap = false;  // 본문에 지도 포함 여부

    // 미션 가이드 및 일정
    @Column(name = "mission_guide", columnDefinition = "TEXT")
    private String missionGuide;  // 미션 가이드 본문

    @Column(name = "mission_start_date")
    private LocalDate missionStartDate;  // 미션 시작일

    @Column(name = "mission_deadline_date")
    private LocalDate missionDeadlineDate;  // 미션 종료일

    // 메타데이터
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();  // 생성 시간

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();  // 수정 시간

    /**
     * 미션 정보가 업데이트될 때 호출되어 수정 시간을 현재 시간으로 업데이트합니다.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
    
    /**
     * 미션 정보가 처음 생성될 때 호출되어 생성 시간과 수정 시간을 설정합니다.
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

    // 키워드 관련 헬퍼 메소드들

    /**
     * 제목 키워드 목록을 가져옵니다.
     * @return 키워드 목록 (null인 경우 빈 배열 반환)
     */
    public String[] getTitleKeywords() {
        return titleKeywords != null ? titleKeywords : new String[0];
    }

    /**
     * 본문 키워드 목록을 가져옵니다.
     * @return 키워드 목록 (null인 경우 빈 배열 반환)
     */
    public String[] getBodyKeywords() {
        return bodyKeywords != null ? bodyKeywords : new String[0];
    }

    /**
     * 미션이 유효한지 확인합니다.
     * @return 유효성 여부
     */
    public boolean isValidMission() {
        return missionStartDate != null && 
               missionDeadlineDate != null && 
               !missionStartDate.isAfter(missionDeadlineDate);
    }

    /**
     * 콘텐츠 요구사항이 설정되어 있는지 확인합니다.
     * @return 설정 여부
     */
    public boolean hasContentRequirements() {
        return (numberOfVideo != null && numberOfVideo > 0) ||
               (numberOfImage != null && numberOfImage > 0) ||
               (numberOfText != null && numberOfText > 0);
    }

    /**
     * 키워드가 설정되어 있는지 확인합니다.
     * @return 설정 여부
     */
    public boolean hasKeywords() {
        return (titleKeywords != null && titleKeywords.length > 0) ||
               (bodyKeywords != null && bodyKeywords.length > 0);
    }
}
