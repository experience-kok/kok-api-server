package com.example.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 검색 통계 수집 및 인기 검색어 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchAnalyticsService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // Redis 키 상수
    private static final String TRENDING_KEYWORDS_KEY = "trending:keywords";
    private static final String SEARCH_COUNT_PREFIX = "search:count:";
    private static final String DAILY_SEARCH_PREFIX = "daily:search:";
    
    // 설정값
    private static final int MAX_TRENDING_KEYWORDS = 50; // Redis에 저장할 최대 키워드 수
    private static final Duration TRENDING_TTL = Duration.ofDays(7); // 7일간 유지
    
    /**
     * 검색어 사용 통계 기록
     * @param keyword 검색된 키워드
     */
    public void recordSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        String normalizedKeyword = keyword.trim().toLowerCase();
        
        try {
            // 실시간 인기 검색어 스코어 증가 (ZSet 사용)
            redisTemplate.opsForZSet().incrementScore(TRENDING_KEYWORDS_KEY, normalizedKeyword, 1.0);
            
            // 일별 검색 통계 (Hash 사용)
            String dailyKey = DAILY_SEARCH_PREFIX + getTodayString();
            redisTemplate.opsForHash().increment(dailyKey, normalizedKeyword, 1);
            
            // TTL 설정
            redisTemplate.expire(TRENDING_KEYWORDS_KEY, TRENDING_TTL);
            redisTemplate.expire(dailyKey, Duration.ofDays(30)); // 일별 통계는 30일 보관
            
            // 최대 키워드 수 제한 (메모리 관리)
            limitTrendingKeywords();
            
            log.debug("검색어 통계 기록: {}", normalizedKeyword);
            
        } catch (Exception e) {
            log.error("검색어 통계 기록 실패: keyword={}, error={}", normalizedKeyword, e.getMessage());
        }
    }
    
    /**
     * 실시간 인기 검색어 조회 (캠페인 타이틀 기반 추가)
     * @param limit 조회할 키워드 수
     * @return 인기 검색어 목록 (인기순)
     */
    public List<String> getTrendingKeywords(int limit) {
        try {
            Set<String> keywords = redisTemplate.opsForZSet()
                    .reverseRange(TRENDING_KEYWORDS_KEY, 0, limit - 1);
            
            if (keywords != null && !keywords.isEmpty()) {
                List<String> result = new ArrayList<>(keywords);
                log.debug("Redis에서 인기 검색어 조회: {}개", result.size());
                return result;
            }
            
        } catch (Exception e) {
            log.error("인기 검색어 조회 실패: {}", e.getMessage());
        }
        
        // Redis 조회 실패 시 빈 리스트 반환
        log.debug("인기 검색어 데이터가 없어 빈 리스트 반환");
        return Collections.emptyList();
    }
    
    /**
     * 인기 캠페인 타이틀을 기반으로 검색어 추가
     * @param campaignTitles 인기 캠페인 타이틀 목록
     */
    public void addCampaignTitlesToTrending(List<String> campaignTitles) {
        if (campaignTitles == null || campaignTitles.isEmpty()) {
            return;
        }
        
        try {
            for (String title : campaignTitles) {
                if (title != null && !title.trim().isEmpty()) {
                    // 완전한 캠페인 타이틀을 그대로 검색어에 추가
                    String normalizedTitle = title.trim();
                    redisTemplate.opsForZSet().incrementScore(TRENDING_KEYWORDS_KEY, normalizedTitle, 0.5);
                }
            }
            
            // TTL 설정
            redisTemplate.expire(TRENDING_KEYWORDS_KEY, TRENDING_TTL);
            limitTrendingKeywords();
            
            log.debug("캠페인 타이틀 기반 검색어 추가 완료: {}개 타이틀 처리", campaignTitles.size());
            
        } catch (Exception e) {
            log.error("캠페인 타이틀 기반 검색어 추가 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 특정 키워드의 검색 횟수 조회
     * @param keyword 검색어
     * @return 검색 횟수
     */
    public Long getSearchCount(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return 0L;
        }
        
        try {
            String normalizedKeyword = keyword.trim().toLowerCase();
            Double score = redisTemplate.opsForZSet().score(TRENDING_KEYWORDS_KEY, normalizedKeyword);
            return score != null ? score.longValue() : 0L;
        } catch (Exception e) {
            log.error("검색 횟수 조회 실패: keyword={}, error={}", keyword, e.getMessage());
            return 0L;
        }
    }
    
    /**
     * 일별 검색 통계 조회
     * @param date 날짜 (yyyy-MM-dd 형식)
     * @return 해당 날짜의 검색 통계
     */
    public Map<String, String> getDailySearchStats(String date) {
        try {
            String dailyKey = DAILY_SEARCH_PREFIX + date;
            Map<Object, Object> rawEntries = redisTemplate.opsForHash().entries(dailyKey);
            
            // Object 타입을 String 타입으로 변환
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<Object, Object> entry : rawEntries.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey().toString() : "";
                String value = entry.getValue() != null ? entry.getValue().toString() : "0";
                result.put(key, value);
            }
            
            return result;
        } catch (Exception e) {
            log.error("일별 검색 통계 조회 실패: date={}, error={}", date, e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * 인기 검색어에 수동으로 키워드 추가 (관리자용)
     * @param keyword 추가할 키워드
     * @param score 점수
     */
    public void addTrendingKeyword(String keyword, double score) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        try {
            String normalizedKeyword = keyword.trim().toLowerCase();
            redisTemplate.opsForZSet().add(TRENDING_KEYWORDS_KEY, normalizedKeyword, score);
            redisTemplate.expire(TRENDING_KEYWORDS_KEY, TRENDING_TTL);
            
            log.info("인기 검색어 수동 추가: keyword={}, score={}", normalizedKeyword, score);
        } catch (Exception e) {
            log.error("인기 검색어 추가 실패: keyword={}, error={}", keyword, e.getMessage());
        }
    }
    
    /**
     * 특정 키워드를 인기 검색어에서 제거 (관리자용)
     * @param keyword 제거할 키워드
     */
    public void removeTrendingKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        try {
            String normalizedKeyword = keyword.trim().toLowerCase();
            redisTemplate.opsForZSet().remove(TRENDING_KEYWORDS_KEY, normalizedKeyword);
            
            log.info("인기 검색어 제거: {}", normalizedKeyword);
        } catch (Exception e) {
            log.error("인기 검색어 제거 실패: keyword={}, error={}", keyword, e.getMessage());
        }
    }
    
    /**
     * 인기 검색어 초기화 (관리자용)
     */
    public void clearTrendingKeywords() {
        try {
            redisTemplate.delete(TRENDING_KEYWORDS_KEY);
            log.info("인기 검색어 초기화 완료");
        } catch (Exception e) {
            log.error("인기 검색어 초기화 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 최대 키워드 수 제한 (메모리 관리)
     */
    private void limitTrendingKeywords() {
        try {
            Long count = redisTemplate.opsForZSet().count(TRENDING_KEYWORDS_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            if (count != null && count > MAX_TRENDING_KEYWORDS) {
                // 점수가 낮은 키워드들 제거
                long removeCount = count - MAX_TRENDING_KEYWORDS;
                redisTemplate.opsForZSet().removeRange(TRENDING_KEYWORDS_KEY, 0, removeCount - 1);
                
                log.debug("인기 검색어 정리: {}개 키워드 제거", removeCount);
            }
        } catch (Exception e) {
            log.error("인기 검색어 정리 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 현재 날짜를 문자열로 반환 (yyyy-MM-dd)
     */
    private String getTodayString() {
        return java.time.LocalDate.now().toString();
    }
}
