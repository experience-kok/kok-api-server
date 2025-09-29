package com.example.auth.scheduler;

import com.example.auth.domain.KokPost;
import com.example.auth.repository.KokPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class ViewCountBatchScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final KokPostRepository kokPostRepository;

    private static final String DAILY_VIEW_COUNT_KEY_PREFIX = "kokpost:daily:";
    private static final String BATCH_LOCK_KEY = "kokpost:batch:lock";
    private static final String BATCH_STATUS_KEY = "kokpost:batch:status";
    private static final int BATCH_SIZE = 100; // 배치 처리 크기
    private static final int LOCK_TIMEOUT_MINUTES = 30; // 배치 락 타임아웃

    /**
     * 매일 새벽 3시에 Redis 누적 조회수를 DB로 업데이트 (개선된 버전)
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    public void scheduledViewCountUpdate() {
        updateViewCountFromRedisToDb();
    }

    /**
     * 매시간 정각에도 부분 업데이트 실행 (실시간성 향상)
     */
    @Scheduled(cron = "0 0 * * * *") // 매시간 정각
    public void hourlyViewCountUpdate() {
        log.info("시간당 조회수 부분 업데이트 시작");
        partialViewCountUpdate();
    }

    /**
     * 메인 배치 업데이트 메서드 (개선된 버전)
     */
    @Transactional
    public void updateViewCountFromRedisToDb() {
        // 분산 환경에서 중복 실행 방지를 위한 락 획득
        if (!acquireBatchLock()) {
            log.warn("다른 인스턴스에서 배치가 실행 중입니다. 현재 배치를 건너뜁니다.");
            return;
        }

        try {
            log.info("=== 개선된 조회수 배치 업데이트 시작 ===");
            updateBatchStatus("RUNNING", "전체 배치 업데이트 시작");

            // Redis에서 모든 일일 조회수 키 조회
            Set<String> keys = redisTemplate.keys(DAILY_VIEW_COUNT_KEY_PREFIX + "*");

            if (keys == null || keys.isEmpty()) {
                log.info("업데이트할 조회수 데이터가 없습니다.");
                updateBatchStatus("COMPLETED", "업데이트할 데이터 없음");
                return;
            }

            // 배치 단위로 처리
            List<String> keysList = keys.stream().toList();
            int totalKeys = keysList.size();
            int processedCount = 0;
            int successCount = 0;
            int errorCount = 0;

            log.info("총 {}개의 조회수 업데이트 대상 발견", totalKeys);

            for (int i = 0; i < totalKeys; i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, totalKeys);
                List<String> batchKeys = keysList.subList(i, endIndex);

                log.info("배치 처리 중... ({}/{}) - 현재 배치 크기: {}",
                        i + 1, totalKeys, batchKeys.size());

                for (String key : batchKeys) {
                    try {
                        if (processSingleViewCount(key)) {
                            successCount++;
                        } else {
                            errorCount++;
                        }
                        processedCount++;

                        // 진행률 업데이트
                        if (processedCount % 10 == 0) {
                            double progress = (double) processedCount / totalKeys * 100;
                            updateBatchStatus("RUNNING",
                                    String.format("진행률: %.1f%% (%d/%d)", progress, processedCount, totalKeys));
                        }

                    } catch (Exception e) {
                        log.error("개별 조회수 업데이트 실패 - key: {}", key, e);
                        errorCount++;
                        processedCount++;
                    }
                }

                // 배치 간 짧은 대기 (DB 부하 분산)
                if (i + BATCH_SIZE < totalKeys) {
                    try {
                        Thread.sleep(100); // 100ms 대기
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("배치 처리 중 인터럽트 발생");
                        break;
                    }
                }
            }

            // 캐시 정리
            cleanupViewCountCache();

            String completionMessage = String.format(
                    "배치 업데이트 완료 - 총: %d건, 성공: %d건, 실패: %d건",
                    processedCount, successCount, errorCount);

            log.info("=== {} ===", completionMessage);
            updateBatchStatus("COMPLETED", completionMessage);

        } catch (Exception e) {
            log.error("조회수 배치 업데이트 중 치명적 오류 발생", e);
            updateBatchStatus("ERROR", "배치 처리 중 오류: " + e.getMessage());
        } finally {
            releaseBatchLock();
        }
    }

    /**
     * 부분 업데이트 (시간당 실행)
     */
    private void partialViewCountUpdate() {
        try {
            Set<String> keys = redisTemplate.keys(DAILY_VIEW_COUNT_KEY_PREFIX + "*");

            if (keys == null || keys.isEmpty()) {
                return;
            }

            // 조회수가 많은 상위 50개만 부분 업데이트
            keys.stream()
                    .limit(50)
                    .forEach(this::processSingleViewCount);

            log.info("시간당 부분 업데이트 완료 - 처리 대상: {}개", Math.min(keys.size(), 50));

        } catch (Exception e) {
            log.error("시간당 부분 업데이트 실패", e);
        }
    }

    /**
     * 🛡️ 안전한 단일 조회수 처리 (데이터 소실 방지)
     */
    private boolean processSingleViewCount(String key) {
        String batchId = "single_" + System.currentTimeMillis();
        
        try {
            // 키에서 postId 추출
            Long postId = extractPostIdFromKey(key);

            // 1단계: 백업 생성
            if (!createSafetyBackup(key, postId, batchId)) {
                log.error("백업 생성 실패 - 처리 중단: {}", key);
                return false;
            }

            // Redis에서 누적 조회수 가져오기
            String countStr = redisTemplate.opsForValue().get(key);
            if (countStr == null || "0".equals(countStr)) {
                cleanupBackup(postId, batchId);
                return true; // 업데이트할 데이터 없음
            }

            Long incrementCount = Long.parseLong(countStr);

            // 2단계: 안전한 DB 업데이트 (트랜잭션)
            boolean dbUpdateSuccess = performSafeDbUpdate(postId, incrementCount, batchId);
            
            if (dbUpdateSuccess) {
                // 3단계: 성공 시에만 지연 삭제 (즉시 삭제 X)
                markForSafeDeletion(key, postId, batchId);
                
                // DB 조회수 캐시 무효화
                String dbCacheKey = "db_view_count:" + postId;
                redisTemplate.delete(dbCacheKey);

                log.debug("안전한 조회수 업데이트 완료 - postId: {}, 증가량: {}", postId, incrementCount);
                return true;
            } else {
                // 실패 시 백업에서 복구
                restoreFromBackup(key, postId, batchId);
                return false;
            }

        } catch (NumberFormatException e) {
            log.error("잘못된 숫자 형식 - key: {}", key, e);
            // 잘못된 데이터는 격리 후 삭제
            quarantineBadData(key, batchId);
            return false;
        } catch (Exception e) {
            log.error("조회수 처리 실패 - 복구 시도: {}", key, e);
            // 예외 발생 시 백업에서 복구
            try {
                Long postId = extractPostIdFromKey(key);
                restoreFromBackup(key, postId, batchId);
            } catch (Exception restoreEx) {
                log.error("복구도 실패 - 수동 확인 필요: {}", key, restoreEx);
            }
            return false;
        }
    }

    /**
     * 백업 생성
     */
    private boolean createSafetyBackup(String originalKey, Long postId, String batchId) {
        try {
            String backupKey = "kokpost:safety_backup:" + batchId + ":" + postId;
            String originalValue = redisTemplate.opsForValue().get(originalKey);
            
            if (originalValue != null) {
                // 백업 생성 (7일 보관)
                redisTemplate.opsForValue().set(backupKey, originalValue, 7, TimeUnit.DAYS);
                
                // 백업 메타데이터 저장
                String metaKey = "kokpost:backup_meta:" + batchId + ":" + postId;
                String metaData = String.format("{\"original_key\":\"%s\",\"timestamp\":\"%s\",\"value\":\"%s\"}", 
                    originalKey, LocalDateTime.now(), originalValue);
                redisTemplate.opsForValue().set(metaKey, metaData, 7, TimeUnit.DAYS);
                
                log.debug("백업 생성 완료 - postId: {}, batchId: {}", postId, batchId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("백업 생성 실패 - postId: {}, batchId: {}", postId, batchId, e);
            return false;
        }
    }

    /**
     *  DB 업데이트
     */
    @Transactional(rollbackFor = Exception.class)
    private boolean performSafeDbUpdate(Long postId, Long incrementCount, String batchId) {
        try {
            // 1. 현재 상태 확인
            Optional<KokPost> postOpt = kokPostRepository.findById(postId);
            if (postOpt.isEmpty()) {
                log.warn("존재하지 않는 포스트 - postId: {}", postId);
                return false;
            }

            Long currentCount = postOpt.get().getViewCount();
            
            // 2. 업데이트 실행
            int updatedRows = kokPostRepository.incrementViewCountByAmount(postId, incrementCount);
            
            if (updatedRows != 1) {
                log.error("예상과 다른 업데이트 결과 - postId: {}, updatedRows: {}", postId, updatedRows);
                throw new RuntimeException("DB 업데이트 이상");
            }
            
            // 3. 업데이트 검증
            Long newCount = kokPostRepository.findById(postId)
                .map(KokPost::getViewCount)
                .orElseThrow(() -> new RuntimeException("업데이트 후 포스트 조회 실패"));
            
            if (!newCount.equals(currentCount + incrementCount)) {
                log.error("업데이트 검증 실패 - postId: {}, 예상: {}, 실제: {}", 
                    postId, currentCount + incrementCount, newCount);
                throw new RuntimeException("업데이트 검증 실패");
            }
            
            // 4. 성공 로그 기록
            recordSuccessfulUpdate(postId, incrementCount, currentCount, newCount, batchId);
            
            return true;
            
        } catch (Exception e) {
            log.error("안전한 DB 업데이트 실패 - postId: {}, incrementCount: {}", postId, incrementCount, e);
            return false;
        }
    }

    /**
     * 지연 삭제 마킹 (즉시 삭제 방지)
     */
    private void markForSafeDeletion(String originalKey, Long postId, String batchId) {
        try {
            // 삭제 예약 (24시간 후 실제 삭제)
            String deleteMarkKey = "kokpost:delete_scheduled:" + postId;
            String deleteInfo = String.format("{\"original_key\":\"%s\",\"batchId\":\"%s\",\"scheduled_time\":\"%s\"}", 
                originalKey, batchId, LocalDateTime.now().plusDays(1));
            
            redisTemplate.opsForValue().set(deleteMarkKey, deleteInfo, 2, TimeUnit.DAYS);
            
            // 원본 키에 처리 완료 마크 추가 (중복 처리 방지)
            String processedMarkKey = originalKey + ":processed:" + batchId;
            redisTemplate.opsForValue().set(processedMarkKey, "completed", 2, TimeUnit.DAYS);
            
            log.debug("지연 삭제 마킹 완료 - postId: {}", postId);
            
        } catch (Exception e) {
            log.error("지연 삭제 마킹 실패 - postId: {}", postId, e);
        }
    }

    /**
     * 🔄 백업에서 복구
     */
    private void restoreFromBackup(String originalKey, Long postId, String batchId) {
        try {
            String backupKey = "kokpost:safety_backup:" + batchId + ":" + postId;
            String backupValue = redisTemplate.opsForValue().get(backupKey);
            
            if (backupValue != null) {
                // 원본 키 복구
                redisTemplate.opsForValue().set(originalKey, backupValue, 2, TimeUnit.DAYS);
                
                // 복구 로그 기록
                String recoveryLogKey = "kokpost:recovery_log:" + batchId + ":" + postId;
                String recoveryInfo = String.format("{\"restored_key\":\"%s\",\"value\":\"%s\",\"timestamp\":\"%s\"}", 
                    originalKey, backupValue, LocalDateTime.now());
                redisTemplate.opsForValue().set(recoveryLogKey, recoveryInfo, 7, TimeUnit.DAYS);
                
                log.info("백업에서 복구 완료 - postId: {}, 복구된 값: {}", postId, backupValue);
            } else {
                log.error("백업 데이터를 찾을 수 없음 - postId: {}, batchId: {}", postId, batchId);
            }
            
        } catch (Exception e) {
            log.error("백업 복구 실패 - postId: {}, batchId: {}", postId, batchId, e);
        }
    }

    /**
     * 잘못된 데이터 격리
     */
    private void quarantineBadData(String key, String batchId) {
        try {
            String badValue = redisTemplate.opsForValue().get(key);
            if (badValue != null) {
                // 격리 영역으로 이동
                String quarantineKey = "kokpost:quarantine:" + batchId + ":" + System.currentTimeMillis();
                String quarantineData = String.format("{\"original_key\":\"%s\",\"bad_value\":\"%s\",\"timestamp\":\"%s\"}", 
                    key, badValue, LocalDateTime.now());
                
                redisTemplate.opsForValue().set(quarantineKey, quarantineData, 30, TimeUnit.DAYS);
                redisTemplate.delete(key); // 원본 삭제
                
                log.warn("잘못된 데이터 격리 완료 - key: {}, value: {}", key, badValue);
            }
        } catch (Exception e) {
            log.error("데이터 격리 실패 - key: {}", key, e);
        }
    }

    /**
     * 성공 로그 기록
     */
    private void recordSuccessfulUpdate(Long postId, Long incrementCount, Long oldCount, Long newCount, String batchId) {
        try {
            String logKey = "kokpost:update_log:" + batchId + ":" + postId;
            String logData = String.format(
                "{\"postId\":%d,\"increment\":%d,\"old_count\":%d,\"new_count\":%d,\"timestamp\":\"%s\",\"batchId\":\"%s\"}", 
                postId, incrementCount, oldCount, newCount, LocalDateTime.now(), batchId);
            
            redisTemplate.opsForValue().set(logKey, logData, 30, TimeUnit.DAYS);
            
        } catch (Exception e) {
            log.error("성공 로그 기록 실패", e);
        }
    }

    /**
     *  백업 정리
     */
    private void cleanupBackup(Long postId, String batchId) {
        try {
            String backupKey = "kokpost:safety_backup:" + batchId + ":" + postId;
            String metaKey = "kokpost:backup_meta:" + batchId + ":" + postId;
            
            redisTemplate.delete(backupKey);
            redisTemplate.delete(metaKey);
            
        } catch (Exception e) {
            log.error("백업 정리 실패 - postId: {}", postId, e);
        }
    }

    /**
     * Redis 키에서 postId 추출
     */
    private Long extractPostIdFromKey(String key) {
        String postIdStr = key.substring(DAILY_VIEW_COUNT_KEY_PREFIX.length());
        return Long.parseLong(postIdStr);
    }

    /**
     * 배치 락 획득
     */
    private boolean acquireBatchLock() {
        try {
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                    BATCH_LOCK_KEY,
                    "locked_" + System.currentTimeMillis(),
                    LOCK_TIMEOUT_MINUTES,
                    TimeUnit.MINUTES
            );

            if (Boolean.TRUE.equals(lockAcquired)) {
                log.info("배치 락 획득 성공");
                return true;
            } else {
                log.warn("배치 락 획득 실패 - 다른 프로세스에서 실행 중");
                return false;
            }
        } catch (Exception e) {
            log.error("배치 락 획득 중 오류", e);
            return false;
        }
    }

    /**
     * 배치 락 해제
     */
    private void releaseBatchLock() {
        try {
            redisTemplate.delete(BATCH_LOCK_KEY);
            log.info("배치 락 해제 완료");
        } catch (Exception e) {
            log.error("배치 락 해제 중 오류", e);
        }
    }

    /**
     * 배치 상태 업데이트
     */
    private void updateBatchStatus(String status, String message) {
        try {
            String statusInfo = String.format(
                    "{\"status\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                    status, message, LocalDateTime.now()
            );

            redisTemplate.opsForValue().set(
                    BATCH_STATUS_KEY,
                    statusInfo,
                    1,
                    TimeUnit.HOURS
            );

        } catch (Exception e) {
            log.error("배치 상태 업데이트 실패", e);
        }
    }

    /**
     * 조회수 관련 캐시 정리
     */
    private void cleanupViewCountCache() {
        try {
            // DB 조회수 캐시 정리 (1시간 이상 된 캐시)
            Set<String> dbCacheKeys = redisTemplate.keys("db_view_count:*");
            if (dbCacheKeys != null && !dbCacheKeys.isEmpty()) {
                // TTL이 짧은 캐시들만 삭제 (자연스러운 만료 대기)
                log.info("DB 조회수 캐시 정리 대상: {}개", dbCacheKeys.size());
            }

            // 오래된 중복 체크 키 정리
            Set<String> duplicateKeys = redisTemplate.keys("kokpost:duplicate:*");
            if (duplicateKeys != null && !duplicateKeys.isEmpty()) {
                long expiredCount = duplicateKeys.stream()
                        .mapToLong(key -> {
                            Long ttl = redisTemplate.getExpire(key);
                            return (ttl != null && ttl < 3600) ? 1 : 0; // 1시간 미만 TTL
                        })
                        .sum();

                log.info("중복 체크 키 정리 - 전체: {}개, 곧 만료: {}개", duplicateKeys.size(), expiredCount);
            }

        } catch (Exception e) {
            log.error("캐시 정리 중 오류", e);
        }
    }

    /**
     * 수동 배치 실행 (관리용)
     */
    public void manualBatchUpdate() {
        log.info("수동 조회수 배치 업데이트 실행");
        updateViewCountFromRedisToDb();
    }

    /**
     * 배치 상태 조회 (관리용)
     */
    public String getBatchStatus() {
        try {
            String status = redisTemplate.opsForValue().get(BATCH_STATUS_KEY);
            return status != null ? status : "{\"status\":\"IDLE\",\"message\":\"대기 중\"}";
        } catch (Exception e) {
            log.error("배치 상태 조회 실패", e);
            return "{\"status\":\"ERROR\",\"message\":\"상태 조회 실패\"}";
        }
    }

    /**
     * Redis 조회수 데이터 통계 조회 (관리용)
     */
    public String getRedisViewCountStatistics() {
        try {
            Set<String> dailyKeys = redisTemplate.keys(DAILY_VIEW_COUNT_KEY_PREFIX + "*");
            Set<String> duplicateKeys = redisTemplate.keys("kokpost:duplicate:*");
            Set<String> viewCountKeys = redisTemplate.keys("kokpost:view:*");

            // 총 누적 조회수 계산
            long totalPendingViews = 0;
            if (dailyKeys != null) {
                for (String key : dailyKeys) {
                    try {
                        String countStr = redisTemplate.opsForValue().get(key);
                        if (countStr != null) {
                            totalPendingViews += Long.parseLong(countStr);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("잘못된 조회수 데이터 - key: {}", key);
                    }
                }
            }

            return String.format(
                    "{\"dailyKeys\":%d,\"duplicateKeys\":%d,\"viewCountKeys\":%d,\"totalPendingViews\":%d,\"timestamp\":\"%s\"}",
                    dailyKeys != null ? dailyKeys.size() : 0,
                    duplicateKeys != null ? duplicateKeys.size() : 0,
                    viewCountKeys != null ? viewCountKeys.size() : 0,
                    totalPendingViews,
                    LocalDateTime.now()
            );

        } catch (Exception e) {
            log.error("Redis 통계 조회 실패", e);
            return String.format(
                    "{\"error\":\"통계 조회 실패\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                    e.getMessage(),
                    LocalDateTime.now()
            );
        }
    }

    /**
     * 특정 포스트의 Redis 조회수 강제 DB 반영 (관리용)
     */
    public boolean forceUpdateSinglePost(Long postId) {
        try {
            String key = DAILY_VIEW_COUNT_KEY_PREFIX + postId;
            boolean result = processSingleViewCount(key);

            if (result) {
                log.info("포스트 조회수 강제 업데이트 완료 - postId: {}", postId);
            } else {
                log.warn("포스트 조회수 강제 업데이트 실패 - postId: {}", postId);
            }

            return result;

        } catch (Exception e) {
            log.error("포스트 조회수 강제 업데이트 중 오류 - postId: {}", postId, e);
            return false;
        }
    }

    /**
     * 배치 처리 성능 모니터링을 위한 메트릭 수집
     */
    public void collectBatchMetrics() {
        try {
            long startTime = System.currentTimeMillis();

            // Redis 키 개수 조회
            Set<String> dailyKeys = redisTemplate.keys(DAILY_VIEW_COUNT_KEY_PREFIX + "*");
            int pendingUpdates = dailyKeys != null ? dailyKeys.size() : 0;

            // 메모리 사용량 확인
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();

            long endTime = System.currentTimeMillis();

            log.info("배치 메트릭 수집 완료 - 대기 업데이트: {}건, 메모리 사용률: {}%, 수집 시간: {}ms",
                    pendingUpdates,
                    (usedMemory * 100 / maxMemory),
                    (endTime - startTime));

        } catch (Exception e) {
            log.error("배치 메트릭 수집 실패", e);
        }
    }
}