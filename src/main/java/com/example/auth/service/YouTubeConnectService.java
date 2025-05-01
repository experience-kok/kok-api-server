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
public class YouTubeConnectService implements SnsConnectService {

    private final UserRepository userRepository;
    private final UserSnsPlatformRepository userSnsPlatformRepository;
    
    private static final Pattern YOUTUBE_CHANNEL_PATTERN = Pattern.compile(
            "^https?://(www\\.)?youtube\\.com/(channel|c|user)/([^/]+)/?.*$");
    private static final Pattern YOUTUBE_CUSTOM_URL_PATTERN = Pattern.compile(
            "^https?://(www\\.)?youtube\\.com/(@[^/]+)/?.*$");

    @Override
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return YOUTUBE_CHANNEL_PATTERN.matcher(url).matches() || 
               YOUTUBE_CUSTOM_URL_PATTERN.matcher(url).matches();
    }

    /**
     * 유튜브 URL에서 채널 정보 추출
     */
    private String extractChannelInfo(String youtubeUrl) {
        Matcher channelMatcher = YOUTUBE_CHANNEL_PATTERN.matcher(youtubeUrl);
        if (channelMatcher.matches()) {
            return channelMatcher.group(2) + "/" + channelMatcher.group(3);
        }
        
        Matcher customUrlMatcher = YOUTUBE_CUSTOM_URL_PATTERN.matcher(youtubeUrl);
        if (customUrlMatcher.matches()) {
            return customUrlMatcher.group(2);
        }
        
        return null;
    }

    @Override
    @Transactional
    public Long connect(Long userId, String url) {
        log.info("유튜브 연동 시작: userId={}, url={}", userId, url);

        // URL 유효성 검사
        if (!isValidUrl(url)) {
            throw new IllegalArgumentException("유효한 유튜브 채널 URL이 아닙니다: " + url);
        }

        // 채널 정보 추출 및 URL 정규화
        String channelPath = extractChannelInfo(url);
        if (channelPath == null) {
            throw new IllegalArgumentException("유튜브 채널 정보를 추출할 수 없습니다: " + url);
        }
        
        // 정규화된 URL
        String normalizedUrl = "https://youtube.com/" + channelPath;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // 이미 같은 사용자가 같은 URL로 연동한 계정이 있는지 확인
        Optional<UserSnsPlatform> existingPlatform = userSnsPlatformRepository
                .findByUserIdAndPlatformTypeAndAccountUrl(
                        userId, 
                        PlatformType.YOUTUBE.getValue(), 
                        normalizedUrl);

        if (existingPlatform.isPresent()) {
            log.info("이미 연동된 유튜브 채널이 있습니다: platformId={}", existingPlatform.get().getId());
            return existingPlatform.get().getId();
        }

        // 다른 사용자가 이미 연동한 URL인지 확인
        Optional<UserSnsPlatform> otherUserPlatform = userSnsPlatformRepository
                .findByPlatformTypeAndAccountUrl(
                        PlatformType.YOUTUBE.getValue(), 
                        normalizedUrl);

        if (otherUserPlatform.isPresent() && !otherUserPlatform.get().getUser().getId().equals(userId)) {
            log.warn("다른 사용자가 이미 연동한 유튜브 채널입니다: url={}", normalizedUrl);
            throw new IllegalStateException("이미 다른 사용자가 연동한 유튜브 채널입니다.");
        }

        // 플랫폼 데이터 초기화 (팔로워 수 수동 업데이트 필요)
        UserSnsPlatform platform = UserSnsPlatform.builder()
                .user(user)
                .platformType(PlatformType.YOUTUBE.getValue())
                .accountUrl(normalizedUrl)
                .followerCount(0) // 기본값 0으로 설정
                .build();

        UserSnsPlatform savedPlatform = userSnsPlatformRepository.save(platform);
        log.info("유튜브 연동 완료: platformId={}, url={}", savedPlatform.getId(), normalizedUrl);

        return savedPlatform.getId();
    }

    @Override
    @Transactional
    public void disconnect(Long userId, Long platformId) {
        log.info("유튜브 연동 해제: userId={}, platformId={}", userId, platformId);

        UserSnsPlatform platform = userSnsPlatformRepository.findByUserIdAndId(userId, platformId)
                .orElseThrow(() -> new RuntimeException("연동된 유튜브 채널을 찾을 수 없습니다."));

        userSnsPlatformRepository.delete(platform);
        log.info("유튜브 연동 해제 완료");
    }
}
