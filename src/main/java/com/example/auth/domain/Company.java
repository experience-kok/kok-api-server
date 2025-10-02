package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * 업체 정보 엔티티
 * 사용자와 1:1 관계 (CLIENT 권한 사용자만 업체 정보 보유 가능)
 * 하나의 업체는 여러 개의 캠페인을 생성할 수 있음 (1:N 관계)
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "business_registration_number", unique = true, length = 20)
    private String businessRegistrationNumber;

    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "terms_agreed")
    private Boolean termsAgreed;

    @Column(name = "terms_agreed_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime termsAgreedAt;

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
     * 업체 정보를 업데이트합니다.
     */
    public void updateCompanyInfo(String companyName, String contactPerson, String phoneNumber, String businessRegistrationNumber) {
        if (companyName != null) this.companyName = companyName;
        if (contactPerson != null) this.contactPerson = contactPerson;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (businessRegistrationNumber != null) this.businessRegistrationNumber = businessRegistrationNumber;
        this.updatedAt = ZonedDateTime.now();
    }
}
