package com.example.auth.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.User;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.util.ImageUrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * S3에 저장된 이미지 중 현재 데이터베이스에서 사용되지 않는 이미지를 식별하고 정리하는 서비스
 * 스케줄러에 의해 자동으로 실행됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3CleanupService {

    private final AmazonS3Client amazonS3Client;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ImageUrlUtils imageUrlUtils;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * 미사용 이미지를 자동으로 찾아 정리하는 통합 메소드
     * 스케줄러에 의해 호출됩니다.
     * 
     * @param prefix S3 객체 접두사 (특정 경로만 처리하고 싶을 때)
     * @param olderThanDays 지정된 일수보다 오래된 이미지만 삭제 (기본값: 7)
     * @param dryRun true면 삭제하지 않고 삭제 대상 목록만 반환
     * @return 삭제 대상 이미지 정보 또는 삭제된 이미지 정보
     */
    public Map<String, Object> cleanupUnusedImages(String prefix, int olderThanDays, boolean dryRun) {
        log.info("미사용 이미지 정리 작업 시작 - 접두사: {}, {}일 이상 지난 이미지, 테스트 모드: {}", 
                prefix, olderThanDays, dryRun);
        
        // 1. S3에서 모든 이미지 가져오기
        List<S3ObjectSummary> allS3Objects = listAllS3Objects(prefix);
        log.info("S3에서 총 {}개의 객체를 조회했습니다", allS3Objects.size());

        // 2. 데이터베이스에서 사용 중인 모든 이미지 URL 가져오기
        Set<String> usedImageKeys = getAllUsedImageKeys();
        log.info("데이터베이스에서 사용 중인 이미지 키: {}개", usedImageKeys.size());

        // 3. 기준 날짜 계산 (현재로부터 olderThanDays일 이전)
        Instant cutoffDate = Instant.now().minus(olderThanDays, ChronoUnit.DAYS);

        // 4. 미사용 이미지 필터링 (데이터베이스에 없고, 기준일보다 오래된 이미지)
        List<Map<String, Object>> unusedImages = allS3Objects.stream()
                .filter(obj -> {
                    String objectKey = obj.getKey();
                    Date lastModified = obj.getLastModified();
                    
                    // 폴더는 제외
                    if (objectKey.endsWith("/")) {
                        return false;
                    }
                    
                    // 이미지 파일만 대상으로 하고 싶다면 확장자 체크
                    if (!isImageFile(objectKey)) {
                        return false;
                    }
                    
                    // 데이터베이스에 사용 중인지 확인
                    boolean isUsed = usedImageKeys.contains(objectKey);
                    
                    // 기준일보다 오래되었는지 확인
                    boolean isOldEnough = lastModified.toInstant().isBefore(cutoffDate);
                    
                    return !isUsed && isOldEnough;
                })
                .map(obj -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("key", obj.getKey());
                    result.put("size", obj.getSize());
                    result.put("lastModified", obj.getLastModified());
                    return result;
                })
                .collect(Collectors.toList());

        log.info("총 {}개의 미사용 이미지를 찾았습니다", unusedImages.size());
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalFound", unusedImages.size());
        result.put("unusedImages", unusedImages);
        
        // 5. dryRun이 아닌 경우 이미지 삭제
        if (!dryRun && !unusedImages.isEmpty()) {
            List<String> imagesToDelete = unusedImages.stream()
                    .map(img -> (String) img.get("key"))
                    .collect(Collectors.toList());
            
            int deletedCount = deleteUnusedImages(imagesToDelete);
            result.put("deletedCount", deletedCount);
            log.info("총 {}개의 미사용 이미지 중 {}개를 삭제했습니다", unusedImages.size(), deletedCount);
        } else {
            result.put("dryRun", true);
            log.info("테스트 모드: 삭제 없이 {}개의 미사용 이미지만 식별했습니다", unusedImages.size());
        }
        
        return result;
    }
    
    /**
     * 특정 이미지 파일 확장자인지 확인합니다.
     */
    private boolean isImageFile(String objectKey) {
        String lowerKey = objectKey.toLowerCase();
        return lowerKey.endsWith(".jpg") ||
               lowerKey.endsWith(".jpeg") ||
               lowerKey.endsWith(".png") ||
               lowerKey.endsWith(".gif") ||
               lowerKey.endsWith(".bmp") ||
               lowerKey.endsWith(".webp") ||
               lowerKey.endsWith(".avif") ||
               lowerKey.endsWith(".svg");
    }

    /**
     * S3에서 지정된 접두사를 가진 모든 객체를 가져옵니다.
     */
    private List<S3ObjectSummary> listAllS3Objects(String prefix) {
        List<S3ObjectSummary> allObjects = new ArrayList<>();
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix);

        ListObjectsV2Result result;
        do {
            result = amazonS3Client.listObjectsV2(request);
            allObjects.addAll(result.getObjectSummaries());
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return allObjects;
    }

    /**
     * 데이터베이스에서 사용 중인 모든 이미지 URL을 가져와서 S3 객체 키로 변환합니다.
     */
    @Transactional(readOnly = true)
    protected Set<String> getAllUsedImageKeys() {
        Set<String> usedImageKeys = new HashSet<>();

        // 1. 캠페인 썸네일 이미지
        campaignRepository.findAll().forEach(campaign -> {
            addImageKeyIfNotEmpty(usedImageKeys, campaign.getThumbnailUrl());
        });

        // 2. 사용자 프로필 이미지
        userRepository.findAll().forEach(user -> {
            addImageKeyIfNotEmpty(usedImageKeys, user.getProfileImg());
        });

        // 여기에 다른 이미지 URL을 포함하는 엔티티를 추가할 수 있습니다.

        return usedImageKeys;
    }

    /**
     * URL이 비어있지 않으면 S3 객체 키로 변환하여 세트에 추가합니다.
     */
    private void addImageKeyIfNotEmpty(Set<String> keys, String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String objectKey = extractObjectKeyFromUrl(imageUrl);
            if (objectKey != null && !objectKey.isEmpty()) {
                keys.add(objectKey);
                
                // 이미지 크기 변형 버전도 포함 (예: thumbnail-, medium-, large- 접두사가 붙은 버전)
                // 이 부분은 이미지 처리 로직에 따라 다를 수 있습니다.
                String baseName = getBaseNameFromObjectKey(objectKey);
                if (baseName != null && !baseName.isEmpty()) {
                    String directory = getDirectoryFromObjectKey(objectKey);
                    if (directory != null) {
                        keys.add(directory + "thumbnail-" + baseName);
                        keys.add(directory + "medium-" + baseName);
                        keys.add(directory + "large-" + baseName);
                    }
                }
            }
        }
    }

    /**
     * 객체 키에서 기본 파일명을 추출합니다.
     */
    private String getBaseNameFromObjectKey(String objectKey) {
        if (objectKey == null) return null;
        
        int lastSlashIndex = objectKey.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < objectKey.length() - 1) {
            return objectKey.substring(lastSlashIndex + 1);
        }
        return objectKey;
    }

    /**
     * 객체 키에서 디렉토리 경로를 추출합니다.
     */
    private String getDirectoryFromObjectKey(String objectKey) {
        if (objectKey == null) return null;
        
        int lastSlashIndex = objectKey.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            return objectKey.substring(0, lastSlashIndex + 1);
        }
        return "";
    }

    /**
     * URL에서 객체 키를 추출합니다.
     */
    private String extractObjectKeyFromUrl(String imageUrl) {
        try {
            if (imageUrl == null) return null;
            
            // CloudFront URL인 경우
            if (imageUrl.contains("cloudfront.net")) {
                java.net.URL url = new java.net.URL(imageUrl);
                String path = url.getPath();
                return path.startsWith("/") ? path.substring(1) : path;
            }
            
            // S3 URL인 경우
            if (imageUrl.contains(bucketName + ".s3.")) {
                java.net.URL url = new java.net.URL(imageUrl);
                String path = url.getPath();
                if (path.startsWith("/" + bucketName + "/")) {
                    return path.substring(bucketName.length() + 2);
                } else if (path.startsWith("/")) {
                    return path.substring(1);
                }
                return path;
            }
            
            // 이미 객체 키인 경우
            return imageUrl;
        } catch (Exception e) {
            log.warn("URL에서 객체 키 추출 중 오류: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 사용되지 않는 이미지를 S3에서 삭제합니다.
     * 내부적으로만 사용되는 메소드입니다.
     *
     * @param imagesToDelete 삭제할 이미지 객체 키 목록
     * @return 삭제된 이미지 수
     */
    private int deleteUnusedImages(List<String> imagesToDelete) {
        if (imagesToDelete == null || imagesToDelete.isEmpty()) {
            log.info("삭제할 이미지가 없습니다.");
            return 0;
        }

        log.info("{}개의 미사용 이미지 삭제 시작", imagesToDelete.size());
        
        int deletedCount = 0;
        for (String objectKey : imagesToDelete) {
            try {
                amazonS3Client.deleteObject(bucketName, objectKey);
                log.info("이미지 삭제 성공: {}", objectKey);
                deletedCount++;
            } catch (Exception e) {
                log.error("이미지 삭제 중 오류 발생: {}, 오류: {}", objectKey, e.getMessage(), e);
            }
        }

        log.info("총 {}개의 미사용 이미지를 삭제했습니다", deletedCount);
        return deletedCount;
    }
}
