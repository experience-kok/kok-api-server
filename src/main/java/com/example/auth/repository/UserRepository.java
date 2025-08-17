package com.example.auth.repository;

import com.example.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 소셜 로그인: provider + social_id 조합으로 사용자 조회
    Optional<User> findByProviderAndSocialId(String provider, String socialId);

    // 일반 로그인 확장 시: 이메일로 사용자 조회
    Optional<User> findByEmail(String email);
    
    // 닉네임으로 사용자 조회 (닉네임 중복 확인용)
    Optional<User> findByNickname(String nickname);

    // 이메일 존재 여부 확인 (중복 확인용)
    boolean existsByEmail(String email);

    // 닉네임 존재 여부 확인 (중복 확인용)
    boolean existsByNickname(String nickname);
}
