package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 캠페인 카테고리 정보를 저장하는 엔티티 클래스
 * 
 * 캠페인은 카페, 맛집, 뷰티 등 다양한 카테고리로 분류될 수 있으며,
 * 이 클래스는 그러한 카테고리 정보를 관리합니다.
 */
@Entity
@Table(name = "campaign_categories")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;  // 카테고리 고유 식별자

    @Column(name = "category_type", nullable = false)
    private String categoryType;  // 카테고리 유형 (예: 카페, 맛집, 뷰티 등)

    @Column(name = "category_name", nullable = false)
    private String categoryName;  // 카테고리 이름

    @Column(name = "created_at")
    private LocalDateTime createdAt;  // 카테고리 생성 시간

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // 카테고리 정보 수정 시간

    /**
     * 카테고리가 처음 생성될 때 호출되어 생성 시간과 수정 시간을 설정합니다.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 카테고리 정보가 업데이트될 때 호출되어 수정 시간을 현재 시간으로 업데이트합니다.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}