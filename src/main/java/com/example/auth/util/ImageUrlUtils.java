package com.example.auth.util;

import com.example.auth.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 이미지 URL 변환 유틸리티
 * S3 URL과 CloudFront URL 간의 변환을 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageUrlUtils {

    private final S3Service s3Service;
    
    @Value("${aws.cloudfront.enabled:false}")
    private boolean cloudfrontEnabled;
    
    @Value("${aws.cloudfront.domain:}")
    private String cloudfrontDomain;
    
    /**
     * 이미지 URL을 CloudFront URL로 변환합니다.
     * CloudFront가 활성화된 경우에만 변환이 이루어집니다.
     *
     * @param imageUrl 원본 이미지 URL (S3 URL 또는 객체 키)
     * @return CloudFront URL 또는 원본 URL(CloudFront 비활성화 시)
     */
    public String convertToCloudFrontUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return imageUrl;
        }
        
        // 이미 CloudFront URL인 경우 변환하지 않음
        if (isCloudFrontUrl(imageUrl)) {
            return imageUrl;
        }
        
        // S3Service의 getImageUrl 메서드를 통해 변환
        return s3Service.getImageUrl(imageUrl);
    }
    
    /**
     * 해당 URL이 CloudFront URL인지 확인합니다.
     *
     * @param url 확인할 URL
     * @return CloudFront URL 여부
     */
    public boolean isCloudFrontUrl(String url) {
        if (!cloudfrontEnabled || cloudfrontDomain == null || cloudfrontDomain.isEmpty()) {
            return false;
        }
        
        return url != null && url.contains(cloudfrontDomain);
    }
}
