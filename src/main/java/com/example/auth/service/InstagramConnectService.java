package com.example.auth.service;

import com.example.auth.constant.PlatformType;
import com.example.auth.domain.User;
import com.example.auth.domain.UserSnsPlatform;
import com.example.auth.repository.UserRepository;
import com.example.auth.repository.UserSnsPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstagramConnectService implements SnsConnectService {

    private final UserRepository userRepository;
    private final UserSnsPlatformRepository userSnsPlatformRepository;
    
    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile("^https?://(www\\.)?instagram\\.com/([^/]+)/?.*$");

    @Override
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return INSTAGRAM_PATTERN.matcher(url).matches();
    }

    /**
     * 인스타그램 URL에서 사용자명 추출
     */
    private String extractUsername(String instagramUrl) {
        Matcher matcher = INSTAGRAM_PATTERN.matcher(instagramUrl);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    @Override
    @Transactional
    public Long connect(Long userId, String url) {
        log.info("인스타그램 연동 시작: userId={}, url={}", userId, url);

        // URL 유효성 검사
        if (!isValidUrl(url)) {
            throw new IllegalArgumentException("유효한 인스타그램 URL이 아닙니다: " + url);
        }

        // 사용자명 추출 및 URL 정규화
        String username = extractUsername(url);
        if (username == null) {
            throw new IllegalArgumentException("인스타그램 사용자명을 추출할 수 없습니다: " + url);
        }
        
        // 정규화된 URL
        String normalizedUrl = "https://instagram.com/" + username;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // 이미 같은 사용자가 같은 URL로 연동한 계정이 있는지 확인
        Optional<UserSnsPlatform> existingPlatform = userSnsPlatformRepository
                .findByUserIdAndPlatformTypeAndAccountUrl(
                        userId, 
                        PlatformType.INSTAGRAM.getValue(), 
                        normalizedUrl);

        if (existingPlatform.isPresent()) {
            log.info("이미 연동된 인스타그램이 있습니다: platformId={}", existingPlatform.get().getId());
            return existingPlatform.get().getId();
        }

        // 다른 사용자가 이미 연동한 URL인지 확인
        Optional<UserSnsPlatform> otherUserPlatform = userSnsPlatformRepository
                .findByPlatformTypeAndAccountUrl(
                        PlatformType.INSTAGRAM.getValue(), 
                        normalizedUrl);

        if (otherUserPlatform.isPresent() && !otherUserPlatform.get().getUser().getId().equals(userId)) {
            log.warn("다른 사용자가 이미 연동한 인스타그램입니다: url={}", normalizedUrl);
            throw new IllegalStateException("이미 다른 사용자가 연동한 인스타그램입니다.");
        }

        // 플랫폼 데이터 초기화 (팔로워 수 수동 업데이트 필요)
        UserSnsPlatform platform = UserSnsPlatform.builder()
                .user(user)
                .platformType(PlatformType.INSTAGRAM.getValue())
                .accountUrl(normalizedUrl)
                .followerCount(0) // 기본값 0으로 설정
                .build();

        UserSnsPlatform savedPlatform = userSnsPlatformRepository.save(platform);
        log.info("인스타그램 연동 완료: platformId={}, url={}", savedPlatform.getId(), normalizedUrl);

        return savedPlatform.getId();
    }

    @Override
    @Transactional
    public void disconnect(Long userId, Long platformId) {
        log.info("인스타그램 연동 해제: userId={}, platformId={}", userId, platformId);

        UserSnsPlatform platform = userSnsPlatformRepository.findByUserIdAndId(userId, platformId)
                .orElseThrow(() -> new RuntimeException("연동된 인스타그램을 찾을 수 없습니다."));

        userSnsPlatformRepository.delete(platform);
        log.info("인스타그램 연동 해제 완료");
    }
}
