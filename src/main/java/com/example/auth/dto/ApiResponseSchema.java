package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Swagger 문서화를 위한 응답 스키마 클래스들
 */
public class ApiResponseSchema {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "플랫폼 목록 응답")
    public static class PlatformListResponse {
        @Schema(description = "플랫폼 목록")
        private List<PlatformDto> platforms;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "플랫폼 정보")
    public static class PlatformDto {
        @Schema(description = "플랫폼 ID", example = "1")
        private Long id;
        
        @Schema(description = "플랫폼 유형", example = "blog")
        private String platformType;
        
        @Schema(description = "계정 URL", example = "https://blog.naver.com/example")
        private String accountUrl;
        
        @Schema(description = "팔로워 수", example = "1000")
        private Integer followerCount;
        
        @Schema(description = "마지막 업데이트 시간", example = "2023-08-01T12:00:00")
        private LocalDateTime lastCrawledAt;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "플랫폼 연동 성공 응답")
    public static class ConnectSuccessResponse {
        @Schema(description = "플랫폼 ID", example = "1")
        private Long platformId;
        
        @Schema(description = "메시지", example = "네이버 블로그 연동이 완료되었습니다. (팔로워 수는 수동 업데이트가 필요합니다.)")
        private String message;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "팔로워 수 업데이트 성공 응답")
    public static class FollowerCountUpdateResponse {
        @Schema(description = "플랫폼 ID", example = "1")
        private Long platformId;
        
        @Schema(description = "팔로워 수", example = "1000")
        private Integer followerCount;
        
        @Schema(description = "메시지", example = "팔로워 수가 성공적으로 업데이트되었습니다.")
        private String message;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "모든 플랫폼 연동 해제 성공 응답")
    public static class DisconnectAllResponse {
        @Schema(description = "해제된 플랫폼 수", example = "3")
        private Integer count;
        
        @Schema(description = "메시지", example = "3개의 SNS 연동이 모두 해제되었습니다.")
        private String message;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "오류 응답")
    public static class ErrorResponse {
        @Schema(description = "오류 메시지", example = "유효하지 않은 토큰입니다.")
        private String message;
        
        @Schema(description = "오류 코드", example = "UNAUTHORIZED")
        private String code;
        
        @Schema(description = "HTTP 상태 코드", example = "401")
        private Integer status;
    }
}