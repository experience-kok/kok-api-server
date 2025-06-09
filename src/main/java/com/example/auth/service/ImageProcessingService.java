package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.User;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미지 처리 완료 후 URL 업데이트를 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final S3Service s3Service;

    /**
     * Lambda 처리 완료 후 사용자 프로필 이미지 URL을 리사이징된 URL로 업데이트
     * @param userId 사용자 ID
     * @param originalImageUrl 원본 이미지 URL
     */
    @Async
    public void updateUserProfileImageWhenReady(Long userId, String originalImageUrl) {
        try {
            // 리사이징 기능이 비활성화되어 있거나 Lambda가 없는 경우 즉시 원본 URL로 업데이트
            String finalUrl = cleanPresignedUrl(originalImageUrl);
            String cloudFrontUrl = s3Service.getImageUrl(finalUrl);

            // 즉시 원본 CloudFront URL로 업데이트 (리사이징 없이)
            updateUserProfileImageUrl(userId, cloudFrontUrl);

            log.info("사용자 프로필 이미지 업데이트 완료 (리사이징 대기 없음): userId={}, url={}", userId, cloudFrontUrl);

        } catch (Exception e) {
            log.error("프로필 이미지 비동기 업데이트 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * Lambda 처리 완료 후 캠페인 썸네일 이미지 URL을 리사이징된 URL로 업데이트
     * @param campaignId 캠페인 ID
     * @param originalImageUrl 원본 이미지 URL
     */
    @Async
    public void updateCampaignThumbnailWhenReady(Long campaignId, String originalImageUrl) {
        try {
            // 리사이징 기능이 비활성화되어 있거나 Lambda가 없는 경우 즉시 원본 URL로 업데이트
            String finalUrl = cleanPresignedUrl(originalImageUrl);
            String cloudFrontUrl = s3Service.getImageUrl(finalUrl);

            // 즉시 원본 CloudFront URL로 업데이트 (리사이징 없이)
            updateCampaignThumbnailUrl(campaignId, cloudFrontUrl);

            log.info("캠페인 썸네일 이미지 업데이트 완료 (리사이징 대기 없음): campaignId={}, url={}", campaignId, cloudFrontUrl);

        } catch (Exception e) {
            log.error("캠페인 썸네일 비동기 업데이트 실패: campaignId={}, error={}", campaignId, e.getMessage(), e);
        }
    }

    /**
     * 사용자 프로필 이미지 URL 업데이트 (짧은 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateUserProfileImageUrl(Long userId, String imageUrl) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.updateProfileImg(imageUrl);
                userRepository.save(user);
                log.info("사용자 프로필 이미지 URL 업데이트 완료: userId={}, url={}", userId, imageUrl);
            } else {
                log.warn("사용자를 찾을 수 없습니다: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("사용자 프로필 이미지 URL 업데이트 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 캠페인 썸네일 이미지 URL 업데이트 (짧은 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCampaignThumbnailUrl(Long campaignId, String imageUrl) {
        try {
            Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign != null) {
                campaign.setThumbnailUrl(imageUrl);
                campaignRepository.save(campaign);
                log.info("캠페인 썸네일 이미지 URL 업데이트 완료: campaignId={}, url={}", campaignId, imageUrl);
            } else {
                log.warn("캠페인을 찾을 수 없습니다: campaignId={}", campaignId);
            }
        } catch (Exception e) {
            log.error("캠페인 썸네일 이미지 URL 업데이트 실패: campaignId={}, error={}", campaignId, e.getMessage(), e);
        }
    }

    /**
     * Presigned URL에서 쿼리 파라미터 제거
     */
    private String cleanPresignedUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            return url.substring(0, queryIndex);
        }

        return url;
    }

    /**
     * 리사이징이 필요한 경우를 위한 대기 로직 (현재는 사용하지 않음)
     * Lambda 리사이징이 구현되면 다시 활성화할 수 있습니다.
     */
    @Deprecated
    private String waitForResizedImageWithRetry(String originalImageUrl, int timeoutSeconds) {
        String cleanUrl = cleanPresignedUrl(originalImageUrl);
        String originalCloudFrontUrl = s3Service.getImageUrl(cleanUrl);

        log.info("리사이징 대기 시작 - 원본 URL: {}", cleanUrl);

        // 최대 3번까지만 확인 (60초가 아닌 3초)
        for (int i = 0; i < Math.min(timeoutSeconds, 3); i++) {
            try {
                String resizedUrl = s3Service.getResizedImageUrl(cleanUrl);

                // 리사이징된 URL이 원본과 다른지 확인
                if (!resizedUrl.equals(originalCloudFrontUrl)) {
                    log.info("리사이징 완료 감지: 원본={}, 리사이징={}", originalCloudFrontUrl, resizedUrl);
                    return resizedUrl;
                }

                log.debug("리사이징 대기 중... {}초 경과", i + 1);
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("리사이징 대기 중 인터럽트 발생: {}", originalImageUrl);
                break;
            } catch (Exception e) {
                log.warn("리사이징 이미지 확인 중 오류: {}", e.getMessage());
                break;
            }
        }

        log.info("리사이징 대기 시간 초과 - 원본 URL 사용: {}", cleanUrl);
        return originalCloudFrontUrl; // 원본 CloudFront URL 반환
    }
}