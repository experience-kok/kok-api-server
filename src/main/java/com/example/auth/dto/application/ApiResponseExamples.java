package com.example.auth.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Swagger 문서화를 위한 API 응답 예시 클래스들
 * 실제로 사용되지는 않고 API 문서 자동 생성을 위한 클래스입니다.
 */
public class ApiResponseExamples {

    /**
     * 캠페인 신청 성공 응답 예시
     */
    @Data
    @Schema(description = "캠페인 신청 성공 응답")
    public static class ApplicationSuccessResponse {
        @Schema(description = "응답 성공 여부", example = "true")
        private boolean success;

        @Schema(description = "응답 메시지", example = "캠페인 신청이 완료되었습니다.")
        private String message;

        @Schema(description = "응답 데이터")
        private Map<String, CampaignApplicationResponse> data;

        @Schema(description = "응답 코드", example = "201")
        private int code;
    }

    /**
     * 캠페인 신청 목록 페이징 응답 예시
     */
    @Data
    @Schema(description = "캠페인 신청 목록 페이징 응답")
    public static class ApplicationPageResponse {
        @Schema(description = "응답 성공 여부", example = "true")
        private boolean success;

        @Schema(description = "응답 메시지", example = "신청 목록 조회 성공")
        private String message;

        @Schema(description = "페이징 응답 데이터")
        private PageResponseData data;

        @Schema(description = "응답 코드", example = "200")
        private int code;

        @Data
        @Schema(description = "페이징 응답 데이터")
        public static class PageResponseData {
            @Schema(description = "신청 목록 데이터")
            private List<CampaignApplicationResponse> content;

            @Schema(description = "현재 페이지", example = "0")
            private int pageNumber;

            @Schema(description = "페이지 크기", example = "10")
            private int pageSize;

            @Schema(description = "전체 페이지 수", example = "5")
            private int totalPages;

            @Schema(description = "전체 항목 수", example = "42")
            private long totalElements;

            @Schema(description = "첫 페이지 여부", example = "true")
            private boolean first;

            @Schema(description = "마지막 페이지 여부", example = "false")
            private boolean last;
        }
    }

    /**
     * 오류 응답 예시
     */
    @Data
    @Schema(description = "API 오류 응답")
    public static class ErrorResponse {
        @Schema(description = "응답 성공 여부", example = "false")
        private boolean success;

        @Schema(description = "오류 메시지", example = "이미 해당 캠페인에 신청하셨습니다.")
        private String message;

        @Schema(description = "오류 코드", example = "INVALID_REQUEST")
        private String errorCode;

        @Schema(description = "응답 코드", example = "400")
        private int code;
    }

    /**
     * 신청 상태 확인 응답 예시
     */
    @Data
    @Schema(description = "신청 상태 확인 응답")
    public static class ApplicationCheckResponse {
        @Schema(description = "응답 성공 여부", example = "true")
        private boolean success;

        @Schema(description = "응답 메시지", example = "신청 상태 확인 완료")
        private String message;

        @Schema(description = "응답 데이터")
        private ApplicationCheckData data;

        @Schema(description = "응답 코드", example = "200")
        private int code;

        @Data
        @Schema(description = "신청 상태 확인 데이터")
        public static class ApplicationCheckData {
            @Schema(description = "신청 여부", example = "true")
            private boolean hasApplied;
        }
    }

    /**
     * 통계 응답 예시
     */
    @Data
    @Schema(description = "신청 통계 응답")
    public static class ApplicationStatsResponse {
        @Schema(description = "응답 성공 여부", example = "true")
        private boolean success;

        @Schema(description = "응답 메시지", example = "신청 통계 조회 성공")
        private String message;

        @Schema(description = "응답 데이터")
        private ApplicationStatsData data;

        @Schema(description = "응답 코드", example = "200")
        private int code;

        @Data
        @Schema(description = "신청 통계 데이터")
        public static class ApplicationStatsData {
            @Schema(description = "대기 중인 신청 수", example = "10")
            private long pendingCount;

            @Schema(description = "거절된 신청 수", example = "5")
            private long rejectedCount;

            @Schema(description = "완료된 신청 수", example = "15")
            private long completedCount;

            @Schema(description = "전체 신청 수", example = "30")
            private long totalCount;
        }
    }
}