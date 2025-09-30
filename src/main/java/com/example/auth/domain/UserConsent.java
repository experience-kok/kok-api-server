package com.example.auth.domain;

import com.example.auth.constant.ConsentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_consents", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "consent_type"}),
       indexes = {
           @Index(name = "idx_user_consent", columnList = "user_id, consent_type"),
           @Index(name = "idx_consent_type", columnList = "consent_type")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;

    @Column(nullable = false)
    private Boolean agreed;

    @Column(name = "consent_version", length = 20)
    private String consentVersion; // 약관 버전 관리

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // 동의 시점의 IP

    @CreationTimestamp
    @Column(name = "agreed_at", updatable = false)
    private LocalDateTime agreedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt; // 동의 철회 시점

    @Column(length = 500)
    private String notes; // 특이사항

    // 비즈니스 메서드
    public void revoke() {
        this.agreed = false;
        this.revokedAt = LocalDateTime.now();
    }

    public void updateConsent(Boolean agreed, String version, String ipAddress) {
        this.agreed = agreed;
        this.consentVersion = version;
        this.ipAddress = ipAddress;
        if (!agreed) {
            this.revokedAt = LocalDateTime.now();
        } else {
            this.revokedAt = null;
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
