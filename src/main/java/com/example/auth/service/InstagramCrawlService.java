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
public class InstagramCrawlService {

    private final UserSnsPlatformRepository userSnsPlatformRepository;

    /**
     * 인스타그램 팔로워 수 크롤링 (크롤링 코드 제거 - 메모리 최적화)
     * @param platform 연동된 플랫폼 정보
     * @return 팔로워 수 또는 0
     */
    @Async
    public CompletableFuture<Integer> crawlFollowerCount(UserSnsPlatform platform) {
        String url = platform.getAccountUrl();
        log.info("인스타그램 연동 처리: platformId={}, url={}", platform.getId(), url);
        
        try {
            // 크롤링 코드 제거 - 기존 데이터 유지 또는 기본값 설정
            int followerCount = platform.getFollowerCount() != null ? platform.getFollowerCount() : 0;
            
            // DB에 업데이트
            platform.updateLastCrawledAt(LocalDateTime.now());
            userSnsPlatformRepository.save(platform);
            
            log.info("인스타그램 연동 처리 완료: platformId={}, followerCount={}", 
                    platform.getId(), followerCount);
            
            return CompletableFuture.completedFuture(followerCount);
            
        } catch (Exception e) {
            log.error("인스타그램 연동 처리 실패: platformId={}, error={}", 
                    platform.getId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(0);
        }
    }
}
