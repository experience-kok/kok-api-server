package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * 캠페인 카테고리 엔티티
 * 방문/배송 유형과 세부 카테고리를 관리
 */
@Entity
@Table(name = "campaign_categories", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"category_type", "category_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CategoryType categoryType;

    @Column(name = "category_name", nullable = false, length = 50)
    private String categoryName;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
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
     * 카테고리 타입 열거형
     */
    public enum CategoryType {
        방문("방문"),
        배송("배송");

        private final String value;

        CategoryType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
