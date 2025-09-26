package com.example.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 통합된 Swagger/OpenAPI 설정
 * - 기본 OpenAPI 설정 및 보안 스키마
 * - 공통 응답 스키마 및 예시
 * - 비즈니스 도메인 스키마
 * - API 그룹화 (V1, V2)
 */
@Configuration
@SuppressWarnings({"unchecked", "rawtypes"})
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(createComponents())
                .info(new Info()
                        .title("체험콕 API")
                        .version("2.0.0")
                        .description("체험콕 캠페인 플랫폼 API 문서"))
                .servers(List.of(
                        new io.swagger.v3.oas.models.servers.Server()
                                .url("https://chkok.kr")
                                .description("운영 서버"),
                        new io.swagger.v3.oas.models.servers.Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버")
                ));
    }

    /**
     * Components 통합 생성
     */
    private Components createComponents() {
        Components components = new Components();

        // 보안 스키마
        components.addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요 (Bearer 제외)"));

        // 공통 스키마
        addCommonSchemas(components);

        // 비즈니스 스키마
        addBusinessSchemas(components);

        // 공통 응답 정의
        addCommonResponses(components);

        // 공통 예시 정의
        addCommonExamples(components);

        // 공지사항 API 스키마 추가
        components.addSchemas("NoticeListSuccessResponse", createNoticeListSuccessResponseSchema());
        
        // 홍보글 API 스키마 추가
        components.addSchemas("KokPostListSuccessResponse", createKokPostListSuccessResponseSchema());

        // 캠페인 진행 상태 API 스키마 추가
        components.addSchemas("CampaignProgressSuccessResponse", createCampaignProgressSuccessResponseSchema());
        components.addSchemas("CampaignProgressResponse", createCampaignProgressResponseSchema());

        // 캠페인 생성 에러 스키마 추가
        components.addSchemas("CampaignCategoryErrorResponse", createCampaignCategoryErrorResponseSchema());
        components.addSchemas("CampaignCreationPermissionErrorResponse", createCampaignCreationPermissionErrorResponseSchema());

        return components;
    }

    /**
     * 공통 스키마 정의
     */
    private void addCommonSchemas(Components components) {
        // 기본 응답 스키마
        components.addSchemas("ApiSuccessResponse", createApiSuccessResponseSchema());
        components.addSchemas("ApiErrorResponse", createApiErrorResponseSchema());

        // 열거형 스키마
        components.addSchemas("ErrorCode", createErrorCodeSchema());
        // Gender와 UserRole은 TypeScript enum으로만 사용하므로 스키마 제거
        // components.addSchemas("Gender", createGenderSchema());
        // components.addSchemas("UserRole", createUserRoleSchema());
    }

    /**
     * 비즈니스 도메인 스키마 정의
     */
    private void addBusinessSchemas(Components components) {
        // V2 캠페인 관련 스키마
        components.addSchemas("CategoryV2", createCategoryV2Schema());
        components.addSchemas("CreateCampaignV2Request", createCampaignV2RequestSchema());
        components.addSchemas("CampaignListV2Response", createCampaignListV2ResponseSchema());

        // 사용자 관련 스키마
        components.addSchemas("UserUpdateRequest", createUserUpdateRequestSchema());
        components.addSchemas("FollowerCountUpdateRequest", createFollowerCountUpdateRequestSchema());
        components.addSchemas("NicknameUpdateRequest", createNicknameUpdateRequestSchema());
        components.addSchemas("ProfileImageUpdateRequest", createProfileImageUpdateRequestSchema());

        // 카테고리 및 회사 정보 (V1)
        components.addSchemas("CategoryInfo", createCategoryInfoSchema());
        components.addSchemas("CompanyInfo", createCompanyInfoSchema());

        // 캠페인 관련 (V1)
        components.addSchemas("CreateCampaignRequest", createCampaignRequestSchema());

        // 플랫폼 연동
        components.addSchemas("PlatformConnectRequest", createPlatformConnectRequestSchema());

        // 파일 업로드
        components.addSchemas("PresignedUrlRequest", createPresignedUrlRequestSchema());

        // 캠페인 신청
        components.addSchemas("CampaignApplicationRequest", createCampaignApplicationRequestSchema());

        // 인증 관련
        components.addSchemas("RefreshTokenRequest", createRefreshTokenRequestSchema());
        components.addSchemas("KakaoAuthRequest", createKakaoAuthRequestSchema());

        // 응답 스키마
        components.addSchemas("TokenData", createTokenDataSchema());
        components.addSchemas("TokenRefreshResponse", createTokenRefreshResponseSchema());
        components.addSchemas("LoginData", createLoginDataSchema());
        components.addSchemas("LoginSuccessResponse", createLoginSuccessResponseSchema());
        components.addSchemas("CampaignCreateResponse", createCampaignCreateResponseSchema());
        components.addSchemas("CampaignData", createCampaignDataSchema());
        components.addSchemas("UserInfo", createUserInfoSchema());

        // V2 전용 구체적인 응답 스키마
        components.addSchemas("CreateCampaignV2Response", createCampaignV2ResponseSchema());
        components.addSchemas("CampaignCreateV2SuccessResponse", createCampaignCreateV2SuccessResponseSchema());
        components.addSchemas("CampaignDetailV2SuccessResponse", createCampaignDetailV2SuccessResponseSchema());
        components.addSchemas("UserProfileSuccessResponse", createUserProfileSuccessResponseSchema());
        components.addSchemas("PlatformListSuccessResponse", createPlatformListSuccessResponseSchema());
        components.addSchemas("PlatformConnectSuccessResponse", createPlatformConnectSuccessResponseSchema());
        components.addSchemas("PresignedUrlSuccessResponse", createPresignedUrlSuccessResponseSchema());
        components.addSchemas("CampaignApplicationSuccessResponse", createCampaignApplicationSuccessResponseSchema());
        
        // 브랜드존 및 좋아요 API 스키마 추가
        components.addSchemas("BrandListSuccessResponse", createBrandListSuccessResponseSchema());
        components.addSchemas("BrandInfoSuccessResponse", createBrandInfoSuccessResponseSchema());
        components.addSchemas("LikeToggleSuccessResponse", createLikeToggleSuccessResponseSchema());
        components.addSchemas("MyLikedCampaignSuccessResponse", createMyLikedCampaignSuccessResponseSchema());
        components.addSchemas("CampaignApplicantsSuccessResponse", createCampaignApplicantsSuccessResponseSchema());
        components.addSchemas("CampaignApplicantResponse", createCampaignApplicantResponseSchema());
        components.addSchemas("CampaignApplicantListResponse", createCampaignApplicantListResponseSchema());
        components.addSchemas("AllPlatformListSuccessResponse", createAllPlatformListSuccessResponseSchema());
        components.addSchemas("CampaignSelectionSuccessResponse", createCampaignSelectionSuccessResponseSchema());
        components.addSchemas("MyApplicationsSuccessResponse", createMyApplicationsSuccessResponseSchema());
        
        // 내 캠페인 요약 유니온 타입 스키마
        components.addSchemas("UserCampaignSummaryResponse", createUserCampaignSummaryResponseSchema());
        components.addSchemas("ClientCampaignSummaryResponse", createClientCampaignSummaryResponseSchema());
        
        // V1 전용 구체적인 응답 스키마
        components.addSchemas("CampaignV1SuccessResponse", createCampaignV1SuccessResponseSchema());
        components.addSchemas("CampaignV1ListSuccessResponse", createCampaignV1ListSuccessResponseSchema());
        components.addSchemas("AutoCompleteSuccessResponse", createAutoCompleteSuccessResponseSchema());
        components.addSchemas("BannerImageSuccessResponse", createBannerImageSuccessResponseSchema());
        components.addSchemas("MyCampaignSuccessResponse", createMyCampaignSuccessResponseSchema());
        components.addSchemas("CampaignBasicInfoResponse", createCampaignBasicInfoResponseSchema());
        components.addSchemas("CampaignDetailInfoResponse", createCampaignDetailInfoResponseSchema());
        components.addSchemas("CampaignMissionGuideResponse", createCampaignMissionGuideResponseSchema());
        components.addSchemas("CampaignKeywordsResponse", createCampaignKeywordsResponseSchema());
        components.addSchemas("CampaignThumbnailResponse", createCampaignThumbnailResponseSchema());
        components.addSchemas("RealtimeSearchSuccessResponse", createRealtimeSearchSuccessResponseSchema());
        components.addSchemas("AutoCompleteSuggestionsSuccessResponse", createAutoCompleteSuggestionsSuccessResponseSchema());

        // 응답 스키마 정의 완료
    }

    /**
     * 공통 응답 정의
     */
    private void addCommonResponses(Components components) {
        components.addResponses("200", create200Response());
        components.addResponses("201", create201Response());
        components.addResponses("400", create400Response());
        components.addResponses("401", create401Response());
        components.addResponses("403", create403Response());
        components.addResponses("404", create404Response());
        components.addResponses("500", create500Response());
    }

    /**
     * 공통 예시 정의
     */
    private void addCommonExamples(Components components) {
        components.addExamples("SuccessExample", createSuccessExample());
        components.addExamples("CreatedExample", createCreatedExample());
        components.addExamples("BadRequestExample", createBadRequestExample());
        components.addExamples("UnauthorizedExample", createUnauthorizedExample());
        components.addExamples("ForbiddenExample", createForbiddenExample());
        components.addExamples("NotFoundExample", createNotFoundExample());
        components.addExamples("ServerErrorExample", createServerErrorExample());
    }

    // ========== 공통 스키마 생성 메서드 ==========

    private Schema<?> createApiSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("요청이 성공적으로 처리되었습니다."))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .description("응답 데이터"));
    }

    private Schema<Object> createApiErrorResponseSchema() {
        return new Schema<Object>()
                .type("object")
                .description("오류 응답")
                .addProperty("success", new Schema<Boolean>()
                        .type("boolean")
                        .example(false)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("오류 메시지")
                        .example("요청을 처리하는 중 오류가 발생했습니다."))
                .addProperty("errorCode", new Schema<String>()
                        .type("string")
                        .description("오류 코드")
                        .example("VALIDATION_ERROR")
                        ._enum(Arrays.asList("BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND",
                                "TOKEN_EXPIRED", "TOKEN_INVALID", "VALIDATION_ERROR",
                                "DUPLICATE_DATA", "INTERNAL_ERROR")))
                .addProperty("status", new Schema<Integer>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(400));
    }

    private Schema<?> createErrorCodeSchema() {
        return new Schema<>()
                .type("string")
                .description("오류 코드")
                ._enum(Arrays.asList(
                        "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND",
                        "TOKEN_EXPIRED", "TOKEN_INVALID", "VALIDATION_ERROR",
                        "DUPLICATE_DATA", "INTERNAL_ERROR"
                ));
    }

    // ========== 비즈니스 스키마 생성 메서드 ==========

    private Schema<?> createCategoryV2Schema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 카테고리 (OAS 표준 준수)")
                .addProperty("type", new StringSchema()
                        .description("카테고리 타입")
                        .example("VISIT")
                        ._enum(Arrays.asList("VISIT", "DELIVERY")))
                .addProperty("name", new StringSchema()
                        .description("카테고리 이름")
                        .example("CAFE")
                        ._enum(Arrays.asList("RESTAURANT", "CAFE", "BEAUTY", "ACCOMMODATION", "FOOD", "COSMETICS", "LIFESTYLE", "FASHION", "ACCESSORIES")))
                .required(Arrays.asList("type", "name"));
    }

    private Schema<?> createCampaignV2RequestSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 생성 요청")
                .addProperty("thumbnailUrl", new StringSchema()
                        .description("캠페인 썸네일 이미지 URL")
                        .example("https://example.com/images/cafe.jpg"))
                .addProperty("campaignType", new StringSchema()
                        .description("캠페인 타입")
                        .example("INSTAGRAM")
                        ._enum(Arrays.asList("INSTAGRAM", "BLOG", "YOUTUBE")))
                .addProperty("title", new StringSchema()
                        .description("캠페인 제목")
                        .example("Instagram Cafe Experience Campaign")
                        .maxLength(100))
                .addProperty("productShortInfo", new StringSchema()
                        .description("제품 간단 정보")
                        .example("Free signature drinks (2) and dessert (1)")
                        .maxLength(200))
                .addProperty("maxApplicants", new Schema<>()
                        .type("integer")
                        .description("최대 신청자 수")
                        .example(10)
                        .minimum(java.math.BigDecimal.valueOf(1))
                        .maximum(java.math.BigDecimal.valueOf(10000)))
                .addProperty("category", new Schema<>().$ref("#/components/schemas/CategoryV2"))
                .addProperty("companyInfo", new Schema<>().$ref("#/components/schemas/CompanyInfo"))
                .required(Arrays.asList("campaignType", "title", "productShortInfo", "maxApplicants", "category", "companyInfo"));
    }

    private Schema<?> createCampaignListV2ResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 목록 아이템 (V2)")
                .addProperty("id", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("캠페인 ID")
                        .example(123))
                .addProperty("title", new StringSchema()
                        .description("캠페인 제목")
                        .example("인스타 감성 카페 체험단 모집"))
                .addProperty("campaignType", new StringSchema()
                        .description("캠페인 타입")
                        .example("INSTAGRAM"))
                .addProperty("thumbnailUrl", new StringSchema()
                        .description("캠페인 썸네일 이미지 URL")
                        .example("https://example.com/images/cafe.jpg"))
                .addProperty("productShortInfo", new StringSchema()
                        .description("제품 간단 정보")
                        .example("시그니처 음료 2잔 + 디저트 1개 무료 제공"))
                .addProperty("maxApplicants", new Schema<>()
                        .type("integer")
                        .description("최대 신청자 수")
                        .example(10))
                .addProperty("currentApplicants", new Schema<>()
                        .type("integer")
                        .description("현재 신청자 수")
                        .example(8))
                .addProperty("recruitmentStartDate", new StringSchema()
                        .description("모집 시작일")
                        .example("2025-08-01"))
                .addProperty("recruitmentEndDate", new StringSchema()
                        .description("모집 종료일")
                        .example("2025-08-15"))
                .addProperty("applicationDeadlineDate", new StringSchema()
                        .description("신청 마감일")
                        .example("2025-08-14"))
                .addProperty("category", new Schema<>()
                        .type("object")
                        .description("캠페인 카테고리")
                        .addProperty("type", new StringSchema()
                                .description("카테고리 타입")
                                .example("VISIT"))
                        .addProperty("name", new StringSchema()
                                .description("카테고리 이름")
                                .example("CAFE")))
                .addProperty("approvalStatus", new StringSchema()
                        .description("승인 상태")
                        .example("APPROVED"));
    }

    private Schema<?> createUserUpdateRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("사용자 정보 수정 요청")
                .addProperty("nickname", new StringSchema()
                        .description("닉네임 (2~8자)")
                        .example("홍길동")
                        .minLength(2)
                        .maxLength(8))
                .addProperty("profileImage", new StringSchema()
                        .description("프로필 이미지 URL")
                        .example("https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg"))
                .addProperty("phone", new StringSchema()
                        .description("전화번호")
                        .example("010-1234-5678"))
                .addProperty("gender", new Schema<>()
                        .type("string")
                        .description("성별")
                        .example("MALE")
                        ._enum(Arrays.asList("MALE", "FEMALE", "UNKNOWN")))
                .addProperty("age", new Schema<>()
                        .type("integer")
                        .description("나이")
                        .example(30));
    }

    private Schema<?> createFollowerCountUpdateRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("팔로워 수 업데이트 요청")
                .addProperty("followerCount", new Schema<>()
                        .type("integer")
                        .description("팔로워/구독자 수")
                        .example(1000)
                        .minimum(java.math.BigDecimal.valueOf(0)))
                .required(Arrays.asList("followerCount"));
    }

    private Schema<?> createNicknameUpdateRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("닉네임 수정 요청")
                .addProperty("nickname", new StringSchema()
                        .description("새 닉네임")
                        .example("새닉네임")
                        .minLength(2)
                        .maxLength(8))
                .required(Arrays.asList("nickname"));
    }

    private Schema<?> createProfileImageUpdateRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("프로필 이미지 수정 요청")
                .addProperty("profileImage", new StringSchema()
                        .description("프로필 이미지 URL")
                        .example("https://drxgfm74s70w1.cloudfront.net/profile-images/new-image.jpg"))
                .required(Arrays.asList("profileImage"));
    }

    private Schema<?> createCategoryInfoSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 카테고리 정보")
                .addProperty("type", new StringSchema()
                        .description("카테고리 타입")
                        .example("방문")
                        ._enum(Arrays.asList("방문", "배송")))
                .addProperty("name", new StringSchema()
                        .description("카테고리명")
                        .example("카페"))
                .required(Arrays.asList("type", "name"));
    }

    private Schema<?> createCompanyInfoSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 주최 업체 정보")
                .addProperty("companyName", new StringSchema()
                        .description("업체명")
                        .example("맛있는 카페")
                        .maxLength(100))
                .addProperty("businessRegistrationNumber", new StringSchema()
                        .description("사업자등록번호")
                        .example("123-45-67890")
                        .maxLength(20))
                .addProperty("contactPerson", new StringSchema()
                        .description("담당자명")
                        .example("김담당")
                        .maxLength(50))
                .addProperty("phoneNumber", new StringSchema()
                        .description("연락처")
                        .example("010-1234-5678")
                        .maxLength(20))
                .required(Arrays.asList("companyName"));
    }

    private Schema<?> createCampaignRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 등록 요청")
                .addProperty("thumbnailUrl", new StringSchema()
                        .description("캠페인 썸네일 이미지 URL")
                        .example("https://example.com/images/campaign.jpg"))
                .addProperty("campaignType", new StringSchema()
                        .description("캠페인 진행 플랫폼")
                        .example("인스타그램")
                        ._enum(Arrays.asList("인스타그램", "블로그", "유튜브", "틱톡")))
                .addProperty("title", new StringSchema()
                        .description("캠페인 제목")
                        .example("인스타 감성 카페 체험단 모집")
                        .maxLength(200))
                .addProperty("category", new Schema<>().$ref("#/components/schemas/CategoryInfo"))
                .addProperty("companyInfo", new Schema<>().$ref("#/components/schemas/CompanyInfo"))
                .required(Arrays.asList("campaignType", "title", "maxApplicants", "category"));
    }


    private Schema<?> createPlatformConnectRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("SNS 플랫폼 연동 요청")
                .addProperty("type", new StringSchema()
                        .description("플랫폼 타입")
                        .example("INSTAGRAM")
                        ._enum(Arrays.asList("BLOG", "INSTAGRAM", "YOUTUBE")))
                .addProperty("url", new StringSchema()
                        .description("SNS 플랫폼 URL")
                        .example("https://www.instagram.com/username"))
                .required(Arrays.asList("type", "url"));
    }

    private Schema<?> createPresignedUrlRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("Presigned URL 생성 요청")
                .addProperty("fileExtension", new StringSchema()
                        .description("파일 확장자")
                        .example("jpg")
                        ._enum(Arrays.asList("jpg", "jpeg", "png")))
                .required(Arrays.asList("fileExtension"));
    }

    private Schema<?> createCampaignApplicationRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 신청 요청")
                .addProperty("campaignId", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("신청할 캠페인의 고유 식별자")
                        .example(42))
                .required(Arrays.asList("campaignId"));
    }

    private Schema<?> createRefreshTokenRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("토큰 재발급 요청")
                .addProperty("refreshToken", new StringSchema()
                        .description("리프레시 토큰")
                        .example("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
                .required(Arrays.asList("refreshToken"));
    }

    private Schema<?> createKakaoAuthRequestSchema() {
        return new Schema<>()
                .type("object")
                .description("카카오 로그인 요청")
                .addProperty("authorizationCode", new StringSchema()
                        .description("카카오 OAuth 인가 코드")
                        .example("0987654321abcdefghijk"))
                .addProperty("redirectUri", new StringSchema()
                        .description("리다이렉트 URI")
                        .example("http://localhost:3000/login/oauth2/code/kakao")
                        ._enum(Arrays.asList("http://localhost:3000/login/oauth2/code/kakao",
                                "https://chkok.kr/login/oauth2/code/kakao")))
                .required(Arrays.asList("authorizationCode", "redirectUri"));
    }

    private Schema<?> createTokenDataSchema() {
        return new Schema<>()
                .type("object")
                .description("토큰 데이터")
                .addProperty("accessToken", new StringSchema()
                        .description("액세스 토큰")
                        .example("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
                .addProperty("refreshToken", new StringSchema()
                        .description("리프레시 토큰")
                        .example("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."));
    }

    private Schema<?> createTokenRefreshResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("토큰 재발급 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("토큰이 성공적으로 재발급되었습니다."))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>().$ref("#/components/schemas/TokenData"));
    }

    private Schema<?> createLoginDataSchema() {
        return new Schema<>()
                .type("object")
                .description("로그인 데이터")
                .addProperty("loginType", new StringSchema()
                        .description("로그인 타입")
                        .example("login")
                        ._enum(Arrays.asList("login", "registration")))
                .addProperty("accessToken", new StringSchema()
                        .description("액세스 토큰")
                        .example("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
                .addProperty("refreshToken", new StringSchema()
                        .description("리프레시 토큰")
                        .example("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
                .addProperty("user", new Schema<>().$ref("#/components/schemas/UserInfo"));
    }

    private Schema<?> createLoginSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("로그인 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("카카오 로그인 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>().$ref("#/components/schemas/LoginData"));
    }

    private Schema<?> createCampaignCreateResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 생성 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("캠페인이 성공적으로 등록되었습니다."))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(201))
                .addProperty("data", new Schema<>().$ref("#/components/schemas/CampaignData"));
    }

    private Schema<?> createCampaignDataSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 데이터")
                .addProperty("id", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("캠페인 ID")
                        .example(123))
                .addProperty("title", new StringSchema()
                        .description("캠페인 제목")
                        .example("인스타 감성 카페 체험단 모집"))
                .addProperty("approvalStatus", new StringSchema()
                        .description("승인 상태")
                        .example("PENDING")
                        ._enum(Arrays.asList("PENDING", "APPROVED", "REJECTED")))
                .addProperty("createdAt", new StringSchema()
                        .description("생성일시")
                        .example("2025-07-10T15:30:00Z"));
    }

    private Schema<?> createUserInfoSchema() {
        return new Schema<>()
                .type("object")
                .description("사용자 정보 - 로그인 및 프로필 조회 시 반환되는 사용자 데이터")
                .addProperty("id", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("사용자 고유 식별자 - 시스템에서 사용자를 구분하는 유일한 ID")
                        .example(123))
                .addProperty("email", new StringSchema()
                        .description("이메일 주소 - 로그인 및 알림 발송에 사용되는 사용자의 이메일")
                        .example("user@example.com"))
                .addProperty("nickname", new StringSchema()
                        .description("닉네임 - 플랫폼 내에서 표시되는 사용자의 별명 (2~8자)")
                        .example("홍길동"))
                .addProperty("profileImg", new StringSchema()
                        .description("프로필 이미지 URL - 사용자의 프로필 사진 (CloudFront CDN을 통해 제공)")
                        .example("https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg"))
                .addProperty("phone", new StringSchema()
                        .description("전화번호 - 캠페인 선정 시 연락용으로 사용되는 연락처 (선택사항)")
                        .example("010-1234-5678"))
                .addProperty("gender", new Schema<>()
                        .type("string")
                        .description("성별 - 통계 분석 및 맞춤형 캠페인 추천용 (선택사항)")
                        .example("MALE")
                        ._enum(Arrays.asList("MALE", "FEMALE", "UNKNOWN")))
                .addProperty("age", new Schema<>()
                        .type("integer")
                        .description("나이 - 연령대별 캠페인 타겟팅 및 통계 분석용 (만 나이 기준, 선택사항)")
                        .example(30))
                .addProperty("role", new StringSchema()
                        .description("사용자 권한 - 플랫폼 내 역할과 접근 권한을 나타냄")
                        .example("USER")
                        ._enum(Arrays.asList("USER", "CLIENT", "ADMIN")));
    }

    // ========== V2 전용 구체적인 응답 스키마 ==========

    private Schema<?> createCampaignV2ResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 생성/수정 응답 (V2 - OAS 표준)")
                .addProperty("id", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("캠페인 ID")
                        .example(12345))
                .addProperty("thumbnailUrl", new StringSchema()
                        .description("캠페인 썸네일 이미지 URL")
                        .example("https://example.com/images/campaign-thumbnail.jpg"))
                .addProperty("campaignType", new StringSchema()
                        .description("캠페인 타입 (OAS 표준)")
                        .example("INSTAGRAM")
                        ._enum(Arrays.asList("INSTAGRAM", "BLOG", "YOUTUBE")))
                .addProperty("title", new StringSchema()
                        .description("캠페인 제목")
                        .example("Instagram Cafe Experience Campaign"))
                .addProperty("productShortInfo", new StringSchema()
                        .description("제품 간단 정보")
                        .example("Free signature drinks (2) and dessert (1)"))
                .addProperty("maxApplicants", new Schema<>()
                        .type("integer")
                        .description("최대 신청자 수")
                        .example(10))
                .addProperty("productDetails", new StringSchema()
                        .description("제품 상세 정보")
                        .example("Experience signature drinks and desserts at an Instagram-worthy cafe..."))
                .addProperty("recruitmentStartDate", new StringSchema()
                        .description("모집 시작일")
                        .format("date")
                        .example("2025-08-01"))
                .addProperty("recruitmentEndDate", new StringSchema()
                        .description("모집 종료일")
                        .format("date")
                        .example("2025-08-15"))
                .addProperty("applicationDeadlineDate", new StringSchema()
                        .description("신청 마감일")
                        .format("date")
                        .example("2025-08-14"))
                .addProperty("selectionDate", new StringSchema()
                        .description("참여자 선정일")
                        .format("date")
                        .example("2025-08-16"))
                .addProperty("reviewDeadlineDate", new StringSchema()
                        .description("리뷰 제출 마감일")
                        .format("date")
                        .example("2025-08-30"))
                .addProperty("selectionCriteria", new StringSchema()
                        .description("선정 기준")
                        .example("Instagram followers 1000+, cafe review experience required"))
                .addProperty("missionGuide", new StringSchema()
                        .description("미션 가이드")
                        .example("1. Visit the cafe and inform staff you're in the experience group..."))
                .addProperty("missionKeywords", new Schema<>()
                        .type("array")
                        .description("미션 키워드")
                        .items(new StringSchema().example("cafe_recommendation"))
                        .example(Arrays.asList("cafe_recommendation", "dessert_hotspot", "gangnam_cafe")))
                .addProperty("category", new Schema<>()
                        .type("object")
                        .description("캠페인 카테고리 (OAS 표준)")
                        .addProperty("id", new Schema<>()
                                .type("integer")
                                .format("int64")
                                .description("카테고리 ID")
                                .example(1))
                        .addProperty("type", new StringSchema()
                                .description("카테고리 타입 (OAS 표준)")
                                .example("VISIT")
                                ._enum(Arrays.asList("VISIT", "DELIVERY")))
                        .addProperty("name", new StringSchema()
                                .description("카테고리 이름")
                                .example("CAFE"))
                        .addProperty("createdAt", new StringSchema()
                                .description("생성 일시")
                                .format("date-time")
                                .example("2025-05-15T10:30:00.000+09:00"))
                        .addProperty("updatedAt", new StringSchema()
                                .description("수정 일시")
                                .format("date-time")
                                .example("2025-05-15T10:30:00.000+09:00")))
                .addProperty("companyInfo", new Schema<>()
                        .type("object")
                        .description("회사 정보 (OAS 표준)")
                        .addProperty("id", new Schema<>()
                                .type("integer")
                                .format("int64")
                                .description("회사 ID")
                                .example(1))
                        .addProperty("companyName", new StringSchema()
                                .description("회사명")
                                .example("Delicious Cafe Co."))
                        .addProperty("businessRegistrationNumber", new StringSchema()
                                .description("사업자등록번호")
                                .example("123-45-67890"))
                        .addProperty("contactPerson", new StringSchema()
                                .description("담당자명")
                                .example("John Smith"))
                        .addProperty("phoneNumber", new StringSchema()
                                .description("연락처")
                                .example("010-1234-5678"))
                        .addProperty("createdAt", new StringSchema()
                                .description("생성 일시")
                                .format("date-time")
                                .example("2025-05-15T10:30:00.000+09:00"))
                        .addProperty("updatedAt", new StringSchema()
                                .description("수정 일시")
                                .format("date-time")
                                .example("2025-05-15T10:30:00.000+09:00")))
                .addProperty("approvalStatus", new StringSchema()
                        .description("승인 상태 (OAS 표준)")
                        .example("PENDING")
                        ._enum(Arrays.asList("PENDING", "APPROVED", "REJECTED")))
                .addProperty("createdAt", new StringSchema()
                        .description("생성 일시")
                        .format("date-time")
                        .example("2025-07-10T15:30:00.000+09:00"))
                .addProperty("updatedAt", new StringSchema()
                        .description("수정 일시")
                        .format("date-time")
                        .example("2025-07-10T15:30:00.000+09:00"));
    }

    private Schema<?> createCampaignCreateV2SuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 생성 성공 응답 (V2)")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("캠페인이 성공적으로 등록되었습니다."))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(201))
                .addProperty("data", new Schema<>()
                        .$ref("#/components/schemas/CreateCampaignV2Response")
                        .description("생성된 캠페인 데이터"));
    }

    private Schema<?> createCampaignDetailV2SuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 상세 조회 성공 응답 (V2)")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("캠페인 상세 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .$ref("#/components/schemas/CreateCampaignV2Response")
                        .description("캠페인 상세 데이터"));
    }

    private Schema<?> createUserProfileSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("사용자 프로필 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("사용자 프로필 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .$ref("#/components/schemas/UserInfo")
                        .description("사용자 정보"));
    }

    private Schema<?> createPlatformListSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("SNS 플랫폼 목록 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("SNS 플랫폼 목록 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("플랫폼 목록 데이터")
                        .addProperty("platforms", new Schema<>()
                                .type("array")
                                .description("연동된 플랫폼 목록")
                                .items(new Schema<>()
                                        .type("object")
                                        .addProperty("id", new Schema<>().type("integer").description("플랫폼 ID"))
                                        .addProperty("platformType", new StringSchema().description("플랫폼 타입"))
                                        .addProperty("accountUrl", new StringSchema().description("계정 URL"))
                                        .addProperty("followerCount", new Schema<>().type("integer").description("팔로워 수"))
                                        .addProperty("lastCrawledAt", new StringSchema().description("마지막 크롤링 시간")))));
    }

    private Schema<?> createPlatformConnectSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("SNS 플랫폼 연동 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("SNS 플랫폼 연동 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("연동 결과 데이터")
                        .addProperty("platformId", new Schema<>().type("integer").description("생성된 플랫폼 ID"))
                        .addProperty("platformType", new StringSchema().description("플랫폼 타입"))
                        .addProperty("message", new StringSchema().description("연동 완료 메시지")));
    }

    private Schema<?> createPresignedUrlSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("Presigned URL 생성 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("이미지 업로드용 URL이 성공적으로 생성되었습니다."))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("Presigned URL 데이터")
                        .addProperty("presignedUrl", new StringSchema().description("생성된 Presigned URL")));
    }

    private Schema<?> createCampaignApplicationSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 신청 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("캠페인 신청이 완료되었습니다."))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(201))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("신청 결과 데이터")
                        .addProperty("applicationId", new Schema<>().type("integer").description("신청 ID"))
                        .addProperty("campaignId", new Schema<>().type("integer").description("캠페인 ID"))
                        .addProperty("status", new StringSchema().description("신청 상태"))
                        .addProperty("appliedAt", new StringSchema().description("신청 일시")));
    }

    // ========== V1 전용 구체적인 응답 스키마 ==========

    private Schema<?> createCampaignV1SuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 생성/수정 성공 응답 (V1)")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("캠페인이 성공적으로 등록되었습니다."))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(201))
                .addProperty("data", new Schema<>()
                        .$ref("#/components/schemas/CampaignData")
                        .description("캠페인 데이터"));
    }

    private Schema<?> createCampaignV1ListSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 목록 조회 성공 응답 (V1)")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("캠페인 목록 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("캠페인 목록 데이터")
                        .addProperty("campaigns", new Schema<>()
                                .type("array")
                                .description("캠페인 목록")
                                .items(new Schema<>()
                                        .type("object")
                                        .addProperty("id", new Schema<>().type("integer").description("캠페인 ID"))
                                        .addProperty("title", new StringSchema().description("캠페인 제목"))
                                        .addProperty("campaignType", new StringSchema().description("캠페인 타입"))
                                        .addProperty("thumbnailUrl", new StringSchema().description("썸네일 URL"))
                                        .addProperty("productShortInfo", new StringSchema().description("제품 간단 정보"))
                                        .addProperty("maxApplicants", new Schema<>().type("integer").description("최대 신청자 수"))
                                        .addProperty("currentApplicants", new Schema<>().type("integer").description("현재 신청자 수"))
                                        .addProperty("recruitmentStartDate", new StringSchema().description("모집 시작일"))
                                        .addProperty("recruitmentEndDate", new StringSchema().description("모집 종료일"))
                                        .addProperty("applicationDeadlineDate", new StringSchema().description("신청 마감일"))
                                        .addProperty("category", new Schema<>()
                                                .type("object")
                                                .addProperty("type", new StringSchema().description("카테고리 타입"))
                                                .addProperty("name", new StringSchema().description("카테고리명")))))
                        .addProperty("pagination", new Schema<>()
                                .type("object")
                                .description("페이징 정보")
                                .addProperty("pageNumber", new Schema<>()
                                        .type("integer")
                                        .description("현재 페이지 번호")
                                        .example(1))
                                .addProperty("pageSize", new Schema<>()
                                        .type("integer")
                                        .description("페이지 크기")
                                        .example(10))
                                .addProperty("totalPages", new Schema<>()
                                        .type("integer")
                                        .description("총 페이지 수")
                                        .example(2))
                                .addProperty("totalElements", new Schema<>()
                                        .type("integer")
                                        .description("총 요소 수")
                                        .example(11))
                                .addProperty("first", new Schema<>()
                                        .type("boolean")
                                        .description("첫 페이지 여부")
                                        .example(true))
                                .addProperty("last", new Schema<>()
                                        .type("boolean")
                                        .description("마지막 페이지 여부")
                                        .example(false))));
    }

    private Schema<?> createAutoCompleteSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("자동완성 검색 성공 응답 (기존 호환성 유지)")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("자동완성 검색 결과"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("검색 결과 데이터")
                        .addProperty("suggestions", new Schema<>()
                                .type("array")
                                .description("검색 제안 목록")
                                .items(new StringSchema().description("검색 키워드")))
                        .addProperty("trending", new Schema<>()
                                .type("array")
                                .description("인기 검색어 목록")
                                .items(new StringSchema().description("인기 키워드"))));
    }

    private Schema<?> createBannerImageSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("배너 이미지 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("배너 이미지 목록 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("array")
                        .description("배너 이미지 목록")
                        .items(new Schema<>()
                                .type("object")
                                .addProperty("id", new Schema<>()
                                        .type("integer")
                                        .format("int64")
                                        .description("배너 ID")
                                        .example(1))
                                .addProperty("bannerUrl", new StringSchema()
                                        .description("배너 이미지 URL")
                                        .example("https://ckokservice.s3.ap-northeast-2.amazonaws.com/original-images/KakaoTalk_20250610_155803395.png"))
                                .addProperty("redirectUrl", new StringSchema()
                                        .description("클릭 시 이동할 URL")
                                        .example("https:"))));
    }

    private Schema<?> createMyCampaignSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("내 캠페인 요약 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("내 캠페인 요약 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .description("역할별 캠페인 요약 데이터")
                        .oneOf(Arrays.asList(
                                new Schema<>().$ref("#/components/schemas/UserCampaignSummaryResponse"),
                                new Schema<>().$ref("#/components/schemas/ClientCampaignSummaryResponse")
                        )));
    }

    // ========== 공통 응답 생성 메서드 ==========

    private ApiResponse create200Response() {
        return new ApiResponse()
                .description("요청 성공")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiSuccessResponse"))
                                .addExamples("default", new Example().$ref("#/components/examples/SuccessExample"))));
    }

    private ApiResponse create201Response() {
        return new ApiResponse()
                .description("리소스 생성 성공")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiSuccessResponse"))
                                .addExamples("default", new Example().$ref("#/components/examples/CreatedExample"))));
    }

    private ApiResponse create400Response() {
        return new ApiResponse()
                .description("잘못된 요청")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))
                                .addExamples("default", new Example().$ref("#/components/examples/BadRequestExample"))));
    }

    private ApiResponse create401Response() {
        return new ApiResponse()
                .description("인증 실패")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))
                                .addExamples("default", new Example().$ref("#/components/examples/UnauthorizedExample"))));
    }

    private ApiResponse create403Response() {
        return new ApiResponse()
                .description("권한 없음")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))
                                .addExamples("default", new Example().$ref("#/components/examples/ForbiddenExample"))));
    }

    private ApiResponse create404Response() {
        return new ApiResponse()
                .description("리소스를 찾을 수 없음")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))
                                .addExamples("default", new Example().$ref("#/components/examples/NotFoundExample"))));
    }

    private ApiResponse create500Response() {
        return new ApiResponse()
                .description("서버 내부 오류")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))
                                .addExamples("default", new Example().$ref("#/components/examples/ServerErrorExample"))));
    }

    // ========== 공통 예시 생성 메서드 ==========

    private Example createSuccessExample() {
        return new Example()
                .summary("성공 응답 예시")
                .description("요청이 성공적으로 처리된 경우")
                .value("""
                        {
                          "success": true,
                          "message": "요청이 성공적으로 처리되었습니다.",
                          "status": 200,
                          "data": {
                            "id": 123,
                            "title": "인스타 감성 카페 체험단 모집",
                            "status": "ACTIVE",
                            "createdAt": "2025-07-10T15:30:00Z"
                          }
                        }
                        """);
    }

    // ========== 공지사항 및 체험콕 아티클 API 스키마 생성 메서드 ==========

    private Schema<?> createNoticeListSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("공지사항 목록 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("공지사항 목록을 성공적으로 조회했습니다."))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("공지사항 목록 데이터")
                        .addProperty("notices", new Schema<>()
                                .type("array")
                                .description("공지사항 목록")
                                .items(new Schema<>()
                                        .type("object")
                                        .addProperty("id", new Schema<>()
                                                .type("integer")
                                                .format("int64")
                                                .description("공지사항 ID")
                                                .example(1))
                                        .addProperty("title", new StringSchema()
                                                .description("제목")
                                                .example("중요한 공지사항입니다"))
                                        .addProperty("viewCount", new Schema<>()
                                                .type("integer")
                                                .description("조회수")
                                                .example(156))
                                        .addProperty("isMustRead", new Schema<>()
                                                .type("boolean")
                                                .description("필독 여부")
                                                .example(true))
                                        .addProperty("authorId", new Schema<>()
                                                .type("integer")
                                                .format("int64")
                                                .description("작성자 ID")
                                                .example(1))
                                        .addProperty("authorName", new StringSchema()
                                                .description("작성자명")
                                                .example("관리자"))
                                        .addProperty("createdAt", new StringSchema()
                                                .description("생성 시간")
                                                .example("2025-08-27"))
                                        .addProperty("updatedAt", new StringSchema()
                                                .description("수정 시간")
                                                .example("2025-08-27"))))
                        .addProperty("pagination", new Schema<>()
                                .type("object")
                                .description("페이징 정보")
                                .addProperty("pageNumber", new Schema<>()
                                        .type("integer")
                                        .description("페이지 번호")
                                        .example(1))
                                .addProperty("pageSize", new Schema<>()
                                        .type("integer")
                                        .description("페이지 크기")
                                        .example(10))
                                .addProperty("totalPages", new Schema<>()
                                        .type("integer")
                                        .description("총 페이지 수")
                                        .example(5))
                                .addProperty("totalElements", new Schema<>()
                                        .type("integer")
                                        .format("int64")
                                        .description("총 항목 수")
                                        .example(48))
                                .addProperty("first", new Schema<>()
                                        .type("boolean")
                                        .description("첫 페이지 여부")
                                        .example(true))
                                .addProperty("last", new Schema<>()
                                        .type("boolean")
                                        .description("마지막 페이지 여부")
                                        .example(false))));
    }

    private Schema<?> createKokPostListSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("홍보글 목록 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("체험콕 글 목록을 성공적으로 조회했습니다."))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("array")
                        .description("홍보글 목록")
                        .items(new Schema<>()
                                .type("object")
                                .addProperty("id", new Schema<>()
                                        .type("integer")
                                        .format("int64")
                                        .description("홍보글 ID")
                                        .example(1))
                                .addProperty("title", new StringSchema()
                                        .description("제목")
                                        .example("맛있는 치킨집 체험 후기"))
                                .addProperty("viewCount", new Schema<>()
                                        .type("integer")
                                        .description("조회수")
                                        .example(156))
                                .addProperty("campaignId", new Schema<>()
                                        .type("integer")
                                        .format("int64")
                                        .description("캠페인 ID")
                                        .example(10))
                                .addProperty("authorId", new Schema<>()
                                        .type("integer")
                                        .format("int64")
                                        .description("작성자 ID")
                                        .example(1))
                                .addProperty("authorName", new StringSchema()
                                        .description("작성자명")
                                        .example("관리자"))
                                .addProperty("contactPhone", new StringSchema()
                                        .description("연락처")
                                        .example("010-1234-5678"))
                                .addProperty("businessAddress", new StringSchema()
                                        .description("사업장 주소")
                                        .example("서울시 강남구"))
                                .addProperty("isCampaignOpen", new Schema<>()
                                        .type("boolean")
                                        .description("캠페인 오픈 여부")
                                        .example(true))
                                .addProperty("createdAt", new StringSchema()
                                        .description("생성 시간")
                                        .example("2025-08-27"))
                                .addProperty("updatedAt", new StringSchema()
                                        .description("수정 시간")
                                        .example("2025-08-27"))));
    }

    private Example createCreatedExample() {
        return new Example()
                .summary("생성 성공 응답 예시")
                .description("리소스가 성공적으로 생성된 경우")
                .value("""
                        {
                          "success": true,
                          "message": "캠페인이 성공적으로 등록되었습니다.",
                          "status": 201,
                          "data": {
                            "id": 12345,
                            "title": "Instagram Cafe Experience Campaign",
                            "campaignType": "INSTAGRAM",
                            "approvalStatus": "PENDING",
                            "maxApplicants": 10,
                            "currentApplicants": 0,
                            "recruitmentStartDate": "2025-08-01",
                            "recruitmentEndDate": "2025-08-15",
                            "createdAt": "2025-07-10"
                          }
                        }
                        """);
    }

    private Example createBadRequestExample() {
        return new Example()
                .summary("잘못된 요청 예시")
                .description("요청 데이터가 유효하지 않은 경우")
                .value("""
                        {
                          "success": false,
                          "message": "제목은 필수 입력값입니다.",
                          "errorCode": "VALIDATION_ERROR",
                          "status": 400
                        }
                        """);
    }


    private Example createUnauthorizedExample() {
        return new Example()
                .summary("인증 실패 예시")
                .description("유효하지 않은 토큰이거나 토큰이 만료된 경우")
                .value("""
                        {
                          "success": false,
                          "message": "만료된 토큰입니다.",
                          "errorCode": "TOKEN_EXPIRED",
                          "status": 401
                        }
                        """);
    }

    private Example createForbiddenExample() {
        return new Example()
                .summary("권한 없음 예시")
                .description("해당 작업을 수행할 권한이 없는 경우")
                .value("""
                        {
                          "success": false,
                          "message": "CLIENT 권한이 필요합니다.",
                          "errorCode": "INSUFFICIENT_PERMISSION",
                          "status": 403
                        }
                        """);
    }



    private Example createNotFoundExample() {
        return new Example()
                .summary("리소스 없음 예시")
                .description("요청한 리소스를 찾을 수 없는 경우")
                .value("""
                        {
                          "success": false,
                          "message": "캠페인을 찾을 수 없습니다.",
                          "errorCode": "CAMPAIGN_NOT_FOUND",
                          "status": 404
                        }
                        """);
    }


    private Example createServerErrorExample() {
        return new Example()
                .summary("서버 오류 예시")
                .description("서버 내부에서 오류가 발생한 경우")
                .value("""
                        {
                          "success": false,
                          "message": "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                          "errorCode": "INTERNAL_ERROR",
                          "status": 500
                        }
                        """);
    }

    // ========== 내 캠페인 요약 유니온 타입 스키마 생성 메서드 ==========

    private Schema<?> createUserCampaignSummaryResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("USER 역할의 캠페인 요약 정보 응답")
                .addProperty("role", new StringSchema()
                        .description("사용자 역할")
                        .example("USER")
                        ._enum(Arrays.asList("USER")))
                .addProperty("summary", new Schema<>()
                        .type("object")
                        .description("인플루언서 신청 요약 정보")
                        .addProperty("applied", new Schema<>()
                                .type("object")
                                .description("총 지원한 신청")
                                .addProperty("count", new Schema<>()
                                        .type("integer")
                                        .example(1)
                                        .description("총 지원 신청 수"))
                                .addProperty("label", new StringSchema()
                                        .example("지원")
                                        .description("상태 레이블")))
                        .addProperty("pending", new Schema<>()
                                .type("object")
                                .description("대기중인 신청")
                                .addProperty("count", new Schema<>()
                                        .type("integer")
                                        .example(1)
                                        .description("대기중인 신청 수"))
                                .addProperty("label", new StringSchema()
                                        .example("대기중")
                                        .description("상태 레이블")))
                        .addProperty("selected", new Schema<>()
                                .type("object")
                                .description("선정된 신청")
                                .addProperty("count", new Schema<>()
                                        .type("integer")
                                        .example(0)
                                        .description("선정된 신청 수"))
                                .addProperty("label", new StringSchema()
                                        .example("선정")
                                        .description("상태 레이블")))
                        .addProperty("completed", new Schema<>()
                                .type("object")
                                .description("완료된 신청")
                                .addProperty("count", new Schema<>()
                                        .type("integer")
                                        .example(0)
                                        .description("완료된 신청 수"))
                                .addProperty("label", new StringSchema()
                                        .example("완료")
                                        .description("상태 레이블"))));
    }

    private Schema<?> createClientCampaignSummaryResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("CLIENT 역할의 캠페인 요약 정보 응답")
                .addProperty("role", new StringSchema()
                        .description("사용자 역할")
                        .example("CLIENT")
                        ._enum(Arrays.asList("CLIENT")))
                .addProperty("summary", new Schema<>()
                        .type("object")
                        .description("기업 캠페인 요약 정보")
                        .addProperty("pending", new Schema<>()
                                .type("object")
                                .description("관리자 승인 대기중인 캠페인")
                                .addProperty("count", new Schema<>()
                                        .type("integer")
                                        .example(0)
                                        .description("대기중인 캠페인 수"))
                                .addProperty("label", new StringSchema()
                                        .example("대기중")
                                        .description("상태 레이블")))
                        .addProperty("approved", new Schema<>()
                                .type("object")
                                .description("승인되어 활성화된 캠페인")
                                .addProperty("count", new Schema<>()
                                        .type("integer")
                                        .example(14)
                                        .description("승인된 캠페인 수"))
                                .addProperty("label", new StringSchema()
                                        .example("승인됨")
                                        .description("상태 레이블")))
                        .addProperty("rejected", new Schema<>()
                                .type("object")
                                .description("관리자가 거절한 캠페인")
                                .addProperty("count", new Schema<>()
                                        .type("integer")
                                        .example(0)
                                        .description("거절된 캠페인 수"))
                                .addProperty("label", new StringSchema()
                                        .example("거절됨")
                                        .description("상태 레이블")))
                        .addProperty("expired", new Schema<>()
                                .type("object")
                                .description("승인됐지만 신청기간이 종료된 캠페인")
                                .addProperty("count", new Schema<>()
                                        .type("integer")
                                        .example(0)
                                        .description("만료된 캠페인 수"))
                                .addProperty("label", new StringSchema()
                                        .example("만료됨")
                                        .description("상태 레이블"))));
    }

    // ========== 문서에서 요구하는 추가 스키마 메서드들 ==========

    private Schema<?> createCampaignBasicInfoResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 기본 정보 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new Schema<>()
                        .type("string")
                        .example("캠페인 기본 정보 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("캠페인 기본 정보")
                        .addProperty("campaignId", new Schema<>()
                                .type("integer")
                                .example(22)
                                .description("캠페인 ID"))
                        .addProperty("campaignType", new Schema<>()
                                .type("string")
                                .example("인스타그램")
                                .description("캠페인 타입"))
                        .addProperty("categoryType", new Schema<>()
                                .type("string")
                                .example("배송")
                                .description("카테고리 타입"))
                        .addProperty("categoryName", new Schema<>()
                                .type("string")
                                .example("식품")
                                .description("카테고리명"))
                        .addProperty("title", new Schema<>()
                                .type("string")
                                .example("오렌지를 먹은지 얼마나 오렌지")
                                .description("캠페인 제목"))
                        .addProperty("maxApplicants", new Schema<>()
                                .type("integer")
                                .example(15)
                                .description("최대 신청자 수"))
                        .addProperty("currentApplicants", new Schema<>()
                                .type("integer")
                                .example(0)
                                .description("현재 신청자 수"))
                        .addProperty("recruitmentStartDate", new Schema<>()
                                .type("string")
                                .format("date")
                                .example("2025-06-06")
                                .description("모집 시작일"))
                        .addProperty("recruitmentEndDate", new Schema<>()
                                .type("string")
                                .format("date")
                                .example("2027-12-12")
                                .description("모집 종료일")));
    }

    private Schema<?> createCampaignDetailInfoResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 상세 정보 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new Schema<>()
                        .type("string")
                        .example("캠페인 상세 정보 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("캠페인 상세 정보")
                        .addProperty("campaignId", new Schema<>()
                                .type("integer")
                                .example(22)
                                .description("캠페인 ID"))
                        .addProperty("productShortInfo", new Schema<>()
                                .type("string")
                                .example("오렌지 5개")
                                .description("제품 간단 정보"))
                        .addProperty("productDetails", new Schema<>()
                                .type("string")
                                .example("오렌지 5개")
                                .description("제품 상세 정보"))
                        .addProperty("selectionCriteria", new Schema<>()
                                .type("string")
                                .example("팔로워 1000명")
                                .description("선정 기준"))
                        .addProperty("reviewDeadlineDate", new Schema<>()
                                .type("string")
                                .format("date")
                                .example("2027-12-20")
                                .description("리뷰 마감일"))
                        .addProperty("selectionDate", new Schema<>()
                                .type("string")
                                .format("date")
                                .example("2027-12-13")
                                .description("선정일"))
                        .addProperty("applicationDeadlineDate", new Schema<>()
                                .type("string")
                                .format("date")
                                .example("2027-12-12")
                                .description("신청 마감일")));
    }

    private Schema<?> createCampaignMissionGuideResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 미션 가이드 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new Schema<>()
                        .type("string")
                        .example("캠페인 미션 가이드 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("캠페인 미션 가이드")
                        .addProperty("campaignId", new Schema<>()
                                .type("integer")
                                .example(22)
                                .description("캠페인 ID"))
                        .addProperty("missionGuide", new Schema<>()
                                .type("string")
                                .example("ㅁㅇㄹㅁㅇㄹ")
                                .description("미션 가이드")));
    }

    private Schema<?> createCampaignKeywordsResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 키워드 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new Schema<>()
                        .type("string")
                        .example("캠페인 필수 키워드 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("캠페인 키워드")
                        .addProperty("campaignId", new Schema<>()
                                .type("integer")
                                .example(22)
                                .description("캠페인 ID"))
                        .addProperty("missionKeywords", new Schema<>()
                                .type("array")
                                .items(new Schema<>()
                                        .type("string")
                                        .example("오렌지"))
                                .description("미션 키워드 목록")));
    }

    private Schema<?> createCampaignThumbnailResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 썸네일 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new Schema<>()
                        .type("string")
                        .example("캠페인 썸네일 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("캠페인 썸네일 정보")
                        .addProperty("campaignId", new Schema<>()
                                .type("integer")
                                .example(22)
                                .description("캠페인 ID"))
                        .addProperty("thumbnailUrl", new Schema<>()
                                .type("string")
                                .example("https://drxgfm74s70w1.cloudfront.net/campaign-images/1749055404594-9eca8cb4-7a93-4f69-8402-2d70ea1aac93.jpg")
                                .description("썸네일 이미지 URL")));
    }

    private Schema<?> createRealtimeSearchSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("실시간 인기 검색어 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("인기 검색어 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("인기 검색어 데이터")
                        .addProperty("suggestions", new Schema<>()
                                .type("array")
                                .items(new Schema<>()
                                        .type("string")
                                        .example("부티크 호텔 1박 체험단"))
                                .description("실시간 인기 검색어 목록")));
    }

    private Schema<?> createAutoCompleteSuggestionsSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("검색 자동완성 제안 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("자동완성 제안 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("자동완성 제안 데이터")
                        .addProperty("suggestions", new Schema<>()
                                .type("array")
                                .items(new Schema<>()
                                        .type("string")
                                        .example("이탈리안 레스토랑 신메뉴 체험단"))
                                .description("검색 자동완성 제안 목록")));
    }

    // ========== 브랜드존 및 좋아요 API 스키마 생성 메소드 ==========

    private Schema<?> createBrandListSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("브랜드 목록 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("브랜드 목록 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .addProperty("content", new Schema<>()
                                .type("array")
                                .items(new Schema<>()
                                        .type("object")
                                        .addProperty("brandId", new Schema<>().type("integer").example(1))
                                        .addProperty("brandName", new StringSchema().example("ABC 코스메틱"))
                                        .addProperty("createdAt", new StringSchema().example("2024-01-15T10:30:00Z"))
                                        .addProperty("totalCampaigns", new Schema<>().type("integer").example(15))
                                        .addProperty("activeCampaigns", new Schema<>().type("integer").example(3))))
                        .addProperty("pageNumber", new Schema<>().type("integer").example(1))
                        .addProperty("totalElements", new Schema<>().type("integer").example(58)));
    }

    private Schema<?> createBrandInfoSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("브랜드 정보 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("브랜드 정보 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .addProperty("brandId", new Schema<>().type("integer").example(1))
                        .addProperty("brandName", new StringSchema().example("ABC 코스메틱"))
                        .addProperty("contactPerson", new StringSchema().example("김담당"))
                        .addProperty("phoneNumber", new StringSchema().example("02-1234-5678"))
                        .addProperty("totalCampaigns", new Schema<>().type("integer").example(15))
                        .addProperty("activeCampaigns", new Schema<>().type("integer").example(3)));
    }

    private Schema<?> createLikeToggleSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("좋아요 토글 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("좋아요가 추가되었습니다"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .addProperty("liked", new Schema<>().type("boolean").example(true))
                        .addProperty("totalCount", new Schema<>().type("integer").example(43))
                        .addProperty("campaignId", new Schema<>().type("integer").example(123)));
    }

    private Schema<?> createMyLikedCampaignSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("내가 좋아요한 캠페인 목록 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("내가 좋아요한 캠페인 목록 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .addProperty("content", new Schema<>()
                                .type("array")
                                .items(new Schema<>()
                                        .type("object")
                                        .addProperty("campaignId", new Schema<>().type("integer").example(123))
                                        .addProperty("title", new StringSchema().example("신상 음료 체험단 모집"))
                                        .addProperty("campaignType", new StringSchema().example("인스타그램"))
                                        .addProperty("likeCount", new Schema<>().type("integer").example(42))))
                        .addProperty("pageNumber", new Schema<>().type("integer").example(1))
                        .addProperty("totalElements", new Schema<>().type("integer").example(25)));
    }

    private Schema<?> createCampaignApplicantsSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 신청자 목록 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("캠페인 신청자 목록을 조회했어요"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .addProperty("campaign", new Schema<>()
                                .type("object")
                                .addProperty("id", new Schema<>().type("integer").example(42))
                                .addProperty("title", new StringSchema().example("신상 음료 체험단 모집"))
                                .addProperty("totalApplicants", new Schema<>().type("integer").example(15)))
                        .addProperty("applicants", new Schema<>()
                                .type("array")
                                .items(new Schema<>()
                                        .type("object")
                                        .addProperty("applicationId", new Schema<>().type("integer").example(101))
                                        .addProperty("applicationStatus", new StringSchema().example("pending"))
                                        .addProperty("user", new Schema<>()
                                                .type("object")
                                                .addProperty("id", new Schema<>().type("integer").example(5))
                                                .addProperty("nickname", new StringSchema().example("인플루언서닉네임")))))
                        .addProperty("pagination", new Schema<>()
                                .type("object")
                                .addProperty("pageNumber", new Schema<>().type("integer").example(1))
                                .addProperty("totalElements", new Schema<>().type("integer").example(15))));
    }

    private Schema<?> createCampaignApplicantResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 신청자 정보 응답")
                .addProperty("applicationId", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("신청 ID")
                        .example(101))
                .addProperty("user", new Schema<>()
                        .type("object")
                        .description("신청자 정보")
                        .addProperty("id", new Schema<>()
                                .type("integer")
                                .format("int64")
                                .description("사용자 ID")
                                .example(5))
                        .addProperty("nickname", new StringSchema()
                                .description("닉네임")
                                .example("인플루언서닉네임")
                                .nullable(false))
                        .addProperty("profileImage", new StringSchema()
                                .description("프로필 이미지 URL (선택사항)")
                                .example("https://example.com/profile.jpg")
                                .nullable(true))
                        .addProperty("phone", new StringSchema()
                                .description("전화번호 (선택사항)")
                                .example("010-1234-5678")
                                .nullable(true))
                        .addProperty("gender", new StringSchema()
                                .description("성별 (선택사항)")
                                .example("FEMALE")
                                ._enum(Arrays.asList("MALE", "FEMALE", "UNKNOWN"))
                                .nullable(true)))
                .addProperty("allSnsUrls", new Schema<>()
                        .type("array")
                        .description("신청자의 모든 SNS 플랫폼 정보 (빈 배열일 수 있음)")
                        .items(new Schema<>()
                                .type("object")
                                .addProperty("platformType", new StringSchema()
                                        .description("플랫폼 타입")
                                        .example("INSTAGRAM")
                                        .nullable(false))
                                .addProperty("snsUrl", new StringSchema()
                                        .description("계정 URL")
                                        .example("https://instagram.com/username")
                                        .nullable(false))))
                .addProperty("mission", new Schema<>()
                        .type("object")
                        .description("미션 정보 - 인플루언서의 미션 제출 상태와 URL")
                        .nullable(true)
                        .addProperty("missionId", new Schema<>()
                                .type("integer")
                                .format("int64")
                                .description("미션 ID - 미션 제출 고유 식별자")
                                .example(123)
                                .nullable(true))
                        .addProperty("missionStatus", new StringSchema()
                                .description("미션 상태")
                                .example("SUBMITTED")
                                ._enum(Arrays.asList("NOT_SUBMITTED", "SUBMITTED", "REVISION_REQUESTED", "COMPLETED"))
                                .nullable(false))
                        .addProperty("missionUrl", new StringSchema()
                                .description("미션 URL - 인플루언서가 제출한 SNS 포스트 URL")
                                .example("https://instagram.com/p/abc123")
                                .nullable(true)));
    }

    private Schema<?> createCampaignApplicantListResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 신청자 목록 응답")
                .addProperty("campaign", new Schema<>()
                        .type("object")
                        .description("캠페인 기본 정보")
                        .addProperty("id", new Schema<>()
                                .type("integer")
                                .format("int64")
                                .description("캠페인 ID")
                                .example(42))
                        .addProperty("title", new StringSchema()
                                .description("캠페인 제목")
                                .example("신상 음료 체험단 모집"))
                        .addProperty("totalApplicants", new Schema<>()
                                .type("integer")
                                .format("int64")
                                .description("전체 신청자 수")
                                .example(15)))
                .addProperty("applicants", new Schema<>()
                        .type("array")
                        .description("신청자 목록")
                        .items(new Schema<>().$ref("#/components/schemas/CampaignApplicantResponse")))
                .addProperty("pagination", new Schema<>()
                        .type("object")
                        .description("페이징 정보")
                        .addProperty("pageNumber", new Schema<>()
                                .type("integer")
                                .description("현재 페이지 번호 (1부터 시작)")
                                .example(1))
                        .addProperty("pageSize", new Schema<>()
                                .type("integer")
                                .description("페이지 크기")
                                .example(10))
                        .addProperty("totalPages", new Schema<>()
                                .type("integer")
                                .description("전체 페이지 수")
                                .example(2))
                        .addProperty("totalElements", new Schema<>()
                                .type("integer")
                                .format("int64")
                                .description("전체 항목 수")
                                .example(15))
                        .addProperty("first", new Schema<>()
                                .type("boolean")
                                .description("첫 번째 페이지 여부")
                                .example(true))
                        .addProperty("last", new Schema<>()
                                .type("boolean")
                                .description("마지막 페이지 여부")
                                .example(false)));
    }

    private Schema<?> createAllPlatformListSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("전체 SNS 플랫폼 목록 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("전체 SNS 플랫폼 목록 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .description("플랫폼 목록 데이터")
                        .addProperty("platforms", new Schema<>()
                                .type("array")
                                .description("전체 플랫폼 목록 (연동된 것 + 미연동 모두)")
                                .items(new Schema<>()
                                        .type("object")
                                        .description("플랫폼 정보")
                                        .addProperty("platformType", new StringSchema()
                                                .description("플랫폼 타입")
                                                .example("INSTAGRAM")
                                                ._enum(Arrays.asList("INSTAGRAM", "YOUTUBE", "BLOG", "FACEBOOK")))
                                        .addProperty("isConnected", new Schema<>()
                                                .type("boolean")
                                                .description("연동 여부")
                                                .example(true))
                                        .addProperty("id", new Schema<>()
                                                .type("integer")
                                                .format("int64")
                                                .description("플랫폼 ID (연동된 경우만, 미연동시 null)")
                                                .example(15)
                                                .nullable(true))
                                        .addProperty("accountUrl", new StringSchema()
                                                .description("계정 URL (연동된 경우만, 미연동시 null)")
                                                .example("https://instagram.com/myaccount")
                                                .nullable(true)))));
    }

    private Schema<?> createCampaignSelectionSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 신청자 선정 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("캠페인 신청자 선정이 완료되었습니다"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .addProperty("campaignId", new Schema<>().type("integer").example(42))
                        .addProperty("campaignTitle", new StringSchema().example("신상 음료 체험단 모집"))
                        .addProperty("selectedCount", new Schema<>().type("integer").example(5))
                        .addProperty("selectionProcessedAt", new StringSchema().example("2025-07-29T15:30:00Z")));
    }

    private Schema<?> createMyApplicationsSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("내 신청 목록 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true))
                .addProperty("message", new StringSchema()
                        .example("신청 목록을 조회했어요"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .type("object")
                        .addProperty("applications", new Schema<>()
                                .type("array")
                                .items(new Schema<>()
                                        .type("object")
                                        .addProperty("id", new Schema<>().type("integer").example(15))
                                        .addProperty("applicationStatus", new StringSchema().example("APPLIED"))
                                        .addProperty("campaign", new Schema<>()
                                                .type("object")
                                                .addProperty("id", new Schema<>().type("integer").example(42))
                                                .addProperty("title", new StringSchema().example("신상 음료 체험단 모집")))))
                        .addProperty("pagination", new Schema<>()
                                .type("object")
                                .addProperty("pageNumber", new Schema<>().type("integer").example(1))
                                .addProperty("totalElements", new Schema<>().type("integer").example(25))));
    }

    // ========== API 그룹화 ==========

    /**
     * 모든 API 표시 (기본 그룹)
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("전체 API (V1 + V2)")
                .pathsToMatch("/api/**")
                .build();
    }

    /**
     * V1 API 그룹 설정
     */
    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
                .group("v1")
                .displayName("V1 API (기존)")
                .pathsToMatch("/api/campaigns", "/api/campaigns/**", "/api/users/**", "/api/auth/**",
                        "/api/banner/**", "/api/autocomplete/**", "/api/applications/**",
                        "/api/my-campaigns/**", "/api/platforms/**", "/api/image/**")
                .pathsToExclude("/api/v2/**")
                .build();
    }

    /**
     * V2 API 그룹 설정
     */
    @Bean
    public GroupedOpenApi v2Api() {
        return GroupedOpenApi.builder()
                .group("v2")
                .displayName("V2 API (신규 - OAS 표준)")
                .pathsToMatch("/api/v2/**")
                .build();
    }

    // ========== 캠페인 진행 상태 API 스키마 생성 메서드 ==========

    private Schema<?> createCampaignProgressSuccessResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 진행 상태 조회 성공 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("응답 메시지")
                        .example("캠페인 진행 상태 조회 성공"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(200))
                .addProperty("data", new Schema<>()
                        .$ref("#/components/schemas/CampaignProgressResponse")
                        .description("캠페인 진행 상태 데이터"));
    }

    private Schema<?> createCampaignProgressResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 진행 상태 응답 데이터")
                .addProperty("campaignId", new Schema<>()
                        .type("integer")
                        .format("int64")
                        .description("캠페인 ID")
                        .example(123))
                .addProperty("campaignTitle", new StringSchema()
                        .description("캠페인 제목")
                        .example("이탈리안 레스토랑 신메뉴 체험단"))
                .addProperty("isAlwaysOpen", new Schema<>()
                        .type("boolean")
                        .description("상시 캠페인 여부")
                        .example(false))
                .addProperty("progress", new Schema<>()
                        .type("object")
                        .description("진행 상태 정보")
                        .addProperty("status", new StringSchema()
                                .description("현재 진행 상태")
                                .example("RECRUITING")
                                ._enum(Arrays.asList("RECRUITING", "RECRUITMENT_COMPLETED", 
                                        "SELECTION_COMPLETED", "MISSION_IN_PROGRESS", 
                                        "CONTENT_REVIEW_PENDING", "ALWAYS_OPEN")))
                        .addProperty("message", new StringSchema()
                                .description("진행 상태 메시지")
                                .example("지원자를 모집하고 있어요.")));
    }

    // ========== 캠페인 생성 에러 스키마 생성 메서드 ==========

    private Schema<?> createCampaignCategoryErrorResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 카테고리 오류 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(false)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("오류 메시지")
                        .example("유효하지 않은 카테고리 타입입니다."))
                .addProperty("errorCode", new StringSchema()
                        .description("오류 코드")
                        .example("INVALID_CATEGORY"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(400));
    }

    private Schema<?> createCampaignCreationPermissionErrorResponseSchema() {
        return new Schema<>()
                .type("object")
                .description("캠페인 생성 권한 오류 응답")
                .addProperty("success", new Schema<>()
                        .type("boolean")
                        .example(false)
                        .description("성공 여부"))
                .addProperty("message", new StringSchema()
                        .description("오류 메시지")
                        .example("CLIENT 권한이 필요합니다."))
                .addProperty("errorCode", new StringSchema()
                        .description("오류 코드")
                        .example("INSUFFICIENT_PERMISSION"))
                .addProperty("status", new Schema<>()
                        .type("integer")
                        .description("HTTP 상태 코드")
                        .example(403));
    }
}