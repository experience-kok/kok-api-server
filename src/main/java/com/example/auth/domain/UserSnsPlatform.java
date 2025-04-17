package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sns_platforms")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class UserSnsPlatform {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // platformType_id 참조 대신 직접 String으로 저장
    @Column(name = "platform_type", nullable = false)
    private String platformType;

    @Column(name = "account_url", nullable = false)
    private String accountUrl;

    @Column(name = "follower_count")
    private Integer followerCount;

    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 수정 메서드들
    public void updateAccountUrl(String accountUrl) {
        this.accountUrl = accountUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFollowerCount(Integer followerCount) {
        this.followerCount = followerCount;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLastCrawledAt(LocalDateTime lastCrawledAt) {
        this.lastCrawledAt = lastCrawledAt;
        this.updatedAt = LocalDateTime.now();
    }
}