package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * 캠페인 위치 정보 엔티티 (간소화 버전)
 */
@Entity
@Table(name = "campaign_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false, unique = true)
    private Campaign campaign;  // 캠페인 외래키 (1:1 관계)

    @Column(name = "latitude")
    private Double latitude;  // 위도 (선택사항)

    @Column(name = "longitude")
    private Double longitude;  // 경도 (선택사항)

    // 방문 정보 필드들 추가
    @Column(name = "homepage", length = 500)
    private String homepage;  // 공식 홈페이지 주소

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;  // 일반 유저에게 공개되는 연락처

    @Column(name = "visit_reservation_info", columnDefinition = "TEXT")
    private String visitAndReservationInfo;  // 방문 및 예약 안내

    @Column(name = "business_address", length = 200)
    private String businessAddress;  // 사업장 주소

    @Column(name = "business_detail_address", length = 100)
    private String businessDetailAddress;  // 사업장 상세 주소

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now();
        }
    }

    /**
     * 좌표가 있는지 확인
     * @return 위도/경도 모두 있으면 true
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
