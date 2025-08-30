package com.example.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API 응답 스키마 정의
 */
public class ApiResponseSchemas {

    @Data
    @Schema(description = "로그인 성공 응답", example = """
        {
          "success": true,
          "message": "카카오 로그인 성공",
          "status": 200,
          "data": {
            "loginType": "login",
            "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "user": {
              "id": 123,
              "email": "user@example.com",
              "nickname": "사용자123",
              "role": "USER",
              "profileImg": "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg",
              "phone": "010-1234-5678",
              "gender": "MALE",
              "age": 30
            }
          }
        }
        """)
    public static class LoginSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "카카오 로그인 성공")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "로그인 응답 데이터")
        private LoginData data;

        @Data
        @Schema(description = "로그인 데이터")
        public static class LoginData {
            @Schema(description = "로그인 타입", example = "login", allowableValues = {"login", "registration"})
            private String loginType;
            
            @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            private String accessToken;
            
            @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            private String refreshToken;
            
            @Schema(description = "사용자 정보")
            private UserInfo user;
        }

        @Data
        @Schema(description = "사용자 정보")
        public static class UserInfo {
            @Schema(description = "사용자 ID", example = "123")
            private Long id;
            
            @Schema(description = "이메일", example = "user@example.com")
            private String email;
            
            @Schema(description = "닉네임", example = "사용자123")
            private String nickname;
            
            @Schema(description = "권한", example = "USER", allowableValues = {"USER", "CLIENT", "ADMIN"})
            private String role;
            
            @Schema(description = "프로필 이미지 URL", example = "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg")
            private String profileImg;
            
            @Schema(description = "전화번호", example = "010-1234-5678")
            private String phone;
            
            @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE", "UNKNOWN"})
            private String gender;
            
            @Schema(description = "나이", example = "30")
            private Integer age;
        }
    }

    @Data
    @Schema(description = "토큰 재발급 응답", example = """
        {
          "success": true,
          "message": "토큰이 성공적으로 재발급되었습니다.",
          "status": 200,
          "data": {
            "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
          }
        }
        """)
    public static class TokenRefreshResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "토큰이 성공적으로 재발급되었습니다.")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "토큰 데이터")
        private TokenData data;

        @Data
        @Schema(description = "토큰 데이터")
        public static class TokenData {
            @Schema(description = "새 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            private String accessToken;
            
            @Schema(description = "새 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            private String refreshToken;
        }
    }

    // === 브랜드존 API 스키마들 ===
    
    @Data
    @Schema(description = "브랜드 목록 조회 성공 응답")
    public static class BrandListSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "브랜드 목록 조회 성공")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "브랜드 목록 데이터")
        private BrandListData data;

        @Data
        @Schema(description = "브랜드 목록 데이터")
        public static class BrandListData {
            @Schema(description = "브랜드 목록")
            private java.util.List<BrandItem> content;
            
            @Schema(description = "페이지 번호", example = "1")
            private Integer pageNumber;
            
            @Schema(description = "페이지 크기", example = "12")
            private Integer pageSize;
            
            @Schema(description = "총 페이지 수", example = "5")
            private Integer totalPages;
            
            @Schema(description = "총 브랜드 수", example = "58")
            private Long totalElements;
            
            @Schema(description = "첫 페이지 여부", example = "true")
            private Boolean first;
            
            @Schema(description = "마지막 페이지 여부", example = "false")
            private Boolean last;
        }

        @Data
        @Schema(description = "브랜드 항목")
        public static class BrandItem {
            @Schema(description = "브랜드 ID", example = "1")
            private Long brandId;
            
            @Schema(description = "브랜드명", example = "ABC 코스메틱")
            private String brandName;
            
            @Schema(description = "총 캠페인 수", example = "15")
            private Integer totalCampaigns;
            
            @Schema(description = "활성 캠페인 수", example = "3")
            private Integer activeCampaigns;
        }
    }

    @Data
    @Schema(description = "브랜드 정보 조회 성공 응답")
    public static class BrandInfoSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "브랜드 정보 조회 성공")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "브랜드 정보 데이터")
        private BrandInfoData data;

        @Data
        @Schema(description = "브랜드 정보 데이터")
        public static class BrandInfoData {
            @Schema(description = "브랜드 ID", example = "1")
            private Long brandId;
            
            @Schema(description = "브랜드명", example = "ABC 코스메틱")
            private String brandName;
            
            @Schema(description = "담당자명", example = "김담당")
            private String contactPerson;
            
            @Schema(description = "연락처", example = "02-1234-5678")
            private String phoneNumber;
            
            @Schema(description = "총 캠페인 수", example = "15")
            private Integer totalCampaigns;
            
            @Schema(description = "활성 캠페인 수", example = "3")
            private Integer activeCampaigns;
        }
    }

    // === 좋아요 API 스키마들 ===

    @Data
    @Schema(description = "좋아요 토글 성공 응답")
    public static class LikeToggleSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "좋아요가 추가되었습니다")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "좋아요 토글 결과 데이터")
        private LikeToggleData data;

        @Data
        @Schema(description = "좋아요 토글 데이터")
        public static class LikeToggleData {
            @Schema(description = "현재 좋아요 상태", example = "true")
            private Boolean liked;
            
            @Schema(description = "총 좋아요 수", example = "43")
            private Integer totalCount;
            
            @Schema(description = "캠페인 ID", example = "123")
            private Long campaignId;
        }
    }

    @Data
    @Schema(description = "내가 좋아요한 캠페인 목록 조회 성공 응답")
    public static class MyLikedCampaignSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "내가 좋아요한 캠페인 목록 조회 성공")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "내가 좋아요한 캠페인 목록 데이터")
        private MyLikedCampaignData data;

        @Data
        @Schema(description = "내가 좋아요한 캠페인 목록 데이터")
        public static class MyLikedCampaignData {
            @Schema(description = "캠페인 목록")
            private java.util.List<MyLikedCampaignItem> content;
            
            @Schema(description = "페이지 번호", example = "1")
            private Integer pageNumber;
            
            @Schema(description = "페이지 크기", example = "10")
            private Integer pageSize;
            
            @Schema(description = "총 페이지 수", example = "3")
            private Integer totalPages;
            
            @Schema(description = "총 캠페인 수", example = "25")
            private Long totalElements;
            
            @Schema(description = "첫 페이지 여부", example = "true")
            private Boolean first;
            
            @Schema(description = "마지막 페이지 여부", example = "false")
            private Boolean last;
        }

        @Data
        @Schema(description = "내가 좋아요한 캠페인 항목")
        public static class MyLikedCampaignItem {
            @Schema(description = "캠페인 ID", example = "123")
            private Long campaignId;
            
            @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
            private String title;
            
            @Schema(description = "캠페인 유형", example = "인스타그램")
            private String campaignType;
            
            @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumbnail.jpg")
            private String thumbnailUrl;
            
            @Schema(description = "현재 신청자 수", example = "8")
            private Integer currentApplicants;
            
            @Schema(description = "최대 신청자 수", example = "15")
            private Integer maxApplicants;
            
            @Schema(description = "신청 마감일", example = "2027-12-12")
            private String applicationDeadlineDate;
            
            @Schema(description = "좋아요 수", example = "42")
            private Integer likeCount;
            
            @Schema(description = "좋아요한 시간", example = "2025-07-29T10:30:00")
            private String likedAt;
        }
    }

    // === 캠페인 신청 API 스키마들 ===

    @Data
    @Schema(description = "캠페인 신청 성공 응답")
    public static class CampaignApplicationSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "캠페인 신청이 완료되었어요")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "201")
        private int status;
        
        @Schema(description = "신청 결과 데이터")
        private ApplicationData data;

        @Data
        @Schema(description = "신청 데이터")
        public static class ApplicationData {
            @Schema(description = "신청 정보")
            private ApplicationItem application;
        }

        @Data
        @Schema(description = "신청 항목")
        public static class ApplicationItem {
            @Schema(description = "신청 ID", example = "15")
            private Long id;
            
            @Schema(description = "신청 상태", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED", "COMPLETED"})
            private String applicationStatus;
            
            @Schema(description = "신청 여부", example = "true")
            private Boolean hasApplied;
            
            @Schema(description = "캠페인 정보")
            private CampaignInfo campaign;
            
            @Schema(description = "사용자 정보")
            private UserInfo user;
        }

        @Data
        @Schema(description = "캠페인 정보")
        public static class CampaignInfo {
            @Schema(description = "캠페인 ID", example = "42")
            private Long id;
            
            @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
            private String title;
        }

        @Data
        @Schema(description = "사용자 정보")
        public static class UserInfo {
            @Schema(description = "사용자 ID", example = "5")
            private Long id;
            
            @Schema(description = "닉네임", example = "인플루언서닉네임")
            private String nickname;
            
            @Schema(description = "이메일", example = "user@example.com")
            private String email;
            
            @Schema(description = "권한", example = "USER", allowableValues = {"USER", "CLIENT", "ADMIN"})
            private String role;
            
            @Schema(description = "프로필 이미지 URL", example = "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg")
            private String profileImg;
            
            @Schema(description = "전화번호", example = "010-1234-5678")
            private String phone;
            
            @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE", "UNKNOWN"})
            private String gender;
            
            @Schema(description = "나이", example = "30")
            private Integer age;
        }
    }

    @Data
    @Schema(description = "내 신청 목록 조회 성공 응답")
    public static class MyApplicationsSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "신청 목록을 조회했어요")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "신청 목록 데이터")
        private MyApplicationsData data;

        @Data
        @Schema(description = "신청 목록 데이터")
        public static class MyApplicationsData {
            @Schema(description = "신청 목록")
            private java.util.List<ApplicationItem> applications;
            
            @Schema(description = "페이징 정보")
            private PaginationInfo pagination;
        }

        @Data
        @Schema(description = "신청 항목")
        public static class ApplicationItem {
            @Schema(description = "신청 ID", example = "15")
            private Long id;
            
            @Schema(description = "신청 상태", example = "APPLIED")
            private String applicationStatus;
            
            @Schema(description = "신청 여부", example = "true")
            private Boolean hasApplied;
            
            @Schema(description = "캠페인 정보")
            private CampaignInfo campaign;
            
            @Schema(description = "사용자 정보")
            private UserInfo user;
        }

        @Data
        @Schema(description = "캠페인 정보")
        public static class CampaignInfo {
            @Schema(description = "캠페인 ID", example = "42")
            private Long id;
            
            @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
            private String title;
            
            @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
            private String thumbnailUrl;
        }

        @Data
        @Schema(description = "사용자 정보")
        public static class UserInfo {
            @Schema(description = "사용자 ID", example = "5")
            private Long id;
            
            @Schema(description = "닉네임", example = "인플루언서닉네임")
            private String nickname;
            
            @Schema(description = "이메일", example = "user@example.com")
            private String email;
            
            @Schema(description = "권한", example = "USER", allowableValues = {"USER", "CLIENT", "ADMIN"})
            private String role;
            
            @Schema(description = "프로필 이미지 URL", example = "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg")
            private String profileImg;
            
            @Schema(description = "전화번호", example = "010-1234-5678")
            private String phone;
            
            @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE", "UNKNOWN"})
            private String gender;
            
            @Schema(description = "나이", example = "30")
            private Integer age;
        }

        @Data
        @Schema(description = "페이징 정보")
        public static class PaginationInfo {
            @Schema(description = "페이지 번호", example = "1")
            private Integer pageNumber;
            
            @Schema(description = "페이지 크기", example = "10")
            private Integer pageSize;
            
            @Schema(description = "총 페이지 수", example = "3")
            private Integer totalPages;
            
            @Schema(description = "총 항목 수", example = "25")
            private Long totalElements;
            
            @Schema(description = "첫 페이지 여부", example = "true")
            private Boolean first;
            
            @Schema(description = "마지막 페이지 여부", example = "false")
            private Boolean last;
        }
    }

    @Data
    @Schema(description = "캠페인 신청자 목록 조회 성공 응답")
    public static class CampaignApplicantsSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "캠페인 신청자 목록을 조회했어요")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "신청자 목록 데이터")
        private CampaignApplicantsData data;

        @Data
        @Schema(description = "신청자 목록 데이터")
        public static class CampaignApplicantsData {
            @Schema(description = "캠페인 정보")
            private CampaignInfo campaign;
            
            @Schema(description = "신청자 목록")
            private java.util.List<ApplicantItem> applicants;
            
            @Schema(description = "페이징 정보")
            private PaginationInfo pagination;
        }

        @Data
        @Schema(description = "캠페인 정보")
        public static class CampaignInfo {
            @Schema(description = "캠페인 ID", example = "42")
            private Long id;
            
            @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
            private String title;
            
            @Schema(description = "총 신청자 수", example = "15")
            private Long totalApplicants;
        }

        @Data
        @Schema(description = "신청자 항목")
        public static class ApplicantItem {
            @Schema(description = "신청 ID", example = "101")
            private Long applicationId;
            
            @Schema(description = "신청 상태", example = "pending")
            private String applicationStatus;
            
            @Schema(description = "사용자 정보")
            private UserInfo user;
            
            @Schema(description = "SNS 플랫폼 목록")
            private java.util.List<SnsPlatform> snsPlatforms;
        }

        @Data
        @Schema(description = "사용자 정보")
        public static class UserInfo {
            @Schema(description = "사용자 ID", example = "5")
            private Long id;
            
            @Schema(description = "닉네임", example = "인플루언서닉네임")
            private String nickname;
            
            @Schema(description = "전화번호", example = "010-1234-5678")
            private String phone;
            
            @Schema(description = "이메일", example = "user@example.com")
            private String email;
            
            @Schema(description = "권한", example = "USER", allowableValues = {"USER", "CLIENT", "ADMIN"})
            private String role;
            
            @Schema(description = "프로필 이미지 URL", example = "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg")
            private String profileImg;
            
            @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE", "UNKNOWN"})
            private String gender;
            
            @Schema(description = "나이", example = "30")
            private Integer age;
        }

        @Data
        @Schema(description = "SNS 플랫폼")
        public static class SnsPlatform {
            @Schema(description = "플랫폼 타입", example = "INSTAGRAM")
            private String platformType;
            
            @Schema(description = "계정 URL", example = "https://instagram.com/username")
            private String accountUrl;
            
            @Schema(description = "팔로워 수", example = "10000")
            private Integer followerCount;
        }

        @Data
        @Schema(description = "페이징 정보")
        public static class PaginationInfo {
            @Schema(description = "페이지 번호", example = "1")
            private Integer pageNumber;
            
            @Schema(description = "페이지 크기", example = "10")
            private Integer pageSize;
            
            @Schema(description = "총 페이지 수", example = "2")
            private Integer totalPages;
            
            @Schema(description = "총 항목 수", example = "15")
            private Long totalElements;
            
            @Schema(description = "첫 페이지 여부", example = "true")
            private Boolean first;
            
            @Schema(description = "마지막 페이지 여부", example = "false")
            private Boolean last;
        }
    }

    @Data
    @Schema(description = "캠페인 신청자 선정 성공 응답")
    public static class CampaignSelectionSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "캠페인 신청자 선정이 완료되었습니다")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "선정 결과 데이터")
        private CampaignSelectionData data;

        @Data
        @Schema(description = "선정 결과 데이터")
        public static class CampaignSelectionData {
            @Schema(description = "캠페인 ID", example = "42")
            private Long campaignId;
            
            @Schema(description = "캠페인 제목", example = "신상 음료 체험단 모집")
            private String campaignTitle;
            
            @Schema(description = "총 신청자 수", example = "20")
            private Integer totalApplicants;
            
            @Schema(description = "선정자 수", example = "5")
            private Integer selectedCount;
            
            @Schema(description = "미선정자 수", example = "15")
            private Integer unselectedCount;
            
            @Schema(description = "선정된 신청자 목록")
            private java.util.List<SelectedApplicant> selectedApplicants;
            
            @Schema(description = "선정 처리 시간", example = "2025-07-29T15:30:00Z")
            private String selectionProcessedAt;
            
            @Schema(description = "알림 전송 여부", example = "true")
            private Boolean notificationSent;
        }

        @Data
        @Schema(description = "선정된 신청자")
        public static class SelectedApplicant {
            @Schema(description = "신청 ID", example = "101")
            private Long applicationId;
            
            @Schema(description = "사용자 ID", example = "5")
            private Long userId;
            
            @Schema(description = "닉네임", example = "인플루언서1")
            private String nickname;
            
            @Schema(description = "이메일", example = "influencer1@example.com")
            private String email;
            
            @Schema(description = "선정 시간", example = "2025-07-29T15:30:00Z")
            private String selectedAt;
        }
    }

    // === 공통 에러 응답 스키마 ===
    
    @Data
    @Schema(description = "API 에러 응답")
    public static class ApiErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "요청을 처리할 수 없습니다")
        private String message;
        
        @Schema(description = "에러 코드", example = "INVALID_REQUEST")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "400")
        private int status;
    }

    // === 공통 성공 응답 스키마 ===
    
    @Data
    @Schema(description = "API 성공 응답 (데이터 없음)")
    public static class ApiSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "응답 데이터", nullable = true)
        private Object data;
    }

    // === 회원 탈퇴 API 스키마들 ===

    @Data
    @Schema(description = "회원 탈퇴 성공 응답")
    public static class WithdrawalSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "회원 탈퇴가 완료되었습니다")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "탈퇴 결과 데이터")
        private WithdrawalData data;

        @Data
        @Schema(description = "탈퇴 데이터")
        public static class WithdrawalData {
            @Schema(description = "탈퇴 처리 시간", example = "2025-08-06T15:30:00+09:00")
            private String withdrawnAt;
            
            @Schema(description = "재가입 가능 시간", example = "2025-08-07T15:30:00+09:00")
            private String canRejoinAt;
            
            @Schema(description = "탈퇴 사유", example = "서비스를 더 이상 이용하지 않아서")
            private String withdrawalReason;
            
            @Schema(description = "안내 메시지", example = "탈퇴가 완료되었습니다. 24시간 후 재가입이 가능합니다.")
            private String message;
        }
    }

    @Data
    @Schema(description = "재가입 제한 에러 응답")
    public static class RejoinRestrictionErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "탈퇴 후 24시간 동안 재가입할 수 없습니다. (남은 시간: 15시간)")
        private String message;
        
        @Schema(description = "에러 코드", example = "REJOIN_RESTRICTED")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "403")
        private int status;
        
        @Schema(description = "재가입 제한 데이터")
        private RejoinRestrictionData data;

        @Data
        @Schema(description = "재가입 제한 데이터")
        public static class RejoinRestrictionData {
            @Schema(description = "남은 시간 (시간 단위)", example = "15")
            private Long remainingHours;
            
            @Schema(description = "재가입 가능 시간", example = "2025-08-07T15:30:00+09:00")
            private String canRejoinAt;
        }
    }

    // === 캠페인 생성 API 스키마들 ===

    @Data
    @Schema(description = "캠페인 생성 성공 응답 (V1)")
    public static class CampaignV1SuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "캠페인이 성공적으로 등록되었어요.")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "201")
        private int status;
        
        @Schema(description = "생성된 캠페인 정보")
        private CampaignV1Data data;

        @Data
        @Schema(description = "캠페인 V1 데이터")
        public static class CampaignV1Data {
            @Schema(description = "캠페인 고유 식별자", example = "1")
            private Long id;
            
            @Schema(description = "캠페인 썸네일 이미지 URL", example = "https://drxgfm74s70w1.cloudfront.net/campaign-images/campaign123.jpg")
            private String thumbnailUrl;
            
            @Schema(description = "캠페인 진행 플랫폼", example = "인스타그램", allowableValues = {"인스타그램", "블로그", "유튜브", "틱톡"})
            private String campaignType;
            
            @Schema(description = "캠페인 제목", example = "인스타 감성 카페 체험단 모집")
            private String title;
            
            @Schema(description = "제공 제품/서비스에 대한 간략 정보", example = "시그니처 음료 2잔 무료 제공")
            private String productShortInfo;
            
            @Schema(description = "최대 신청 가능 인원 수", example = "10")
            private Integer maxApplicants;
            
            @Schema(description = "모집 시작 날짜", example = "2025-06-01")
            private String recruitmentStartDate;
            
            @Schema(description = "모집 종료 날짜", example = "2025-06-15")
            private String recruitmentEndDate;
            
            @Schema(description = "참여자 선정 날짜", example = "2025-06-16")
            private String selectionDate;
            
            @Schema(description = "승인 상태", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
            private String approvalStatus;
            
            @Schema(description = "카테고리 정보")
            private CampaignCategoryInfo category;
            
            @Schema(description = "캠페인 등록 사용자 정보")
            private CampaignUserInfo user;
            
            @Schema(description = "캠페인 생성 시간", example = "2025-01-01T00:00:00Z")
            private String createdAt;
            
            @Schema(description = "캠페인 수정 시간", example = "2025-01-01T00:00:00Z")
            private String updatedAt;
        }

        @Data
        @Schema(description = "캠페인 카테고리 정보")
        public static class CampaignCategoryInfo {
            @Schema(description = "카테고리 ID", example = "1")
            private Long id;
            
            @Schema(description = "카테고리 타입", example = "방문", allowableValues = {"방문", "배송"})
            private String type;
            
            @Schema(description = "카테고리 이름", example = "카페")
            private String name;
        }

        @Data
        @Schema(description = "캠페인 등록 사용자 정보")
        public static class CampaignUserInfo {
            @Schema(description = "사용자 ID", example = "1")
            private Long id;
            
            @Schema(description = "이메일", example = "client@example.com")
            private String email;
            
            @Schema(description = "닉네임", example = "카페사장님")
            private String nickname;
            
            @Schema(description = "권한", example = "CLIENT", allowableValues = {"USER", "CLIENT", "ADMIN"})
            private String role;
            
            @Schema(description = "프로필 이미지 URL", example = "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg")
            private String profileImg;
        }
    }

    @Data
    @Schema(description = "캠페인 생성 카테고리 에러 응답")
    public static class CampaignCategoryErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "유효하지 않은 카테고리 타입이에요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "INVALID_CATEGORY")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "400")
        private int status;
    }

    @Data
    @Schema(description = "캠페인 생성 날짜 검증 에러 응답")
    public static class CampaignDateValidationErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "모집 시작일은 모집 종료일보다 이전이어야 해요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "INVALID_DATE_ORDER")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "400")
        private int status;
    }

    @Data
    @Schema(description = "캠페인 생성 권한 에러 응답")
    public static class CampaignCreationPermissionErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "CLIENT 권한이 필요해요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "INSUFFICIENT_PERMISSION")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "403")
        private int status;
    }

    @Data
    @Schema(description = "캠페인 수정 제한 에러 응답")
    public static class CampaignUpdateRestrictionErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "신청자가 있을 때는 카테고리를 변경할 수 없어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "UPDATE_RESTRICTED")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "400")
        private int status;
    }

    @Data
    @Schema(description = "캠페인 소유자 권한 에러 응답")
    public static class CampaignOwnershipErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "본인이 생성한 캠페인만 수정할 수 있어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "NOT_CAMPAIGN_OWNER")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "403")
        private int status;
    }

    @Data
    @Schema(description = "인플루언서 다중 선정 성공 응답")
    public static class MultipleSelectionSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "인플루언서 다중 선정이 완료되었습니다")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "다중 선정 결과 데이터")
        private MultipleSelectionData data;

        @Data
        @Schema(description = "다중 선정 결과 데이터")
        public static class MultipleSelectionData {
            @Schema(description = "총 요청 수", example = "3")
            private Integer totalRequested;
            
            @Schema(description = "성공 수", example = "2")
            private Integer successCount;
            
            @Schema(description = "실패 수", example = "1")
            private Integer failCount;
            
            @Schema(description = "성공한 선정 목록", example = "[1, 3]")
            private java.util.List<Long> successfulSelections;
            
            @Schema(description = "실패한 선정 목록")
            private java.util.List<FailedSelection> failedSelections;
        }

        @Data
        @Schema(description = "실패한 선정")
        public static class FailedSelection {
            @Schema(description = "신청 ID", example = "2")
            private Long applicationId;
            
            @Schema(description = "실패 사유", example = "이미 선정된 신청이에요.")
            private String reason;
        }
    }

    @Data
    @Schema(description = "캠페인별 미션 제출 목록 조회 성공 응답")
    public static class CampaignMissionSubmissionsSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "미션 목록 조회 성공")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "미션 제출 목록")
        private java.util.List<MissionSubmissionItem> data;

        @Data
        @Schema(description = "미션 제출 항목")
        public static class MissionSubmissionItem {
            @Schema(description = "미션 제출 ID", example = "1")
            private Long id;
            
            @Schema(description = "인플루언서 정보")
            private UserInfo user;
            
            @Schema(description = "캠페인 정보")
            private CampaignInfo campaign;
            
            @Schema(description = "미션 정보")
            private MissionInfo mission;
        }

        @Data
        @Schema(description = "인플루언서 정보")
        public static class UserInfo {
            @Schema(description = "유저 ID", example = "45")
            private Long id;
            
            @Schema(description = "닉네임", example = "맛집탐험가")
            private String nickname;
            
            @Schema(description = "성별", example = "MALE")
            private String gender;
            
            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            private String profileImage;
        }

        @Data
        @Schema(description = "캠페인 정보")
        public static class CampaignInfo {
            @Schema(description = "캠페인 ID", example = "123")
            private Long id;
            
            @Schema(description = "캠페인 제목", example = "이탈리안 레스토랑 신메뉴 체험단")
            private String title;
            
            @Schema(description = "캠페인 타입", example = "인스타그램")
            private String campaignType;
        }

        @Data
        @Schema(description = "미션 정보")
        public static class MissionInfo {
            @Schema(description = "미션 URL", example = "https://instagram.com/p/abc123")
            private String missionUrl;
            
            @Schema(description = "제출 일시", example = "2024-03-15T14:30:00Z")
            private String submittedAt;
            
            @Schema(description = "검토 일시", example = "2024-03-16T10:30:00Z")
            private String reviewedAt;
            
            @Schema(description = "클라이언트 피드백", example = "미션을 잘 수행해주셨습니다.")
            private String clientFeedback;
        }
    }

    @Data
    @Schema(description = "미션 검토 성공 응답")
    public static class MissionReviewSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "미션 검토 완료")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "응답 데이터", nullable = true)
        private Object data;
    }

    @Data
    @Schema(description = "유저 포트폴리오 조회 성공 응답")
    public static class UserPortfolioSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "포트폴리오 조회 성공")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "포트폴리오 목록")
        private java.util.List<PortfolioItem> data;

        @Data
        @Schema(description = "포트폴리오 항목")
        public static class PortfolioItem {
            @Schema(description = "미션 ID", example = "1")
            private Long missionId;
            
            @Schema(description = "캠페인 제목", example = "맛집 체험단")
            private String campaignTitle;
            
            @Schema(description = "미션 제목", example = "이탈리안 레스토랑 후기")
            private String submissionTitle;
            
            @Schema(description = "플랫폼 타입", example = "인스타그램")
            private String platformType;
            
            @Schema(description = "완료 일시", example = "2024-01-15T10:30:00Z")
            private String completedAt;
            
            @Schema(description = "클라이언트 평점", example = "5")
            private Integer clientRating;
        }
    }

    @Data
    @Schema(description = "미션 제출 성공 응답")
    public static class MissionSubmissionSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "미션 제출 완료")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "미션 제출 결과 데이터")
        private MissionSubmissionData data;

        @Data
        @Schema(description = "미션 제출 데이터")
        public static class MissionSubmissionData {
            @Schema(description = "미션 제출 ID", example = "1")
            private Long id;
            
            @Schema(description = "미션 제출 URL", example = "https://instagram.com/p/xyz123")
            private String submissionUrl;
            
            @Schema(description = "미션 제목", example = "맛집 체험 후기")
            private String submissionTitle;
            
            @Schema(description = "검토 상태", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REVISION_REQUESTED"})
            private String reviewStatus;
            
            @Schema(description = "제출 일시", example = "2024-03-15T14:30:00Z")
            private String submittedAt;
        }
    }

    @Data
    @Schema(description = "내 미션 이력 조회 성공 응답")
    public static class MyMissionHistorySuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "미션 이력 조회 성공")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "미션 이력 목록")
        private java.util.List<MissionHistoryItem> data;

        @Data
        @Schema(description = "미션 이력 항목")
        public static class MissionHistoryItem {
            @Schema(description = "미션 ID", example = "1")
            private Long missionId;
            
            @Schema(description = "캠페인 제목", example = "맛집 체험단")
            private String campaignTitle;
            
            @Schema(description = "미션 제목", example = "이탈리안 레스토랑 후기")
            private String submissionTitle;
            
            @Schema(description = "검토 상태", example = "APPROVED", allowableValues = {"PENDING", "APPROVED", "REVISION_REQUESTED"})
            private String reviewStatus;
            
            @Schema(description = "제출 일시", example = "2024-01-15T10:30:00Z")
            private String submittedAt;
            
            @Schema(description = "검토 일시", example = "2024-01-16T10:30:00Z")
            private String reviewedAt;
            
            @Schema(description = "클라이언트 평점", example = "5")
            private Integer clientRating;
            
            @Schema(description = "클라이언트 피드백", example = "훌륭한 리뷰였습니다!")
            private String clientFeedback;
        }
    }

    // === 미션 관리 API 특정 에러 응답 스키마들 ===
    
    @Data
    @Schema(description = "인플루언서 선정 에러 응답")
    public static class MultipleSelectionErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "캠페인 생성자만 인플루언서를 선정할 수 있어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "MULTIPLE_SELECTION_ERROR")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "400")
        private int status;
    }

    @Data
    @Schema(description = "캠페인 권한 없음 에러 응답")
    public static class CampaignAccessDeniedErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "캠페인 생성자만 미션 제출 목록을 조회할 수 있어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "CAMPAIGN_ACCESS_DENIED")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "403")
        private int status;
    }

    @Data
    @Schema(description = "미션 검토 권한 없음 에러 응답")
    public static class MissionReviewAccessDeniedErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "캠페인 관리자만 미션을 검토할 수 있어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "MISSION_REVIEW_ACCESS_DENIED")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "403")
        private int status;
    }

    @Data
    @Schema(description = "미션 검토 상태 에러 응답")
    public static class MissionReviewStatusErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "검토 대기 상태의 미션만 검토할 수 있어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "INVALID_MISSION_STATUS")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "400")
        private int status;
    }

    @Data
    @Schema(description = "미션 제출 권한 없음 에러 응답")
    public static class MissionSubmissionAccessDeniedErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "본인의 신청에만 미션을 제출할 수 있어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "MISSION_SUBMISSION_ACCESS_DENIED")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "403")
        private int status;
    }

    @Data
    @Schema(description = "미션 제출 상태 에러 응답")
    public static class MissionSubmissionStatusErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "선정된 신청에만 미션을 제출할 수 있어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "INVALID_APPLICATION_STATUS")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "400")
        private int status;
    }

    @Data
    @Schema(description = "중복 미션 제출 에러 응답")
    public static class DuplicateMissionSubmissionErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "이미 미션을 제출했어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "DUPLICATE_MISSION_SUBMISSION")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "400")
        private int status;
    }

    @Data
    @Schema(description = "신청 상태 에러 응답")
    public static class ApplicationStatusErrorResponse {
        @Schema(description = "성공 여부", example = "false")
        private boolean success;
        
        @Schema(description = "에러 메시지", example = "선정 대기 상태의 신청만 선정할 수 있어요.")
        private String message;
        
        @Schema(description = "에러 코드", example = "INVALID_APPLICATION_STATUS")
        private String errorCode;
        
        @Schema(description = "HTTP 상태 코드", example = "400")
        private int status;
    }

    // === 공지사항 API 스키마들 ===
    
    @Data
    @Schema(description = "공지사항 목록 조회 성공 응답")
    public static class NoticeListSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "공지사항 목록을 성공적으로 조회했습니다.")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "공지사항 목록 데이터")
        private NoticeListData data;

        @Data
        @Schema(description = "공지사항 목록 데이터")
        public static class NoticeListData {
            @Schema(description = "공지사항 목록")
            private java.util.List<NoticeItem> notices;
            
            @Schema(description = "페이징 정보")
            private PaginationInfo pagination;
        }

        @Data
        @Schema(description = "공지사항 항목")
        public static class NoticeItem {
            @Schema(description = "공지사항 ID", example = "1")
            private Long id;
            
            @Schema(description = "제목", example = "중요한 공지사항입니다")
            private String title;
            
            @Schema(description = "조회수", example = "156")
            private Integer viewCount;
            
            @Schema(description = "필독 여부", example = "true")
            private Boolean isMustRead;
            
            @Schema(description = "작성자 ID", example = "1")
            private Long authorId;
            
            @Schema(description = "작성자명", example = "관리자")
            private String authorName;
            
            @Schema(description = "생성 시간", example = "2025-08-27T10:30:00")
            private String createdAt;
            
            @Schema(description = "수정 시간", example = "2025-08-27T15:45:00")
            private String updatedAt;
        }

        @Data
        @Schema(description = "페이징 정보")
        public static class PaginationInfo {
            @Schema(description = "페이지 번호", example = "1")
            private Integer pageNumber;
            
            @Schema(description = "페이지 크기", example = "10")
            private Integer pageSize;
            
            @Schema(description = "총 페이지 수", example = "5")
            private Integer totalPages;
            
            @Schema(description = "총 항목 수", example = "48")
            private Long totalElements;
            
            @Schema(description = "첫 페이지 여부", example = "true")
            private Boolean first;
            
            @Schema(description = "마지막 페이지 여부", example = "false")
            private Boolean last;
        }
    }

    // === 홍보글 API 스키마들 ===
    
    @Data
    @Schema(description = "홍보글 목록 조회 성공 응답")
    public static class KokPostListSuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "체험콕 글 목록을 성공적으로 조회했습니다.")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "홍보글 목록")
        private java.util.List<KokPostItem> data;

        @Data
        @Schema(description = "홍보글 항목")
        public static class KokPostItem {
            @Schema(description = "홍보글 ID", example = "1")
            private Long id;
            
            @Schema(description = "제목", example = "맛있는 치킨집 체험 후기")
            private String title;
            
            @Schema(description = "조회수", example = "156")
            private Integer viewCount;
            
            @Schema(description = "캠페인 ID", example = "10")
            private Long campaignId;
            
            @Schema(description = "작성자 ID", example = "1")
            private Long authorId;
            
            @Schema(description = "작성자명", example = "관리자")
            private String authorName;
            
            @Schema(description = "연락처", example = "010-1234-5678")
            private String contactPhone;
            
            @Schema(description = "사업장 주소", example = "서울시 강남구")
            private String businessAddress;
            
            @Schema(description = "캠페인 오픈 여부", example = "true")
            private Boolean isCampaignOpen;
            
            @Schema(description = "생성 시간", example = "2025-08-27T10:30:00")
            private String createdAt;
            
            @Schema(description = "수정 시간", example = "2025-08-27T15:45:00")
            private String updatedAt;
        }
    }

    // === 유저 미션 이력 API 스키마들 ===
    
    @Data
    @Schema(description = "유저 미션 이력 조회 성공 응답 (클라이언트용)")
    public static class UserMissionHistorySuccessResponse {
        @Schema(description = "성공 여부", example = "true")
        private boolean success;
        
        @Schema(description = "응답 메시지", example = "유저 미션 이력 조회 성공")
        private String message;
        
        @Schema(description = "HTTP 상태 코드", example = "200")
        private int status;
        
        @Schema(description = "미션 이력 데이터")
        private UserMissionHistoryData data;

        @Data
        @Schema(description = "미션 이력 데이터")
        public static class UserMissionHistoryData {
            @Schema(description = "미션 이력 목록")
            private java.util.List<UserMissionHistoryItem> histories;
        }

        @Data
        @Schema(description = "사용자 미션 이력 항목")
        public static class UserMissionHistoryItem {
            @Schema(description = "미션 ID", example = "1")
            private Long id;
            
            @Schema(description = "캠페인 정보")
            private CampaignInfo campaign;
            
            @Schema(description = "미션 정보")
            private MissionInfo mission;
        }

        @Data
        @Schema(description = "캠페인 정보")
        public static class CampaignInfo {
            @Schema(description = "캠페인 제목", example = "이탈리안 레스토랑 신메뉴 체험단")
            private String title;
            
            @Schema(description = "캠페인 카테고리", example = "맛집")
            private String category;
        }

        @Data
        @Schema(description = "미션 정보")
        public static class MissionInfo {
            @Schema(description = "미션 URL", example = "https://instagram.com/p/abc123")
            private String missionUrl;
            
            @Schema(description = "완료 여부", example = "true")
            private Boolean isCompleted;
            
            @Schema(description = "완료 일시", example = "2024-03-16T10:30:00Z")
            private java.time.ZonedDateTime completionDate;
        }
    }
}
