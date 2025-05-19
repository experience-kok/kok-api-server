package com.example.auth.dto.image;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 응답을 위한 Wrapper DTO
 * 일관된 응답 구조를 위해 사용됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Presigned URL 응답")
public class PresignedUrlResponseWrapper {
    
    @Schema(description = "이미지 업로드 정보")
    private ImageUploadInfo image;
    
    /**
     * 이미지 업로드 정보를 담는 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "이미지 업로드 정보")
    public static class ImageUploadInfo {
        @Schema(description = "Presigned URL", example = "https://example-bucket.s3.amazonaws.com/path/to/image.jpg?...")
        private String presignedUrl;
        
        @Schema(description = "CloudFront URL (CDN URL)", example = "https://d1a2b3c.cloudfront.net/path/to/image.jpg")
        private String cdnUrl;
        
        @Schema(description = "객체 키 (S3 경로)", example = "profile-images/1620000000-abc123.jpg")
        private String objectKey;
    }
}
