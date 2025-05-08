package com.example.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.presigned-url.expiration}")
    private int presignedUrlExpiration;

    /**
     * 이미지 업로드를 위한 presigned URL을 생성합니다.
     * 
     * @param fileExtension 파일 확장자 (예: jpg, png)
     * @return PresignedUrlResponse 객체 (URL과 키 정보 포함)
     */
    public PresignedUrlResponse generatePresignedUrl(String fileExtension) {
        // 고유한 키 생성
        String uuid = UUID.randomUUID().toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String directory = "profile-images";
        String key = String.format("%s/%s-%s.%s", directory, timestamp, uuid, fileExtension);
        
        // 확장자로 콘텐츠 타입 결정
        String contentType = determineContentType(fileExtension);
        
        try {
            // S3 PutObject 요청 구성
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();
    
            // Presigned URL 요청 구성
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
                    .putObjectRequest(objectRequest)
                    .build();
    
            // Presigned URL 생성
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String url = presignedRequest.url().toString();
            
            log.info("Presigned URL generated for key: {}, contentType: {}", key, contentType);
            
            return new PresignedUrlResponse(url, getObjectUrl(key), key);
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
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }

    /**
     * S3 객체의 공개 URL을 생성합니다.
     */
    private String getObjectUrl(String key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }

    /**
     * Presigned URL 응답을 위한 내부 클래스
     */
    public record PresignedUrlResponse(
            String presignedUrl,   // 업로드용 presigned URL
            String objectUrl,      // 업로드 후 접근할 수 있는 객체 URL
            String objectKey       // S3에 저장된 객체 키
    ) {}
}
