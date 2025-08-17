package com.example.auth.service;

import com.example.auth.domain.User;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 기반 인증 서비스
 * 일반 로그인 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 이메일/비밀번호로 사용자 인증
     * @param email 이메일
     * @param password 비밀번호
     * @return 인증된 사용자 엔티티
     * @throws IllegalArgumentException 인증 실패시
     */
    public User authenticateUser(String email, String password) {
        log.info("이메일 로그인 인증 시도: email={}", email);

        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 이메일 기반 계정인지 확인
        if (!"email".equals(user.getProvider()) || !"EMAIL".equals(user.getAccountType())) {
            throw new IllegalArgumentException("소셜 로그인으로 가입된 계정입니다. 해당 소셜 서비스로 로그인해주세요.");
        }

        // 계정 활성화 상태 확인
        if (!user.getActive()) {
            throw new IllegalArgumentException("비활성화된 계정입니다. 고객센터에 문의해주세요.");
        }

        // 비밀번호 확인
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        log.info("이메일 로그인 인증 성공: userId={}, email={}", user.getId(), user.getEmail());
        return user;
    }
}
