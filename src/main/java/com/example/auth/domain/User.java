package com.example.auth.domain;

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

    @Column
    @Builder.Default
    private Boolean active = true;
    private String phone;
    private String gender;
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

    public void updateGender(String gender) {
        this.gender = gender;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAge(Integer age) {
        this.age = age;
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
}
