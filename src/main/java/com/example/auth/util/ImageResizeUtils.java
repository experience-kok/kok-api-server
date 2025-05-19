package com.example.auth.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 이미지 리사이징 관련 유틸리티 클래스
 * Lambda 함수로 리사이징된 이미지 URL을 생성합니다.
 */
@Component
public class ImageResizeUtils {

    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Value("${aws.cloudfront.domain:}")
    private String cloudfrontDomain;
    
    @Value("${aws.cloudfront.enabled:false}")
    private boolean cloudfrontEnabled;
    
    @Value("${aws.s3.resize.prefix:resized}")
    private String resizePrefix;
    
    /**
     * 원본 이미지 URL 또는 키에서 리사이징된 이미지 URL을 생성합니다.
     * 
     * @param originalUrlOrKey 원본 이미지 URL 또는 객체 키
     * @param size 리사이징 크기 (예: 480, 720)
     * @return 리사이징된 이미지 URL
     */
    public String getResizedImageUrl(String originalUrlOrKey, int size) {
        if (originalUrlOrKey == null || originalUrlOrKey.isEmpty()) {
            return originalUrlOrKey;
        }
        
        // 객체 키 추출
        String objectKey = extractObjectKey(originalUrlOrKey);
        if (objectKey == null) {
            return originalUrlOrKey;
        }
        
        // 파일명 추출
        String filename = extractFilename(objectKey);
        if (filename == null) {
            return originalUrlOrKey;
        }
        
        // 리사이징된 이미지 키 생성
        String resizedKey = String.format("%s/%dx%d/%s", resizePrefix, size, size, filename);
        
        // CloudFront 사용 시 CloudFront URL 반환
        if (cloudfrontEnabled && cloudfrontDomain != null && !cloudfrontDomain.isEmpty()) {
            return String.format("https://%s/%s", cloudfrontDomain, resizedKey);
        }
        
        // S3 URL 반환
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, resizedKey);
    }
    
    /**
     * URL 또는 경로에서 객체 키를 추출합니다.
     */
    private String extractObjectKey(String urlOrKey) {
        // 이미 객체 키인 경우
        if (!urlOrKey.startsWith("http")) {
            return urlOrKey;
        }
        
        // URL에서 경로 부분 추출
        try {
            java.net.URL url = new java.net.URL(urlOrKey);
            String path = url.getPath();
            
            // 첫 번째 '/'를 제거
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 객체 키에서 파일명을 추출합니다.
     */
    private String extractFilename(String objectKey) {
        int lastSlashIndex = objectKey.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < objectKey.length() - 1) {
            return objectKey.substring(lastSlashIndex + 1);
        }
        return objectKey;
    }
}
