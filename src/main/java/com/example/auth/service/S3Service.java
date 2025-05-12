package com.example.auth.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
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

    /**
     * 이미지 업로드를 위한 presigned URL을 생성합니다.
     * 단순화된 버전 - presigned URL만 반환
     *
     * @param fileExtension 파일 확장자 (예: jpg, png)
     * @return 업로드용 presigned URL 문자열
     */
    public String generatePresignedUrl(String fileExtension) {
        // 고유한 키 생성
        String uuid = UUID.randomUUID().toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String directory = "profile-images";
        String objectKey = String.format("%s/%s-%s.%s", directory, timestamp, uuid, fileExtension);

        try {
            // S3 Presigned URL 요청 생성
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey)
                    .withMethod(HttpMethod.PUT)
                    .withExpiration(new Date(System.currentTimeMillis() + (presignedUrlExpirationSeconds * 1000)));

            // 공개 읽기 권한 설정
            generatePresignedUrlRequest.addRequestParameter(
                    Headers.S3_CANNED_ACL,
                    CannedAccessControlList.PublicRead.toString());

            // 콘텐츠 타입 설정
            generatePresignedUrlRequest.setContentType(determineContentType(fileExtension));

            // Presigned URL 생성 및 반환
            String presignedUrl = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();

            log.info("Presigned URL generated for key: {}", objectKey);

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
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
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