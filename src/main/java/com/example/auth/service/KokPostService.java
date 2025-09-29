package com.example.auth.service;

import com.example.auth.constant.SortOption;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.KokPost;
import com.example.auth.dto.KokPostDetailResponse;
import com.example.auth.dto.KokPostListResponse;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.KokPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KokPostService {

    private final KokPostRepository kokPostRepository;
    private final CampaignRepository campaignRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // Redis 키 상수
    private static final String VIEW_COUNT_KEY_PREFIX = "kokpost:view:";
    private static final String DUPLICATE_CHECK_KEY_PREFIX = "kokpost:duplicate:";
    private static final String DAILY_VIEW_COUNT_KEY_PREFIX = "kokpost:daily:";
    private static final String POPULAR_POSTS_KEY = "kokpost:popular";

    // TTL 설정 (시간)
    private static final int DUPLICATE_CHECK_TTL_HOURS = 24;
    private static final int VIEW_COUNT_CACHE_TTL_HOURS = 1;
    private static final int POPULAR_POSTS_TTL_HOURS = 1;

    // Lua 스크립트 - 원자적 조회수 증가
    private static final String VIEW_COUNT_INCREMENT_SCRIPT = """
        local duplicate_key = KEYS[1]
        local view_count_key = KEYS[2]
        local daily_key = KEYS[3]
        local ttl = tonumber(ARGV[1])
        
        -- 중복 체크
        if redis.call('EXISTS', duplicate_key) == 0 then
            -- 중복 방지 키 설정
            redis.call('SETEX', duplicate_key, ttl, 'viewed')
            
            -- 실시간 조회수 증가
            local new_count = redis.call('INCR', view_count_key)
            redis.call('EXPIRE', view_count_key, ttl * 2)
            
            -- 일일 누적 조회수 증가 (배치 처리용)
            redis.call('INCR', daily_key)
            redis.call('EXPIRE', daily_key, ttl * 2)
            
            return new_count
        else
            -- 중복 조회인 경우 현재 조회수 반환
            local current_count = redis.call('GET', view_count_key)
            return current_count or 0
        end
        """;

    private final DefaultRedisScript<Long> viewCountScript =
            new DefaultRedisScript<>(VIEW_COUNT_INCREMENT_SCRIPT, Long.class);

    /**
     * 캠페인별 콕포스트 상세 조회 (개선된 버전)
     */
    public KokPostDetailResponse getKokPostDetailByCampaignId(Long campaignId, String clientIP, String userAgent) {
        log.info("개선된 콕포스트 상세 조회 - campaignId: {}", campaignId);

        // 1. 포스트 조회
        KokPost kokPost = kokPostRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 캠페인의 체험콕 글을 찾을 수 없습니다."));

        // 2. 조회수 증가 처리 (원자적 연산)
        Long realTimeViewCount = processViewCountIncrement(kokPost.getId(), clientIP, userAgent);

        // 3. 캠페인 상태 확인 (캐시 활용)
        Boolean isCampaignOpen = getCampaignStatus(campaignId);

        // 4. 비동기로 인기 포스트 업데이트
        updatePopularPostsAsync(kokPost.getId(), realTimeViewCount);

        log.info("콕포스트 상세 조회 완료 - postId: {}, 실시간 조회수: {}", kokPost.getId(), realTimeViewCount);

        return KokPostDetailResponse.fromEntityWithViewCount(kokPost, isCampaignOpen, realTimeViewCount);
    }

    /**
     * 원자적 조회수 증가 처리 (Lua 스크립트 사용) - 백업 포함
     */
    private Long processViewCountIncrement(Long postId, String clientIP, String userAgent) {
        try {
            // 복합 키 생성 (IP + UserAgent + 시간대)
            String complexKey = generateComplexKey(postId, clientIP, userAgent);

            String duplicateKey = DUPLICATE_CHECK_KEY_PREFIX + complexKey;
            String viewCountKey = VIEW_COUNT_KEY_PREFIX + postId;
            String dailyCountKey = DAILY_VIEW_COUNT_KEY_PREFIX + postId;
            String backupKey = "kokpost:backup_daily:" + postId; // 백업 키 추가

            // 🛡️ 백업 포함 Lua 스크립트 실행 (원자적 연산)
            Long newViewCount = redisTemplate.execute(
                    createBackupIncludeScript(),
                    List.of(duplicateKey, viewCountKey, dailyCountKey, backupKey),
                    String.valueOf(DUPLICATE_CHECK_TTL_HOURS * 3600)
            );

            // DB 조회수와 합산하여 총 조회수 계산
            Long dbViewCount = getDbViewCount(postId);
            Long totalViewCount = dbViewCount + (newViewCount != null ? newViewCount : 0L);

            log.debug("안전한 조회수 처리 완료 - postId: {}, Redis: {}, DB: {}, 총합: {}",
                    postId, newViewCount, dbViewCount, totalViewCount);

            return totalViewCount;

        } catch (Exception e) {
            log.error("조회수 증가 처리 중 오류 발생 - postId: {}", postId, e);
            // 오류 시 DB 조회수만 반환
            return getDbViewCount(postId);
        }
    }

    /**
     * 🛡️ 백업 포함 Lua 스크립트 생성
     */
    private DefaultRedisScript<Long> createBackupIncludeScript() {
        String backupIncludeScript = """
            local duplicate_key = KEYS[1]
            local view_count_key = KEYS[2]
            local daily_key = KEYS[3]
            local backup_key = KEYS[4]
            local ttl = tonumber(ARGV[1])
            
            -- 중복 체크
            if redis.call('EXISTS', duplicate_key) == 0 then
                -- 중복 방지 키 설정
                redis.call('SETEX', duplicate_key, ttl, 'viewed')
                
                -- 실시간 조회수 증가
                local new_count = redis.call('INCR', view_count_key)
                redis.call('EXPIRE', view_count_key, ttl * 2)
                
                -- 일일 누적 조회수 증가 (배치 처리용)
                redis.call('INCR', daily_key)
                redis.call('EXPIRE', daily_key, ttl * 2)
                
                -- 🛡️ 백업 키에도 동시 증가 (데이터 소실 방지)
                redis.call('INCR', backup_key)
                redis.call('EXPIRE', backup_key, ttl * 3) -- 더 긴 TTL
                
                return new_count
            else
                -- 중복 조회인 경우 현재 조회수 반환
                local current_count = redis.call('GET', view_count_key)
                return current_count or 0
            end
            """;
        
        return new DefaultRedisScript<>(backupIncludeScript, Long.class);
    }

    /**
     * 복합 키 생성 (중복 방지 정확도 향상)
     */
    private String generateComplexKey(Long postId, String clientIP, String userAgent) {
        // 시간대별 구분 (1시간 단위)
        int hourOfDay = LocalDateTime.now(ZoneId.systemDefault()).getHour();

        // IP + UserAgent 해시 + 시간대 조합
        String baseKey = clientIP + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
        return postId + ":" + baseKey.hashCode() + ":" + hourOfDay;
    }

    /**
     * DB에서 기본 조회수 조회 (캐시 적용)
     */
    private Long getDbViewCount(Long postId) {
        String cacheKey = "db_view_count:" + postId;
        String cachedCount = redisTemplate.opsForValue().get(cacheKey);

        if (cachedCount != null) {
            return Long.parseLong(cachedCount);
        }

        // 캐시 미스 시 DB 조회
        Long dbViewCount = kokPostRepository.findById(postId)
                .map(KokPost::getViewCount)
                .orElse(0L);

        // 캐시에 저장 (1시간 TTL)
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(dbViewCount),
                Duration.ofHours(VIEW_COUNT_CACHE_TTL_HOURS));

        return dbViewCount;
    }

    /**
     * 캠페인 상태 조회 (캐시 적용)
     */
    private Boolean getCampaignStatus(Long campaignId) {
        String cacheKey = "campaign_status:" + campaignId;
        String cachedStatus = redisTemplate.opsForValue().get(cacheKey);

        if (cachedStatus != null) {
            return Boolean.parseBoolean(cachedStatus);
        }

        // 캐시 미스 시 DB 조회
        Boolean isOpen = campaignRepository.findById(campaignId)
                .map(Campaign::isRecruitmentOpen)
                .orElse(false);

        // 캐시에 저장 (1시간 TTL)
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(isOpen),
                Duration.ofHours(1));

        return isOpen;
    }

    /**
     * 비동기로 인기 포스트 순위 업데이트
     */
    @Async
    public CompletableFuture<Void> updatePopularPostsAsync(Long postId, Long viewCount) {
        try {
            // Redis Sorted Set으로 인기 포스트 관리
            redisTemplate.opsForZSet().add(POPULAR_POSTS_KEY, String.valueOf(postId), viewCount.doubleValue());

            // 상위 100개만 유지
            redisTemplate.opsForZSet().removeRange(POPULAR_POSTS_KEY, 0, -101);

            // TTL 설정
            redisTemplate.expire(POPULAR_POSTS_KEY, Duration.ofHours(POPULAR_POSTS_TTL_HOURS));

            log.debug("인기 포스트 업데이트 완료 - postId: {}, viewCount: {}", postId, viewCount);

        } catch (Exception e) {
            log.error("인기 포스트 업데이트 실패 - postId: {}", postId, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 인기 포스트 목록 조회
     */
    public List<KokPostListResponse> getPopularKokPosts(int limit) {
        try {
            // Redis에서 인기 포스트 ID 조회 (조회수 높은 순)
            Set<String> popularPostIds = redisTemplate.opsForZSet()
                    .reverseRange(POPULAR_POSTS_KEY, 0, limit - 1);

            if (popularPostIds == null || popularPostIds.isEmpty()) {
                // 캐시 미스 시 DB에서 조회
                return getPopularPostsFromDb(limit);
            }

            // 포스트 ID를 Long으로 변환
            List<Long> postIds = popularPostIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            // DB에서 포스트 정보 조회
            List<KokPost> kokPosts = kokPostRepository.findAllById(postIds);

            return convertToResponseWithCampaignStatus(kokPosts);

        } catch (Exception e) {
            log.error("인기 포스트 조회 중 오류 발생", e);
            return getPopularPostsFromDb(limit);
        }
    }

    /**
     * DB에서 인기 포스트 조회 (폴백)
     */
    private List<KokPostListResponse> getPopularPostsFromDb(int limit) {
        List<KokPost> kokPosts = kokPostRepository.findTopByOrderByViewCountDesc(limit);
        return convertToResponseWithCampaignStatus(kokPosts);
    }

    /**
     * 실시간 조회수 조회
     */
    public Long getRealTimeViewCount(Long postId) {
        try {
            String viewCountKey = VIEW_COUNT_KEY_PREFIX + postId;
            String redisCount = redisTemplate.opsForValue().get(viewCountKey);
            Long redisViewCount = redisCount != null ? Long.parseLong(redisCount) : 0L;

            Long dbViewCount = getDbViewCount(postId);

            return dbViewCount + redisViewCount;

        } catch (Exception e) {
            log.error("실시간 조회수 조회 실패 - postId: {}", postId, e);
            return getDbViewCount(postId);
        }
    }

    /**
     * 콕포스트 전체 목록 조회 (기존 메서드 유지)
     */
    public List<KokPostListResponse> getAllKokPosts(SortOption sortOption) {
        log.info("콕포스트 전체 목록 조회 요청 - 정렬: {}", sortOption.getDescription());

        List<KokPost> kokPosts;

        switch (sortOption) {
            default -> kokPosts = kokPostRepository.findAllByOrderByCreatedAtDesc();
        }

        log.info("콕포스트 목록 조회 완료 - 정렬: {}, 총 {}개", sortOption.getDescription(), kokPosts.size());

        return convertToResponseWithCampaignStatus(kokPosts);
    }

    /**
     * 제목으로 콕포스트 검색 (기존 메서드 유지)
     */
    public List<KokPostListResponse> searchKokPostsByTitle(String title, SortOption sortOption) {
        log.info("콕포스트 제목 검색 요청 - 키워드: {}, 정렬: {}", title, sortOption.getDescription());

        List<KokPost> kokPosts = kokPostRepository.findByTitleContainingIgnoreCase(title, sortOption.getSort());

        log.info("콕포스트 검색 완료 - 키워드: {}, 정렬: {}, 결과: {}개",
                title, sortOption.getDescription(), kokPosts.size());

        return convertToResponseWithCampaignStatus(kokPosts);
    }

    /**
     * KokPost 리스트를 KokPostListResponse로 변환 (실시간 조회수 적용)
     */
    private List<KokPostListResponse> convertToResponseWithCampaignStatus(List<KokPost> kokPosts) {
        if (kokPosts.isEmpty()) {
            return Collections.emptyList();
        }

        // 모든 캠페인 ID 수집
        Set<Long> campaignIds = kokPosts.stream()
                .map(KokPost::getCampaignId)
                .collect(Collectors.toSet());

        // 한 번에 모든 캠페인 정보 조회 (성능 최적화)
        Map<Long, Boolean> campaignOpenStatusMap = campaignRepository.findAllById(campaignIds)
                .stream()
                .collect(Collectors.toMap(
                        Campaign::getId,
                        Campaign::isRecruitmentOpen
                ));

        // 응답 객체 생성하면서 실시간 조회수 적용
        return kokPosts.stream()
                .map(kokPost -> {
                    Boolean isCampaignOpen = campaignOpenStatusMap.getOrDefault(kokPost.getCampaignId(), false);
                    Long realTimeViewCount = getRealTimeViewCount(kokPost.getId());

                    return KokPostListResponse.builder()
                            .id(kokPost.getId())
                            .title(kokPost.getTitle())
                            .viewCount(realTimeViewCount) // 실시간 조회수 적용
                            .campaignId(kokPost.getCampaignId())
                            .authorId(kokPost.getAuthorId())
                            .authorName(kokPost.getAuthorName())
                            .contactPhone(kokPost.getVisitInfo() != null ? kokPost.getVisitInfo().getContactPhone() : null)
                            .businessAddress(kokPost.getVisitInfo() != null ? kokPost.getVisitInfo().getBusinessAddress() : null)
                            .isCampaignOpen(isCampaignOpen)
                            .createdAt(kokPost.getCreatedAt())
                            .updatedAt(kokPost.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}