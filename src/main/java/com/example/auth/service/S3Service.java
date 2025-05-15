package com.example.auth.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3Client amazonS3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.presigned-url.expiration}")
    private int presignedUrlExpirationSeconds;
    
    @Value("${aws.cloudfront.domain:}")
    private String cloudfrontDomain;
    
    @Value("${aws.cloudfront.enabled:false}")
    private boolean cloudfrontEnabled;

    /**
     * 원본 이미지 업로드를 위한 presigned URL을 생성합니다.
     *
     * @param fileExtension 파일 확장자 (예: jpg, png)
     * @return 업로드용 presigned URL 문자열
     */
    public String generatePresignedUrl(String fileExtension) {
        return generatePresignedUrlWithOptions(fileExtension, null, null, null);
    }
    
    /**
     * 프로필 이미지 업로드를 위한 presigned URL을 생성합니다. (내부 메소드)
     * 프로필 이미지는 480px x 480px 크기로 리사이징됩니다.
     *
     * @param fileExtension 파일 확장자 (예: jpg, png)
     * @return 업로드용 presigned URL 문자열
     */
    public String generateProfileImagePresignedUrl(String fileExtension) {
        return generatePresignedUrlWithOptions(fileExtension, 480, 480, "85");
    }
    
    /**
     * 커버 이미지 업로드를 위한 presigned URL을 생성합니다. (내부 메소드)
     * 커버 이미지는 720px 너비로 리사이징됩니다. (높이는 비율에 맞게 자동 조정)
     *
     * @param fileExtension 파일 확장자 (예: jpg, png)
     * @return 업로드용 presigned URL 문자열
     */
    public String generateCoverImagePresignedUrl(String fileExtension) {
        return generatePresignedUrlWithOptions(fileExtension, 720, null, "85");
    }
    
    /**
     * 이미지 업로드를 위한 presigned URL을 생성합니다.
     * 이미지 최적화 옵션 지원 버전
     *
     * @param fileExtension 파일 확장자 (예: jpg, png)
     * @param width 이미지 너비 (null이면 원본 크기 유지)
     * @param height 이미지 높이 (null이면 원본 크기 유지 또는 너비에 맞춰 자동 조정)
     * @param quality 이미지 품질 (null이면 기본값 "90" 사용)
     * @return 업로드용 presigned URL 문자열
     */
    public String generatePresignedUrlWithOptions(String fileExtension, Integer width, Integer height, String quality) {
        // 기본 이미지 품질 설정
        if (quality == null) {
            quality = "90";
        }
        
        // 고유한 키 생성
        String uuid = UUID.randomUUID().toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        // 경로 및 파일명 생성
        String directory;
        String filename;
        
        if (width != null || height != null) {
            // 리사이징이 요청된 경우 별도 경로 사용
            if (width != null && height != null && width.equals(height) && width == 480) {
                // 480x480 이미지는 프로필 이미지로 간주
                directory = "profile-images";
                filename = String.format("%s-%s-%dx%d", timestamp, uuid, width, height);
            } else if (width != null && width == 720) {
                // 720px 너비는 커버 이미지로 간주
                directory = "cover-images";
                filename = String.format("%s-%s-%dw", timestamp, uuid, width);
            } else {
                // 기타 리사이징 이미지 - 일반적으로 사용하지 않음
                directory = "resized-images";
                if (width != null && height != null) {
                    filename = String.format("%s-%s-%dx%d", timestamp, uuid, width, height);
                } else if (width != null) {
                    filename = String.format("%s-%s-%dw", timestamp, uuid, width);
                } else {
                    filename = String.format("%s-%s-%dh", timestamp, uuid, height);
                }
            }
        } else {
            // 원본 이미지
            directory = "original-images";
            filename = String.format("%s-%s", timestamp, uuid);
        }
        
        // 최종 객체 키 생성
        String objectKey = String.format("%s/%s.%s", directory, filename, fileExtension);

        try {
            log.info("이미지 Presigned URL 생성 - 경로: {}, 크기: {}x{}, 품질: {}", 
                    directory, width, height, quality);
            
            // S3 Presigned URL 요청 생성
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey)
                    .withMethod(HttpMethod.PUT)
                    .withExpiration(new Date(System.currentTimeMillis() + (presignedUrlExpirationSeconds * 1000)));

            // 콘텐츠 타입 설정
            generatePresignedUrlRequest.setContentType(determineContentType(fileExtension));
            
            // 캐싱 헤더 추가 (1년 캐싱)
            generatePresignedUrlRequest.addRequestParameter(
                    Headers.CACHE_CONTROL,
                    "max-age=31536000, immutable");
            
            // 리사이징 헤더 추가 (필요한 경우)
            if (width != null || height != null) {
                // x-amz-meta-resize-dimensions 헤더에 크기 정보 추가
                String dimensions = "";
                if (width != null && height != null) {
                    dimensions = width + "x" + height;
                } else if (width != null) {
                    dimensions = width + "xAUTO";
                } else if (height != null) {
                    dimensions = "AUTOx" + height;
                }
                
                generatePresignedUrlRequest.addRequestParameter(
                        "x-amz-meta-resize-dimensions", 
                        dimensions);
                
                // 이미지 품질 설정
                generatePresignedUrlRequest.addRequestParameter(
                        "x-amz-meta-image-quality", 
                        quality);
            }

            // Presigned URL 생성 및 반환
            String presignedUrl = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();

            log.info("Presigned URL 생성 완료 - 객체 키: {}", objectKey);

            return presignedUrl;
        } catch (Exception e) {
            log.error("Presigned URL 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("Presigned URL 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 확장자로 콘텐츠 타입을 결정합니다.
     */
    private String determineContentType(String fileExtension) {
        if (fileExtension == null) {
            return "application/octet-stream";
        }

        return switch (fileExtension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }
    
    /**
     * 이미지 URL을 생성합니다.
     * CloudFront가 활성화된 경우 CloudFront URL을 사용하고,
     * 그렇지 않은 경우 S3 URL을 사용합니다.
     *
     * @param objectKeyOrUrl S3 객체 키 또는 S3 URL
     * @return 이미지 URL
     */
    public String getImageUrl(String objectKeyOrUrl) {
        // URL에서 객체 키 추출 (S3 URL이 입력된 경우)
        String objectKey = extractObjectKeyFromUrl(objectKeyOrUrl);
        
        log.info("getImageUrl 호출됨 - 입력값: {}, 추출된 객체 키: {}", objectKeyOrUrl, objectKey);
        log.info("CloudFront 설정 - enabled: {}, domain: {}", cloudfrontEnabled, cloudfrontDomain);
        
        String resultUrl;
        if (cloudfrontEnabled && cloudfrontDomain != null && !cloudfrontDomain.isEmpty()) {
            resultUrl = "https://" + cloudfrontDomain + "/" + objectKey;
            log.info("CloudFront URL 생성됨: {}", resultUrl);
        } else {
            resultUrl = amazonS3Client.getUrl(bucketName, objectKey).toString();
            log.info("S3 URL 생성됨: {}", resultUrl);
        }
        
        return resultUrl;
    }
    
    /**
     * URL에서 객체 키를 추출합니다.
     * URL 형식이 아니면 그대로 반환합니다.
     *
     * @param urlOrKey S3 URL 또는 객체 키
     * @return 추출된 객체 키
     */
    private String extractObjectKeyFromUrl(String urlOrKey) {
        // null이거나 빈 문자열이면 그대로 반환
        if (urlOrKey == null || urlOrKey.isEmpty()) {
            return urlOrKey;
        }
        
        // URL 형식인지 확인
        if (urlOrKey.startsWith("http://") || urlOrKey.startsWith("https://")) {
            try {
                java.net.URL url = new java.net.URL(urlOrKey);
                String path = url.getPath();
                
                // 버킷 이름이 경로에 포함된 경우 처리 (일부 S3 URL 형식)
                if (path.contains(bucketName)) {
                    int bucketEndIndex = path.indexOf(bucketName) + bucketName.length();
                    if (bucketEndIndex < path.length()) {
                        path = path.substring(bucketEndIndex);
                    }
                }
                
                // 첫 번째 '/'를 제거
                return path.startsWith("/") ? path.substring(1) : path;
            } catch (Exception e) {
                log.warn("URL 파싱 중 오류 발생, 원본 값 반환: {}", e.getMessage());
                return urlOrKey;
            }
        }
        
        // URL 형식이 아니면 그대로 반환 (이미 객체 키인 경우)
        return urlOrKey;
    }

    /**
     * S3에서 이미지를 삭제합니다.
     *
     * @param objectKey 삭제할 객체의 키
     */
    @Async
    public void deleteImage(String objectKey) {
        try {
            amazonS3Client.deleteObject(bucketName, objectKey);
            log.info("이미지가 성공적으로 삭제되었습니다: {}", objectKey);
        } catch (Exception e) {
            log.error("이미지 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("이미지 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }
}