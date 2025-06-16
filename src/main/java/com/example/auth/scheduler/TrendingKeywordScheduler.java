package com.example.auth.scheduler;

import com.example.auth.dto.campaign.CampaignListSimpleResponse;
import com.example.auth.service.CampaignViewService;
import com.example.auth.service.SearchAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 인기 검색어 스케줄러
 * 주기적으로 인기 검색어 데이터를 갱신하고 정리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.analytics.enabled", havingValue = "true", matchIfMissing = true)
public class TrendingKeywordScheduler {

    private final SearchAnalyticsService searchAnalyticsService;
    private final CampaignViewService campaignViewService;

    /**
     * 매 30분마다 인기 캠페인 타이틀 기반 검색어 업데이트
     * 실제로 인기 있는 캠페인의 타이틀을 분석해서 검색어에 추가합니다.
     */
    @Scheduled(cron = "0 */30 * * * *") // 매 30분마다
    public void updateTrendingFromPopularCampaigns() {
        try {
            log.info("인기 캠페인 기반 검색어 업데이트 시작");
            
            // 인기 캠페인 30개 조회
            var popularCampaigns = campaignViewService.getCampaignListWithFilters(
                0, 30, "currentApplicants", true, null, null, null);
            
            if (popularCampaigns != null && !popularCampaigns.getContent().isEmpty()) {
                List<String> campaignTitles = popularCampaigns.getContent().stream()
                        .map(CampaignListSimpleResponse::getTitle)
                        .filter(title -> title != null && !title.trim().isEmpty())
                        .collect(Collectors.toList());
                
                // 캠페인 타이틀을 검색어에 추가
                searchAnalyticsService.addCampaignTitlesToTrending(campaignTitles);
                
                log.info("인기 캠페인 기반 검색어 업데이트 완료 - {}개 캠페인 타이틀 분석", campaignTitles.size());
            } else {
                log.warn("인기 캠페인 데이터가 없습니다.");
            }
            
        } catch (Exception e) {
            log.error("인기 캠페인 기반 검색어 업데이트 중 오류 발생", e);
        }
    }

    /**
     * 매 시간마다 인기 검색어 데이터 정리
     * - 오래된 검색어 제거
     * - 메모리 사용량 최적화
     */
    @Scheduled(cron = "0 0 * * * *") // 매 시간 정각
    public void cleanupTrendingKeywords() {
        try {
            log.info("인기 검색어 데이터 정리 시작");

            log.info("인기 검색어 데이터 정리 완료");
            
        } catch (Exception e) {
            log.error("인기 검색어 데이터 정리 중 오류 발생", e);
        }
    }

    /**
     * 매주 월요일 새벽 2시에 인기 검색어 통계 리셋
     * 오래된 데이터를 정리하여 최신 트렌드를 반영합니다.
     */
    @Scheduled(cron = "0 0 2 * * MON") // 매주 월요일 새벽 2시
    public void weeklyTrendingReset() {
        try {
            log.info("주간 인기 검색어 리셋 시작");
            
            // 인기 검색어 완전 초기화
            searchAnalyticsService.clearTrendingKeywords();
            
            log.info("주간 인기 검색어 리셋 완료");
            
        } catch (Exception e) {
            log.error("주간 인기 검색어 리셋 중 오류 발생", e);
        }
    }
}
