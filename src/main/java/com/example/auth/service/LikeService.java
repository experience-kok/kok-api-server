package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.Like;
import com.example.auth.domain.User;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.dto.like.LikeResponse;
import com.example.auth.dto.like.LikeStatusResponse;
import com.example.auth.dto.like.LikeUserResponse;
import com.example.auth.dto.like.MyLikedCampaignResponse;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.LikeRepository;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 캠페인 좋아요 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

    /**
     * 캠페인 좋아요 토글 (좋아요/취소)
     */
    @Transactional
    public LikeResponse toggleCampaignLike(Long userId, Long campaignId) {
        log.info("캠페인 좋아요 토글 요청: userId={}, campaignId={}", userId, campaignId);

        // 캠페인 존재 여부 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        // 기존 좋아요 확인
        Optional<Like> existingLike = likeRepository.findByUserIdAndCampaignId(userId, campaignId);

        boolean isLiked;
        if (existingLike.isPresent()) {
            // 좋아요 취소
            likeRepository.delete(existingLike.get());
            isLiked = false;
            log.info("캠페인 좋아요 취소: userId={}, campaignId={}", userId, campaignId);
        } else {
            // 좋아요 추가
            Like newLike = Like.builder()
                    .userId(userId)
                    .campaignId(campaignId)
                    .createdAt(LocalDateTime.now())
                    .build();
            likeRepository.save(newLike);
            isLiked = true;
            log.info("캠페인 좋아요 추가: userId={}, campaignId={}", userId, campaignId);
        }

        // 현재 좋아요 수 계산
        long totalCount = likeRepository.countByCampaignId(campaignId);

        return LikeResponse.builder()
                .liked(isLiked)
                .totalCount(totalCount)
                .campaignId(campaignId)
                .build();
    }

    /**
     * 캠페인 좋아요 상태 조회
     */
    public LikeStatusResponse getCampaignLikeStatus(Long campaignId, Long userId) {
        log.info("캠페인 좋아요 상태 조회: campaignId={}, userId={}", campaignId, userId);

        // 캠페인 존재 여부 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        // 좋아요 수 조회
        long totalCount = likeRepository.countByCampaignId(campaignId);

        // 사용자 좋아요 여부 확인 (로그인한 경우만)
        boolean isLiked = userId != null && likeRepository.existsByUserIdAndCampaignId(userId, campaignId);

        // 좋아요 가능 여부 (로그인 여부)
        boolean canLike = userId != null;

        return LikeStatusResponse.builder()
                .liked(isLiked)
                .totalCount(totalCount)
                .campaignId(campaignId)
                .canLike(canLike)
                .build();
    }

    /**
     * 내가 좋아요한 캠페인 목록 조회
     */
    public PageResponse<MyLikedCampaignResponse> getMyLikedCampaigns(Long userId, int page, int size) {
        log.info("내가 좋아요한 캠페인 목록 조회: userId={}, page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Like> likePage = likeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // 캠페인 ID 목록 추출
        List<Long> campaignIds = likePage.getContent().stream()
                .map(Like::getCampaignId)
                .collect(Collectors.toList());

        if (campaignIds.isEmpty()) {
            return PageResponse.<MyLikedCampaignResponse>builder()
                    .content(List.of())
                    .pageNumber(likePage.getNumber() + 1)
                    .pageSize(likePage.getSize())
                    .totalPages(likePage.getTotalPages())
                    .totalElements(likePage.getTotalElements())
                    .first(likePage.isFirst())
                    .last(likePage.isLast())
                    .build();
        }

        // 캠페인 정보 조회
        List<Campaign> campaigns = campaignRepository.findAllById(campaignIds);
        Map<Long, Campaign> campaignMap = campaigns.stream()
                .collect(Collectors.toMap(Campaign::getId, campaign -> campaign));

        // 각 캠페인의 좋아요 수 조회
        Map<Long, Long> likeCountMap = getLikeCountMap(campaignIds);

        // 응답 DTO 생성 (null 제거)
        List<MyLikedCampaignResponse> responses = likePage.getContent().stream()
                .map(like -> {
                    Campaign campaign = campaignMap.get(like.getCampaignId());
                    if (campaign == null) {
                        return null; // 삭제된 캠페인
                    }
                    long likeCount = likeCountMap.getOrDefault(campaign.getId(), 0L);
                    return MyLikedCampaignResponse.fromCampaign(campaign, likeCount, like.getCreatedAt());
                })
                .filter(Objects::nonNull) // null 제거
                .collect(Collectors.toList());

        return PageResponse.<MyLikedCampaignResponse>builder()
                .content(responses)
                .pageNumber(likePage.getNumber() + 1)
                .pageSize(likePage.getSize())
                .totalPages(likePage.getTotalPages())
                .totalElements(likePage.getTotalElements())
                .first(likePage.isFirst())
                .last(likePage.isLast())
                .build();
    }

    /**
     * 캠페인을 좋아요한 사용자 목록 조회
     */
    public PageResponse<LikeUserResponse> getCampaignLikeUsers(Long campaignId, int page, int size) {
        log.info("캠페인 좋아요 사용자 목록 조회: campaignId={}, page={}, size={}", campaignId, page, size);

        // 캠페인 존재 여부 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Like> likePage = likeRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId, pageable);

        // 사용자 ID 목록 추출
        List<Long> userIds = likePage.getContent().stream()
                .map(Like::getUserId)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return PageResponse.<LikeUserResponse>builder()
                    .content(List.of())
                    .pageNumber(likePage.getNumber() + 1)
                    .pageSize(likePage.getSize())
                    .totalPages(likePage.getTotalPages())
                    .totalElements(likePage.getTotalElements())
                    .first(likePage.isFirst())
                    .last(likePage.isLast())
                    .build();
        }

        // 사용자 정보 조회
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 응답 DTO 생성 (null 제거)
        List<LikeUserResponse> responses = likePage.getContent().stream()
                .map(like -> {
                    User user = userMap.get(like.getUserId());
                    if (user == null) {
                        return null; // 삭제된 사용자
                    }
                    return LikeUserResponse.fromUser(user, like.getCreatedAt());
                })
                .filter(Objects::nonNull) // null 제거
                .collect(Collectors.toList());

        return PageResponse.<LikeUserResponse>builder()
                .content(responses)
                .pageNumber(likePage.getNumber() + 1)
                .pageSize(likePage.getSize())
                .totalPages(likePage.getTotalPages())
                .totalElements(likePage.getTotalElements())
                .first(likePage.isFirst())
                .last(likePage.isLast())
                .build();
    }

    /**
     * 여러 캠페인의 좋아요 수를 한번에 조회 (성능 최적화)
     */
    public Map<Long, Long> getLikeCountMap(List<Long> campaignIds) {
        if (campaignIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = likeRepository.countLikesByCampaignIds(campaignIds);
        
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],      // campaignId
                        row -> (Long) row[1]       // count
                ));
    }

    /**
     * 특정 사용자가 여러 캠페인에 좋아요 했는지 확인 (성능 최적화)
     */
    public Map<Long, Boolean> getUserLikeMap(Long userId, List<Long> campaignIds) {
        if (campaignIds.isEmpty() || userId == null) {
            return new HashMap<>();
        }

        List<Long> likedCampaignIds = likeRepository.findLikedCampaignIdsByUserId(userId, campaignIds);
        
        return campaignIds.stream()
                .collect(Collectors.toMap(
                        campaignId -> campaignId,
                        campaignId -> likedCampaignIds.contains(campaignId)
                ));
    }

    /**
     * 단일 캠페인의 좋아요 수 조회
     */
    public long getCampaignLikeCount(Long campaignId) {
        return likeRepository.countByCampaignId(campaignId);
    }

    /**
     * 사용자가 특정 캠페인을 좋아요 했는지 확인
     */
    public boolean isUserLikedCampaign(Long userId, Long campaignId) {
        return userId != null && likeRepository.existsByUserIdAndCampaignId(userId, campaignId);
    }
}
