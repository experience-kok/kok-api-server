package com.example.auth.scheduler;

import com.example.auth.repository.KokPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class ViewCountBatchScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final KokPostRepository kokPostRepository;
    
    private static final String DAILY_VIEW_COUNT_KEY_PREFIX = "daily_view_count:";

    /**
     * 매일 새벽 3시에 Redis 누적 조회수를 DB로 업데이트
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    @Transactional
    public void updateViewCountFromRedisToDb() {
        log.info("=== 조회수 배치 업데이트 시작 ===");
        
        try {
            // Redis에서 모든 일일 조회수 키 조회
            Set<String> keys = redisTemplate.keys(DAILY_VIEW_COUNT_KEY_PREFIX + "*");
            
            if (keys == null || keys.isEmpty()) {
                log.info("업데이트할 조회수 데이터가 없습니다.");
                return;
            }
            
            int updateCount = 0;
            
            for (String key : keys) {
                try {
                    // 키에서 postId 추출
                    Long postId = extractPostIdFromKey(key);
                    
                    // Redis에서 누적 조회수 가져오기
                    String countStr = redisTemplate.opsForValue().get(key);
                    if (countStr == null) continue;
                    
                    Long incrementCount = Long.parseLong(countStr);
                    
                    // DB 업데이트
                    kokPostRepository.incrementViewCountByAmount(postId, incrementCount);
                    
                    // Redis 키 삭제 (업데이트 완료)
                    redisTemplate.delete(key);
                    
                    updateCount++;
                    log.info("조회수 업데이트 완료 - postId: {}, 증가량: {}", postId, incrementCount);
                    
                } catch (Exception e) {
                    log.error("개별 조회수 업데이트 실패 - key: {}", key, e);
                }
            }
            
            log.info("=== 조회수 배치 업데이트 완료 - 총 {}건 ===", updateCount);
            
        } catch (Exception e) {
            log.error("조회수 배치 업데이트 중 오류 발생", e);
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
     * 수동 배치 실행 (관리용)
     */
    public void manualBatchUpdate() {
        log.info("수동 조회수 배치 업데이트 실행");
        updateViewCountFromRedisToDb();
    }
}
