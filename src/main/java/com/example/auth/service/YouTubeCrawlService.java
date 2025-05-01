package com.example.auth.service;

import com.example.auth.domain.UserSnsPlatform;
import com.example.auth.repository.UserSnsPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeCrawlService {

    private final UserSnsPlatformRepository userSnsPlatformRepository;

    /**
     * 크롤링 없이 기본 팔로워 수 설정 (크롤링 코드 제거)
     * @param platform 연동된 플랫폼 정보
     * @return 팔로워 수 또는 0
     */
    @Async
    public CompletableFuture<Integer> getSubscriberCount(UserSnsPlatform platform) {
        String url = platform.getAccountUrl();
        log.info("유튜브 채널 연동 처리: platformId={}, url={}", platform.getId(), url);
        
        try {
            // 크롤링 기능 제거 - 기존 값 유지 또는 기본값 설정
            int followerCount = platform.getFollowerCount() != null ? platform.getFollowerCount() : 0;
            
            // DB에 업데이트
            platform.updateLastCrawledAt(LocalDateTime.now());
            userSnsPlatformRepository.save(platform);
            
            log.info("유튜브 채널 연동 완료: platformId={}, followerCount={}", 
                    platform.getId(), followerCount);
            
            return CompletableFuture.completedFuture(followerCount);
            
        } catch (Exception e) {
            log.error("유튜브 채널 연동 처리 중 오류 발생: platformId={}, error={}", 
                    platform.getId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(0);
        }
    }
}
