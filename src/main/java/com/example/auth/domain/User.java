package com.example.auth.domain;

import com.example.auth.constant.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String provider;

    @Column(name = "social_id", unique = true, nullable = false)
    private String socialId;

    @Column(unique = true)
    private String email;

    private String nickname;

    @Column(name = "profile_img")
    private String profileImg;

    @Column(nullable = true)
    private String password;

    @Column(name = "account_type", nullable = false)
    @Builder.Default
    private String accountType = "SOCIAL";

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column
    @Builder.Default
    private Boolean active = true;
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    @Schema(hidden = true) // OpenAPI 문서에서 숨김
    private Gender gender = Gender.UNKNOWN;
    
    private Integer age;

    private String role;

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
    public void updateNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfileImg(String profileImg) {
        this.profileImg = profileImg;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePhone(String phone) {
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateGender(Gender gender) {
        this.gender = gender;
        this.updatedAt = LocalDateTime.now();
    }
    
    // String을 받아서 Gender enum으로 변환하는 오버로드 메서드
    public void updateGender(String genderStr) {
        this.gender = Gender.fromString(genderStr);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAge(Integer age) {
        this.age = age;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateEmail(String email) {
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.updatedAt = LocalDateTime.now();
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateRole(String role) {
        this.role = role;
        this.updatedAt = LocalDateTime.now();
    }

    public String getProvider() {
        return provider;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getSocialId() {
        return socialId;
    }
}