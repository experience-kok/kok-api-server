package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 방문형 캠페인의 위치 정보를 저장하는 엔티티 클래스
 * 
 * 방문형 캠페인의 경우 체험단이 방문해야 하는 장소 정보(주소, 좌표 등)를 
 * 관리합니다. 이 정보는 지도 API와 연동하여 위치를 시각적으로 표시하는 데 사용됩니다.
 */
@Entity
@Table(name = "visit_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 방문 위치 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;  // 연결된 캠페인 (다대일 관계)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;  // 방문 장소 주소

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;  // 위도 좌표 (지도 API 연동용)

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;  // 경도 좌표 (지도 API 연동용)

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;  // 추가 장소 정보 (영업시간, 주차 정보 등)

    @Column(name = "created_at")
    private LocalDateTime createdAt;  // 위치 정보 생성 시간

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // 위치 정보 수정 시간

    /**
     * 위치 정보가 처음 생성될 때 호출되어 생성 시간과 수정 시간을 설정합니다.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 위치 정보가 업데이트될 때 호출되어 수정 시간을 현재 시간으로 업데이트합니다.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
