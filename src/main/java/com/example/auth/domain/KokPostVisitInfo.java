package com.example.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 콕포스트 방문 정보를 담는 임베디드 클래스
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KokPostVisitInfo {

    /**
     * 연락처 (필수)
     */
    @Column(nullable = false, length = 20, name = "contact_phone")
    private String contactPhone;

    /**
     * 홈페이지 주소 (선택)
     */
    @Column(length = 500, name = "homepage")
    private String homepage;

    /**
     * 위치 정보 (선택)
     */
    @Column(length = 200, name = "business_address")
    private String businessAddress;

    /**
     * 위치 정보 상세 (선택)
     */
    @Column(length = 200, name = "business_detail_address")
    private String businessDetailAddress;

    /**
     * 위도 (선택)
     */
    @Column(name = "lat")
    private Double lat;

    /**
     * 경도 (선택)
     */
    @Column(name = "lng")
    private Double lng;

    @Builder
    public KokPostVisitInfo(String contactPhone, String homepage, String businessAddress,
                           String businessDetailAddress, Double lat, Double lng) {
        this.contactPhone = contactPhone;
        this.homepage = homepage;
        this.businessAddress = businessAddress;
        this.businessDetailAddress = businessDetailAddress;
        this.lat = lat;
        this.lng = lng;
    }

    /**
     * 방문 정보 업데이트
     */
    public void update(String contactPhone, String homepage, String businessAddress,
                      String businessDetailAddress, Double lat, Double lng) {
        if (contactPhone != null) {
            this.contactPhone = contactPhone;
        }
        this.homepage = homepage;
        this.businessAddress = businessAddress;
        this.businessDetailAddress = businessDetailAddress;
        this.lat = lat;
        this.lng = lng;
    }
}
