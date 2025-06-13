package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoCompleteService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CampaignRepository campaignRepository;
    
    private static final String CAMPAIGN_TITLES_KEY = "campaign:titles";

    /**
     * 자동완성 제안 조회 (캠페인 제목 검색)
     */
    public List<String> getSuggestions(String prefix, int limit) {
        log.debug("자동완성 조회 요청 - prefix: {}, limit: {}", prefix, limit);
        
        if (prefix == null || prefix.trim().isEmpty()) {
            return List.of();
        }
        
        String normalizedPrefix = prefix.toLowerCase().trim();
        
        // Redis에서 캠페인 제목들 조회
        Set<String> allTitles = redisTemplate.opsForSet().members(CAMPAIGN_TITLES_KEY);
        
        if (allTitles == null || allTitles.isEmpty()) {
            log.warn("Redis에 캐시된 캠페인 제목이 없습니다. 즉시 갱신을 시도합니다.");
            refreshCampaignData();
            allTitles = redisTemplate.opsForSet().members(CAMPAIGN_TITLES_KEY);
        }
        
        // null 체크
        if (allTitles == null) {
            return List.of();
        }
        
        // 제목에서 키워드 포함 검색
        List<String> suggestions = allTitles.stream()
                .filter(title -> title != null)
                .filter(title -> title.toLowerCase().contains(normalizedPrefix))
                .sorted((a, b) -> {
                    // 정확히 시작하는 것을 우선순위로
                    boolean aStarts = a.toLowerCase().startsWith(normalizedPrefix);
                    boolean bStarts = b.toLowerCase().startsWith(normalizedPrefix);
                    
                    if (aStarts && !bStarts) return -1;
                    if (!aStarts && bStarts) return 1;
                    
                    // 길이가 짧은 것을 우선순위로
                    return Integer.compare(a.length(), b.length());
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        log.debug("자동완성 결과 - {}개 제안", suggestions.size());
        return suggestions;
    }

    /**
     * 캠페인 데이터 캐시 갱신 (10분마다 실행)
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void refreshCampaignData() {
        try {
            log.info("캠페인 제목 캐시 갱신 시작");
            
            // DB에서 캠페인 제목만 조회 (성능 최적화)
            List<String> campaignTitles = campaignRepository.findAllTitles();
            
            log.info("캐시할 캠페인 제목 수: {}", campaignTitles.size());
            
            // Redis 캐시 갱신 (제목만 저장)
            updateRedisCache(campaignTitles);
            
            log.info("캠페인 제목 캐시 갱신 완료 - {}개 제목", campaignTitles.size());
            
        } catch (Exception e) {
            log.error("캠페인 제목 캐시 갱신 중 오류 발생", e);
        }
    }

    /**
     * Redis 캐시 업데이트 (제목만 저장)
     */
    private void updateRedisCache(List<String> titles) {
        // 기존 캐시 삭제
        redisTemplate.delete(CAMPAIGN_TITLES_KEY);
        
        // 새 제목 데이터 저장
        if (!titles.isEmpty()) {
            redisTemplate.opsForSet().add(CAMPAIGN_TITLES_KEY, titles.toArray(new String[0]));
        }
    }
}


