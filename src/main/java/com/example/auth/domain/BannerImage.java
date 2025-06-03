package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

/**
 * 배너 이미지 정보를 저장하는 엔티티 클래스
 * 
 * 메인 페이지나 특정 페이지에 표시되는 배너 이미지와
 * 클릭 시 이동할 URL 정보를 관리합니다.
 */
@Entity
@Table(name = "banner_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 배너 고유 식별자

    @Column(name = "banner_url", nullable = false, columnDefinition = "TEXT")
    private String bannerUrl;  // 배너 이미지 URL

    @Column(name = "redirect_url", nullable = false, columnDefinition = "TEXT")
    private String redirectUrl;  // 클릭 시 이동할 URL

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();  // 배너 생성 시간

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();  // 배너 정보 수정 시간

    /**
     * 배너 정보가 업데이트될 때 호출되어 수정 시간을 현재 시간으로 업데이트합니다.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
    
    /**
     * 배너가 처음 생성될 때 호출되어 생성 시간과 수정 시간을 설정합니다.
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
}
