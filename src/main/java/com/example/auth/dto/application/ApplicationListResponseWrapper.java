package com.example.auth.dto.application;

import com.example.auth.constant.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
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
        @Schema(description = "현재 페이지 번호 (1부터 시작)", example = "1")
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApplicationInfoDTO {
        @Schema(description = "신청 ID", example = "15")
        private Long id;
        
        @Schema(description = "신청 상태", example = "PENDING")
        private String applicationStatus;
        
        @Schema(description = "신청 여부", example = "true")
        private Boolean hasApplied;
        
        @Schema(description = "캠페인 정보")
        private Object campaign;
        
        @Schema(description = "사용자 정보")
        private UserInfo user;
        
        /**
         * 캠페인 정보 (기본용)
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
         * 캠페인 정보 (썸네일 포함용 - my-applications 전용)
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "썸네일 포함 캠페인 정보")
        public static class CampaignInfoWithThumbnail {
            @Schema(description = "캠페인 ID", example = "42")
            private Long id;
            
            @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
            private String title;
            
            @Schema(description = "캠페인 썸네일 URL", example = "https://example.com/thumbnail.jpg")
            private String thumbnailUrl;
            
            @Schema(description = "제품 간단 정보", example = "시그니처 음료 2잔 무료 제공")
            private String productShortInfo;
            
            @Schema(description = "캠페인 타입", example = "인스타그램")
            private String campaignType;
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
         * ApplicationResponse에서 변환 (목록용 - hasApplied 없음)
         */
        public static ApplicationInfoDTO fromApplicationResponse(ApplicationResponse response) {
            return ApplicationInfoDTO.builder()
                    .id(response.getId())
                    .applicationStatus(response.getApplicationStatus().toUpperCase()) // 상태를 대문자로 정규화
                    .campaign(CampaignInfoWithThumbnail.builder()
                            .id(response.getCampaignId())
                            .title(response.getCampaignTitle())
                            .thumbnailUrl(response.getCampaignThumbnailUrl()) // 썸네일 URL 추가
                            .productShortInfo(response.getProductShortInfo()) // 제품 간단 정보 추가
                            .campaignType(response.getCampaignType()) // 캠페인 타입 추가
                            .build())
                    .user(UserInfo.builder()
                            .id(response.getUserId())
                            .nickname(response.getUserNickname())
                            .build())
                    .build();
        }
        
        /**
         * ApplicationResponse에서 변환 (my-applications용 - 썸네일 포함)
         */
        public static ApplicationInfoDTO fromApplicationResponseWithThumbnail(ApplicationResponse response, String thumbnailUrl) {
            return ApplicationInfoDTO.builder()
                    .id(response.getId())
                    .applicationStatus(response.getApplicationStatus().toUpperCase()) // 상태를 대문자로 정규화
                    .campaign(CampaignInfoWithThumbnail.builder()
                            .id(response.getCampaignId())
                            .title(response.getCampaignTitle())
                            .thumbnailUrl(thumbnailUrl)
                            .productShortInfo(response.getProductShortInfo()) // 제품 간단 정보 추가
                            .campaignType(response.getCampaignType()) // 캠페인 타입 추가
                            .build())
                    .user(UserInfo.builder()
                            .id(response.getUserId())
                            .nickname(response.getUserNickname())
                            .build())
                    .build();
        }
        
        /**
         * ApplicationResponse에서 변환 (신청 성공용 - hasApplied 포함)
         */
        public static ApplicationInfoDTO fromApplicationResponseWithApplied(ApplicationResponse response) {
            return ApplicationInfoDTO.builder()
                    .id(response.getId())
                    .applicationStatus(response.getApplicationStatus().toUpperCase()) // 상태를 대문자로 정규화
                    .hasApplied(true) // 신청 정보가 있으면 true
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
        
        /**
         * ApplicationResponse에서 변환 (체크용 - hasApplied 포함)
         */
        public static ApplicationInfoDTO fromApplicationResponseForCheck(ApplicationResponse response) {
            return ApplicationInfoDTO.builder()
                    .id(response.getId())
                    .applicationStatus(response.getApplicationStatus().toUpperCase()) // 상태를 대문자로 정규화
                    .hasApplied(true) // 신청 정보가 있으면 true
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
        
        /**
         * 신청하지 않은 경우의 DTO 생성 (hasApplied 포함)
         */
        public static ApplicationInfoDTO notApplied() {
            return ApplicationInfoDTO.builder()
                    .hasApplied(false) // 신청하지 않았으므로 false
                    .build();
        }
    }
}
