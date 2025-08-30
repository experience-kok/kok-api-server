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
public class NaverBlogConnectService implements SnsConnectService {

    private final UserRepository userRepository;
    private final UserSnsPlatformRepository userSnsPlatformRepository;
    
    private static final Pattern NAVER_BLOG_PATTERN = Pattern.compile("^https?://blog\\.naver\\.com/([^/]+)/?.*$");

    @Override
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return NAVER_BLOG_PATTERN.matcher(url).matches();
    }

    /**
     * 네이버 블로그 URL에서 블로그 ID 추출
     */
    private String extractBlogId(String blogUrl) {
        Matcher matcher = NAVER_BLOG_PATTERN.matcher(blogUrl);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    @Transactional
    public Long connect(Long userId, String url) {
        log.info("네이버 블로그 연동 시작: userId={}, url={}", userId, url);

        // URL 유효성 검사
        if (!isValidUrl(url)) {
            throw new IllegalArgumentException("유효한 네이버 블로그 URL이 아닙니다: " + url);
        }

        // 블로그 ID 추출 및 URL 정규화
        String blogId = extractBlogId(url);
        if (blogId == null) {
            throw new IllegalArgumentException("네이버 블로그 ID를 추출할 수 없습니다: " + url);
        }
        
        // 정규화된 URL
        String normalizedUrl = "https://blog.naver.com/" + blogId;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // 이미 같은 사용자가 Blog 플랫폼을 연동했는지 확인 (플랫폼 타입 기준)
        Optional<UserSnsPlatform> existingBlogPlatform = userSnsPlatformRepository
                .findByUserIdAndPlatformType(userId, PlatformType.BLOG.getValue());

        if (existingBlogPlatform.isPresent()) {
            UserSnsPlatform platform = existingBlogPlatform.get();
            
            // 같은 URL인 경우 기존 연동 정보 반환
            if (normalizedUrl.equals(platform.getAccountUrl())) {
                log.info("이미 연동된 동일한 네이버 블로그가 있습니다: platformId={}", platform.getId());
                return platform.getId();
            }
            // 다른 URL인 경우 기존 연동을 새 URL로 업데이트 (덮어쓰기)
            else {
                // 🔍 업데이트 전에 다른 사용자가 이미 연동했는지 확인
                Optional<UserSnsPlatform> otherUserPlatform = userSnsPlatformRepository
                        .findByPlatformTypeAndAccountUrl(
                                PlatformType.BLOG.getValue(), 
                                normalizedUrl);

                if (otherUserPlatform.isPresent() && !otherUserPlatform.get().getUser().getId().equals(userId)) {
                    log.warn("다른 사용자가 이미 연동한 네이버 블로그입니다: url={}", normalizedUrl);
                    throw new IllegalStateException("이미 다른 사용자가 연동한 네이버 블로그입니다.");
                }

                log.info("기존 네이버 블로그 연동을 새 URL로 업데이트합니다: userId={}, old={}, new={}", 
                        userId, platform.getAccountUrl(), normalizedUrl);
                platform.setAccountUrl(normalizedUrl);
                platform.setFollowerCount(0); // 새 계정이므로 팔로워 수 초기화
                platform.setLastCrawledAt(null); // 크롤링 상태 초기화
                
                UserSnsPlatform updatedPlatform = userSnsPlatformRepository.save(platform);
                log.info("네이버 블로그 연동 업데이트 완료: platformId={}, url={}", updatedPlatform.getId(), normalizedUrl);
                return updatedPlatform.getId();
            }
        }

        // 다른 사용자가 이미 연동한 URL인지 확인
        Optional<UserSnsPlatform> otherUserPlatform = userSnsPlatformRepository
                .findByPlatformTypeAndAccountUrl(
                        PlatformType.BLOG.getValue(), 
                        normalizedUrl);

        if (otherUserPlatform.isPresent() && !otherUserPlatform.get().getUser().getId().equals(userId)) {
            log.warn("다른 사용자가 이미 연동한 네이버 블로그입니다: url={}", normalizedUrl);
            throw new IllegalStateException("이미 다른 사용자가 연동한 네이버 블로그입니다.");
        }

        // 플랫폼 데이터 초기화 (팔로워 수 수동 업데이트 필요)
        UserSnsPlatform platform = UserSnsPlatform.builder()
                .user(user)
                .platformType(PlatformType.BLOG.getValue())
                .accountUrl(normalizedUrl)
                .followerCount(0) // 기본값 0으로 설정
                .build();

        UserSnsPlatform savedPlatform = userSnsPlatformRepository.save(platform);
        log.info("네이버 블로그 연동 완료: platformId={}, url={}", savedPlatform.getId(), normalizedUrl);

        return savedPlatform.getId();
    }

    @Override
    @Transactional
    public void disconnect(Long userId, Long platformId) {
        log.info("네이버 블로그 연동 해제: userId={}, platformId={}", userId, platformId);

        UserSnsPlatform platform = userSnsPlatformRepository.findByUserIdAndId(userId, platformId)
                .orElseThrow(() -> new RuntimeException("연동된 네이버 블로그를 찾을 수 없습니다."));

        userSnsPlatformRepository.delete(platform);
        log.info("네이버 블로그 연동 해제 완료");
    }
}
