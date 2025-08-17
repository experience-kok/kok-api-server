package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * 탈퇴한 사용자 정보를 관리하는 엔티티
 * 
 * 사용자 탈퇴 시 재가입 제한 정책(24시간)을 적용하기 위해
 * 탈퇴한 사용자의 정보를 임시로 저장합니다.
 */
@Entity
@Table(name = "withdrawn_users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawnUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 탈퇴한 사용자의 원래 ID (참조용)
     */
    @Column(name = "original_user_id", nullable = false)
    private Long originalUserId;

    /**
     * 탈퇴한 사용자의 이메일 (재가입 시 중복 체크용)
     */
    @Column(nullable = false)
    private String email;

    /**
     * 소셜 로그인 ID (카카오 등, 재가입 시 중복 체크용)
     */
    @Column(name = "social_id")
    private String socialId;

    /**
     * 로그인 제공업체 ('kakao', 'email')
     */
    @Column(nullable = false, length = 50)
    private String provider;

    /**
     * 계정 타입 ('SOCIAL', 'EMAIL')
     */
    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType;

    /**
     * 사용자가 입력한 탈퇴 사유
     */
    @Column(name = "withdrawal_reason", columnDefinition = "TEXT")
    private String withdrawalReason;

    /**
     * 탈퇴 처리된 시간
     */
    @Builder.Default
    @Column(name = "withdrawn_at", nullable = false)
    private ZonedDateTime withdrawnAt = ZonedDateTime.now();

    /**
     * 재가입 가능한 시간 (탈퇴 시간 + 24시간)
     */
    @Column(name = "can_rejoin_at", nullable = false)
    private ZonedDateTime canRejoinAt;

    /**
     * 레코드 생성 시간
     */
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    /**
     * 현재 시점에서 재가입 가능한지 확인
     * @return 재가입 가능 여부
     */
    public boolean canRejoinNow() {
        return ZonedDateTime.now().isAfter(canRejoinAt);
    }

    /**
     * 재가입까지 남은 시간(시간 단위)을 반환
     * @return 남은 시간 (음수면 이미 재가입 가능)
     */
    public long getRemainingHours() {
        ZonedDateTime now = ZonedDateTime.now();
        if (now.isAfter(canRejoinAt)) {
            return 0;
        }
        return java.time.Duration.between(now, canRejoinAt).toHours();
    }

    /**
     * 탈퇴 시 재가입 가능 시간을 자동으로 설정하는 팩토리 메서드
     * 개인정보보호를 위해 이메일과 소셜ID는 해시값으로 저장
     */
    public static WithdrawnUser createFromUser(User user, String withdrawalReason) {
        ZonedDateTime now = ZonedDateTime.now();
        
        return WithdrawnUser.builder()
                .originalUserId(user.getId())
                .email(hashValue(user.getEmail()))  // 이메일 해시값 저장
                .socialId(user.getSocialId() != null ? hashValue(user.getSocialId()) : null)  // 소셜ID 해시값 저장
                .provider(user.getProvider())
                .accountType(user.getAccountType())
                .withdrawalReason(withdrawalReason)
                .withdrawnAt(now)
                .canRejoinAt(now.plusDays(1))  // 24시간 후 재가입 가능
                .build();
    }

    /**
     * 값을 해시 처리 (SHA-256)
     * @param value 원본 값
     * @return 해시된 값
     */
    private static String hashValue(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 해시 처리 실패", e);
        }
    }
}
