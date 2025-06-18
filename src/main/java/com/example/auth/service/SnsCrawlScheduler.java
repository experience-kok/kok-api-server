package com.example.auth.service;

import com.example.auth.domain.UserSnsPlatform;
import com.example.auth.repository.UserSnsPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnsCrawlScheduler {

    private final UserSnsPlatformRepository platformRepository;
    private final SnsCrawlService snsCrawlService;

    /**
     * 매일 새벽 3시에 전체 SNS 플랫폼 크롤링 수행
     * cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduleDailyCrawl() {
        log.info("일일 SNS 크롤링 작업 시작: {}", LocalDateTime.now());

        try {
            List<UserSnsPlatform> platforms = platformRepository.findAll();
            log.info("크롤링 대상 플랫폼 수: {}", platforms.size());

            for (UserSnsPlatform platform : platforms) {
                try {
                    // 각 플랫폼별 크롤링 수행
                    snsCrawlService.crawlSnsDataAsync(platform.getId());
                } catch (Exception e) {
                    log.error("플랫폼 크롤링 실패 (작업 계속 진행): platformId={}, error={}",
                            platform.getId(), e.getMessage());
                }
            }

            log.info("일일 SNS 크롤링 작업 완료: {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("일일 SNS 크롤링 작업 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 일주일 이상 크롤링되지 않은 플랫폼 재크롤링 (매주 월요일 오전 4시)
     */
    @Scheduled(cron = "0 0 4 * * MON")
    public void scheduleWeeklyCleanup() {
        log.info("주간 미크롤링 플랫폼 정리 작업 시작: {}", LocalDateTime.now());

        try {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);

            // 일주일 이상 크롤링되지 않은 플랫폼 조회
            List<UserSnsPlatform> platforms = platformRepository.findByLastCrawledAtBeforeOrLastCrawledAtIsNull(oneWeekAgo);
            log.info("일주일 이상 미크롤링 플랫폼 수: {}", platforms.size());

            for (UserSnsPlatform platform : platforms) {
                try {
                    snsCrawlService.crawlSnsDataAsync(platform.getId());
                } catch (Exception e) {
                    log.error("미크롤링 플랫폼 처리 실패: platformId={}, error={}",
                            platform.getId(), e.getMessage());
                }
            }

            log.info("주간 미크롤링 플랫폼 정리 작업 완료: {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("주간 미크롤링 플랫폼 정리 작업 실패: {}", e.getMessage(), e);
        }
    }
}