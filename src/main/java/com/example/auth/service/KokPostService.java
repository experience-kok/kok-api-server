package com.example.auth.service;

import com.example.auth.constant.SortOption;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.KokPost;
import com.example.auth.dto.KokPostListResponse;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.KokPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
