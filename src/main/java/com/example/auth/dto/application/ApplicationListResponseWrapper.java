package com.example.auth.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 캠페인 신청 목록 조회 응답을 담는 Wrapper DTO
 * 신청 목록과 페이징 정보를 구조화된 형태로 제공합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캠페인 신청 목록 조회 응답")
public class ApplicationListResponseWrapper {
    
    @Schema(description = "신청 목록")
    private List<ApplicationInfoDTO> applications;
    
    @Schema(description = "페이징 정보")
    private PaginationInfo pagination;
    
    /**
     * 페이징 정보를 담는 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "페이징 정보")
    public static class PaginationInfo {
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int pageNumber;
        
        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;
        
        @Schema(description = "전체 페이지 수", example = "5")
        private int totalPages;
        
        @Schema(description = "전체 항목 수", example = "42")
        private long totalElements;
        
        @Schema(description = "첫 번째 페이지 여부", example = "true")
        private boolean first;
        
        @Schema(description = "마지막 페이지 여부", example = "false")
        private boolean last;
    }
    
    /**
     * 캠페인 신청 정보를 구조화하는 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캠페인 신청 정보")
    public static class ApplicationInfoDTO {
        @Schema(description = "신청 ID", example = "15")
        private Long id;
        
        @Schema(description = "신청 상태", example = "pending")
        private String status;
        
        @Schema(description = "신청 생성 시간")
        private String createdAt;
        
        @Schema(description = "신청 마지막 수정 시간")
        private String updatedAt;
        
        @Schema(description = "캠페인 정보")
        private CampaignInfo campaign;
        
        @Schema(description = "사용자 정보")
        private UserInfo user;
        
        /**
         * 캠페인 정보
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "간략한 캠페인 정보")
        public static class CampaignInfo {
            @Schema(description = "캠페인 ID", example = "42")
            private Long id;
            
            @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
            private String title;
        }
        
        /**
         * 사용자 정보
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "간략한 사용자 정보")
        public static class UserInfo {
            @Schema(description = "사용자 ID", example = "5")
            private Long id;
            
            @Schema(description = "사용자 닉네임", example = "인플루언서닉네임")
            private String nickname;
        }
        
        /**
         * ApplicationResponse에서 변환
         */
        public static ApplicationInfoDTO fromApplicationResponse(ApplicationResponse response) {
            return ApplicationInfoDTO.builder()
                    .id(response.getId())
                    .status(response.getStatus())
                    .createdAt(response.getCreatedAt() != null ? response.getCreatedAt().toString() : null)
                    .updatedAt(response.getUpdatedAt() != null ? response.getUpdatedAt().toString() : null)
                    .campaign(CampaignInfo.builder()
                            .id(response.getCampaignId())
                            .title(response.getCampaignTitle())
                            .build())
                    .user(UserInfo.builder()
                            .id(response.getUserId())
                            .nickname(response.getUserNickname())
                            .build())
                    .build();
        }
    }
}
