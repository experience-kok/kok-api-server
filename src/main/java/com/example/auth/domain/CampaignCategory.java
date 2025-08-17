package com.example.auth.domain;

import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "방문형 캠페인")
        방문("방문"),

        @Schema(description = "배송형 캠페인")
        배송("배송");

        private final String displayName;

        CategoryType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * 한글 표시명으로부터 CategoryType 찾기
         */
        public static CategoryType fromDisplayName(String displayName) {
            for (CategoryType type : CategoryType.values()) {
                if (type.displayName.equals(displayName)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown category type: " + displayName);
        }
    }
}
