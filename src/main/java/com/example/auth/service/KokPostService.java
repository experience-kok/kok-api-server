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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KokPostService {

    private final KokPostRepository kokPostRepository;
    private final CampaignRepository campaignRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String VIEW_COUNT_KEY_PREFIX = "view_count:";
    private static final String DAILY_VIEW_COUNT_KEY_PREFIX = "daily_view_count:";
    private static final int VIEW_COUNT_EXPIRE_HOURS = 24;

    /**
     * 조회수 증가 처리 (Redis 캐시만 업데이트)
     */
    @Transactional
    private void processViewCountIncrease(Long postId, String clientIP) {
        String duplicateCheckKey = VIEW_COUNT_KEY_PREFIX + postId + ":" + clientIP;
        
        // 24시간 내 중복 조회 체크
        if (!redisTemplate.hasKey(duplicateCheckKey)) {
            // 1. 중복 방지용 키 생성 (24시간 TTL)
            redisTemplate.opsForValue().set(duplicateCheckKey, "viewed", Duration.ofHours(VIEW_COUNT_EXPIRE_HOURS));
            
            // 2. Redis에 일일 조회수 누적 (DB 업데이트 전까지 임시 저장)
            String dailyCountKey = DAILY_VIEW_COUNT_KEY_PREFIX + postId;
            redisTemplate.opsForValue().increment(dailyCountKey, 1);
            
            log.info("Redis 조회수 증가 - postId: {}, clientIP: {}", postId, clientIP);
        } else {
            log.debug("24시간 내 중복 조회 차단 - postId: {}, clientIP: {}", postId, clientIP);
        }
    }

    /**
     * Redis에서 현재 누적된 조회수 조회
     */
    public Long getRedisViewCount(Long postId) {
        String dailyCountKey = DAILY_VIEW_COUNT_KEY_PREFIX + postId;
        String count = redisTemplate.opsForValue().get(dailyCountKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    /**
     * 실제 조회수 반환 (DB 조회수 + Redis 누적 조회수)
     */
    public Long getTotalViewCount(Long postId) {
        KokPost kokPost = kokPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("포스트를 찾을 수 없습니다."));
        
        Long dbViewCount = kokPost.getViewCount();
        Long redisViewCount = getRedisViewCount(postId);
        
        return dbViewCount + redisViewCount;
    }

    /**
     * 콕포스트 전체 목록 조회 (정렬 옵션 포함)
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
     * 콕포스트 전체 목록 조회 (기본 정렬: 최신순)
     */

    /**
     * 캠페인 ID로 단일 콕포스트 간단 조회 (List용)
     */
    public KokPostListResponse getKokPostSummaryByCampaignId(Long campaignId) {
        log.info("캠페인 ID로 콕포스트 조회 요청 - campaignId: {}", campaignId);

        KokPost kokPost = kokPostRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 캠페인의 체험콕 아티클을 찾을 수 없습니다."));

        // 캠페인 상태 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElse(null);
        
        boolean isCampaignOpen = campaign != null && campaign.isRecruitmentOpen();

        log.info("캠페인 콕포스트 조회 완료 - campaignId: {}, postId: {}", campaignId, kokPost.getId());

        return KokPostListResponse.builder()
                .id(kokPost.getId())
                .title(kokPost.getTitle())
                .viewCount(kokPost.getViewCount())
                .campaignId(kokPost.getCampaignId())
                .authorId(kokPost.getAuthorId())
                .authorName(kokPost.getAuthorName())
                .contactPhone(kokPost.getVisitInfo() != null ? kokPost.getVisitInfo().getContactPhone() : null)
                .businessAddress(kokPost.getVisitInfo() != null ? kokPost.getVisitInfo().getBusinessAddress() : null)
                .isCampaignOpen(isCampaignOpen)
                .createdAt(kokPost.getCreatedAt())
                .updatedAt(kokPost.getUpdatedAt())
                .build();
    }

    /**
     * 캠페인별 콕포스트 상세 조회 (단일 조회)
     */
    public KokPostDetailResponse getKokPostDetailByCampaignId(Long campaignId, String clientIP) {
        log.info("캠페인별 콕포스트 상세 조회 요청 - campaignId: {}", campaignId);

        // 캠페인 ID로 체험콕 글 조회
        KokPost kokPost = kokPostRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 캠페인의 체험콕 글을 찾을 수 없습니다."));

        // 조회수 증가 처리 (Redis에만)
        processViewCountIncrease(kokPost.getId(), clientIP);

        // 실시간 총 조회수 계산 (DB + Redis)
        Long totalViewCount = getTotalViewCount(kokPost.getId());

        // 캠페인 모집 상태 확인
        Boolean isCampaignOpen = campaignRepository.findById(campaignId)
                .map(Campaign::isRecruitmentOpen)
                .orElse(false);

        log.info("캠페인별 콕포스트 상세 조회 완료 - campaignId: {}, kokPostId: {}", campaignId, kokPost.getId());

        return KokPostDetailResponse.fromEntityWithViewCount(kokPost, isCampaignOpen, totalViewCount);
    }

    /**
     * 캠페인별 콕포스트 조회 (정렬 옵션 포함)
     */
    public List<KokPostListResponse> getKokPostsByCampaignId(Long campaignId, SortOption sortOption) {
        log.info("캠페인별 콕포스트 조회 요청 - campaignId: {}, 정렬: {}", campaignId, sortOption.getDescription());

        List<KokPost> kokPosts;
        
        switch (sortOption) {
            default -> kokPosts = kokPostRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId);
        }
        
        log.info("캠페인별 콕포스트 조회 완료 - campaignId: {}, 정렬: {}, 총 {}개", 
                campaignId, sortOption.getDescription(), kokPosts.size());
        
        return convertToResponseWithCampaignStatus(kokPosts);
    }

    /**
     * 제목으로 콕포스트 검색 (정렬 옵션 포함)
     */
    public List<KokPostListResponse> searchKokPostsByTitle(String title, SortOption sortOption) {
        log.info("콕포스트 제목 검색 요청 - 키워드: {}, 정렬: {}", title, sortOption.getDescription());

        List<KokPost> kokPosts = kokPostRepository.findByTitleContainingIgnoreCase(title, sortOption.getSort());

        log.info("콕포스트 검색 완료 - 키워드: {}, 정렬: {}, 결과: {}개",
                title, sortOption.getDescription(), kokPosts.size());

        return convertToResponseWithCampaignStatus(kokPosts);
    }



    /**
     * KokPost 리스트를 KokPostListResponse로 변환하면서 캠페인 모집 상태 확인
     */
    private List<KokPostListResponse> convertToResponseWithCampaignStatus(List<KokPost> kokPosts) {
        if (kokPosts.isEmpty()) {
            return List.of();
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

        // 응답 객체 생성하면서 캠페인 모집 상태 설정
        return kokPosts.stream()
                .map(kokPost -> {
                    Boolean isCampaignOpen = campaignOpenStatusMap.getOrDefault(kokPost.getCampaignId(), false);

                    return KokPostListResponse.builder()
                            .id(kokPost.getId())
                            .title(kokPost.getTitle())
                            .viewCount(kokPost.getViewCount())
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
