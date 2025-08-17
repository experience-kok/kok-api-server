package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 캠페인 좋아요 엔티티
 * 사용자가 캠페인에 좋아요를 표시하는 정보를 저장합니다.
 */
@Entity
@Table(name = "campaign_likes", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "campaign_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
