package com.example.auth.scheduler;

import com.example.auth.service.S3CleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * S3 미사용 이미지 자동 정리를 위한 스케줄러
 * 설정된 주기에 따라 미사용 이미지를 검색하고 삭제합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3CleanupScheduler {

    private final S3CleanupService s3CleanupService;

    @Value("${aws.s3.cleanup.enabled:false}")
    private boolean cleanupEnabled;

    @Value("${aws.s3.cleanup.auto-delete:false}")
    private boolean autoDeleteEnabled;

    @Value("${aws.s3.cleanup.default-retention-days:30}")
    private int retentionDays;

    /**
     * 매일 새벽 3시에 미사용 이미지 정리 작업 실행
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시
    public void scheduledCleanup() {
        if (!cleanupEnabled) {
            log.debug("S3 미사용 이미지 자동 정리 기능이 비활성화되어 있습니다.");
            return;
        }

        log.info("S3 미사용 이미지 정리 작업 시작 - 보존 기간: {}일, 자동 삭제: {}", 
                retentionDays, autoDeleteEnabled);

        try {
            // 미사용 이미지 검색 및 정리
            // autoDeleteEnabled가 false면 실제 삭제는 하지 않고 로그만 남김
            Map<String, Object> result = s3CleanupService.cleanupUnusedImages(
                    "", // 모든 경로 대상
                    retentionDays, 
                    !autoDeleteEnabled // dryRun: autoDeleteEnabled가 false면 실제 삭제하지 않음
            );

            int totalFound = (Integer) result.get("totalFound");
            
            if (autoDeleteEnabled && totalFound > 0) {
                int deletedCount = (Integer) result.getOrDefault("deletedCount", 0);
                log.info("S3 미사용 이미지 정리 완료 - 발견: {}개, 삭제: {}개", 
                        totalFound, deletedCount);
            } else if (totalFound > 0) {
                log.info("S3 미사용 이미지 검색 완료 - 발견: {}개 (자동 삭제 비활성화 상태)", 
                        totalFound);
            } else {
                log.info("미사용 이미지가 발견되지 않았습니다.");
            }
        } catch (Exception e) {
            log.error("S3 미사용 이미지 정리 작업 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
