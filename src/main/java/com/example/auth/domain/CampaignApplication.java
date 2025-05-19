package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 캠페인 신청 정보를 저장하는 엔티티 클래스
 */
@Entity
@Table(name = "campaign_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 신청 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;  // 신청한 캠페인

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 신청한 사용자

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";  // 신청 상태: 'pending', 'rejected', 'completed'

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();  // 신청 시간

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();  // 신청 정보 최종 수정 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 신청 상태를 업데이트합니다.
     * @param status 새로운 상태 ('pending', 'rejected', 'completed')
     */
    public void updateStatus(String status) {
        if (status == null || !isValidStatus(status)) {
            throw new IllegalArgumentException("유효하지 않은 상태값입니다: " + status);
        }
        this.status = status;
    }

    /**
     * 상태값이 유효한지 확인합니다.
     * @param status 확인할 상태값
     * @return 유효 여부
     */
    private boolean isValidStatus(String status) {
        return "pending".equals(status) || "rejected".equals(status) || "completed".equals(status);
    }
}