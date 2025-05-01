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
public class SnsCrawlService {

    private final UserSnsPlatformRepository platformRepository;

    /**
     * SNS 데이터 처리 (비동기, 크롤링 기능 제거됨)
     * @param platformId 대상 플랫폼 ID
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> crawlSnsDataAsync(Long platformId) {
        try {
            processSnsData(platformId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("비동기 처리 작업 실패: platformId={}, error={}", platformId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * SNS 데이터 처리 (동기, 크롤링 기능 제거됨)
     * @param platformId 대상 플랫폼 ID
     */
    public void processSnsData(Long platformId) {
        log.info("SNS 데이터 처리 시작: platformId={}", platformId);

        UserSnsPlatform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new RuntimeException("플랫폼을 찾을 수 없습니다: " + platformId));

        try {
            // 크롤링 로직 제거 - 플랫폼 별 처리 필요 없음
            // 기존 팔로워 수 유지 또는 0으로 설정
            if (platform.getFollowerCount() == null) {
                platform.updateFollowerCount(0);
            }

            // 마지막 처리 시간 업데이트
            platform.updateLastCrawledAt(LocalDateTime.now());
            platformRepository.save(platform);

            log.info("SNS 데이터 처리 완료: platformId={}, followerCount={}",
                    platformId, platform.getFollowerCount());
        } catch (Exception e) {
            log.error("SNS 데이터 처리 실패: platformId={}, error={}", platformId, e.getMessage(), e);
            throw new RuntimeException("SNS 데이터 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 크롤링 대신 팔로워 수 수동 업데이트를 위한 메서드
     * @param platformId 플랫폼 ID
     * @param followerCount 업데이트할 팔로워 수
     */
    public void updateFollowerCount(Long platformId, int followerCount) {
        // 먼저 플랫폼이 존재하는지 확인
        UserSnsPlatform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new RuntimeException("플랫폼을 찾을 수 없습니다: " + platformId));
        
        try {
            platform.updateFollowerCount(followerCount);
            platform.updateLastCrawledAt(LocalDateTime.now());
            platformRepository.save(platform);
            
            log.info("팔로워 수 수동 업데이트 완료: platformId={}, followerCount={}", 
                    platformId, followerCount);
        } catch (Exception e) {
            log.error("팔로워 수 수동 업데이트 실패: platformId={}, error={}", 
                    platformId, e.getMessage(), e);
            throw new RuntimeException("팔로워 수 업데이트 중 오류가 발생했습니다.", e);
        }
    }
}