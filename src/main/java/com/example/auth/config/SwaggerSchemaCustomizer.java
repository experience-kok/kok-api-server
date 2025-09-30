package com.example.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

/**
 * Swagger 스키마 커스터마이저
 * 동적으로 스키마에 예시를 추가합니다
 *
 */
@Component
@SuppressWarnings({"unchecked", "deprecation"})
public class SwaggerSchemaCustomizer implements OpenApiCustomizer {

    @Override
    public void customise(OpenAPI openAPI) {
        // Success와 Error 스키마가 존재하지 않으므로 제거됨
        // 대신 ApiSuccessResponse와 ApiErrorResponse를 사용
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {

            // ApiSuccessResponse 스키마에 예시 추가
            Schema<?> apiSuccessSchema = openAPI.getComponents().getSchemas().get("ApiSuccessResponse");
            if (apiSuccessSchema != null) {
                apiSuccessSchema.example("""
                    {
                      "success": true,
                      "message": "요청이 성공적으로 처리되었습니다.",
                      "status": 200,
                      "data": {
                        "id": 123,
                        "title": "Instagram Cafe Experience Campaign",
                        "campaignType": "INSTAGRAM",
                        "maxApplicants": 10,
                        "currentApplicants": 3,
                        "createdAt": "2025-07-10T15:30:00Z"
                      }
                    }
                    """);
            }

            // ApiErrorResponse 스키마에 예시 추가
            Schema<?> apiErrorSchema = openAPI.getComponents().getSchemas().get("ApiErrorResponse");
            if (apiErrorSchema != null) {
                apiErrorSchema.example("""
                    {
                      "success": false,
                      "message": "제목은 필수 입력값입니다.",
                      "errorCode": "VALIDATION_ERROR",
                      "status": 400
                    }
                    """);
            }

            // Gender와 UserRole 스키마는 더 이상 별도 파일로 생성하지 않음
            // 대신 TypeScript enum으로만 처리됨

            // ErrorCode 스키마에 예시 추가
            Schema<?> errorCodeSchema = openAPI.getComponents().getSchemas().get("ErrorCode");
            if (errorCodeSchema != null) {
                errorCodeSchema.example("VALIDATION_ERROR");
            }

            // V2 CampaignType enum 정리
            Schema<?> campaignTypeV2Schema = openAPI.getComponents().getSchemas().get("CampaignTypeV2");
            if (campaignTypeV2Schema != null) {
                List<String> campaignTypes = Arrays.asList("INSTAGRAM", "BLOG", "YOUTUBE");
                // SuppressWarnings로 unchecked 경고 억제됨
                ((Schema) campaignTypeV2Schema).setEnum(campaignTypes);
                campaignTypeV2Schema.example("INSTAGRAM");
            }

            // V2 CategoryName enum 정리
            Schema<?> categoryNameV2Schema = openAPI.getComponents().getSchemas().get("CategoryNameV2");
            if (categoryNameV2Schema != null) {
                List<String> categoryNames = Arrays.asList(
                    "RESTAURANT", "CAFE", "BEAUTY", "ACCOMMODATION",
                    "FOOD", "COSMETIC", "LIFESTYLE", "FASHION", "ACCESSORIES"
                );
                // SuppressWarnings로 unchecked 경고 억제됨
                ((Schema) categoryNameV2Schema).setEnum(categoryNames);
                categoryNameV2Schema.example("CAFE");
            }

            // UserInfo 스키마에 누락된 필드들 추가
            Schema<?> userInfoSchema = openAPI.getComponents().getSchemas().get("UserInfo");
            if (userInfoSchema != null && userInfoSchema.getProperties() != null) {
                // phone 필드 추가
                if (!userInfoSchema.getProperties().containsKey("phone")) {
                    Schema<String> phoneSchema = new Schema<String>()
                            .type("string")
                            .description("전화번호 - 캠페인 선정 시 연락용으로 사용되는 연락처 (선택사항)")
                            .example("010-1234-5678");
                    userInfoSchema.getProperties().put("phone", phoneSchema);
                }
                
                // gender 필드 추가 (인라인 enum으로 변경)
                if (!userInfoSchema.getProperties().containsKey("gender")) {
                    Schema<String> genderInlineSchema = new Schema<String>()
                            .type("string")
                            .description("성별 - 통계 분석 및 맞춤형 캠페인 추천용 (선택사항)")
                            .example("MALE")
                            ._enum(Arrays.asList("MALE", "FEMALE", "UNKNOWN"));
                    userInfoSchema.getProperties().put("gender", genderInlineSchema);
                }
                
                // age 필드 추가
                if (!userInfoSchema.getProperties().containsKey("age")) {
                    Schema<Integer> ageSchema = new Schema<Integer>()
                            .type("integer")
                            .description("나이 - 연령대별 캠페인 타겟팅 및 통계 분석용 (만 나이 기준, 선택사항)")
                            .example(30);
                    userInfoSchema.getProperties().put("age", ageSchema);
                }
                
                // 기존 필드들의 description 개선
                if (userInfoSchema.getProperties().containsKey("id")) {
                    userInfoSchema.getProperties().get("id")
                            .description("사용자 고유 식별자 - 시스템에서 사용자를 구분하는 유일한 ID");
                }
                
                if (userInfoSchema.getProperties().containsKey("email")) {
                    userInfoSchema.getProperties().get("email")
                            .description("이메일 주소 - 로그인 및 알림 발송에 사용되는 사용자의 이메일");
                }
                
                if (userInfoSchema.getProperties().containsKey("nickname")) {
                    userInfoSchema.getProperties().get("nickname")
                            .description("닉네임 - 플랫폼 내에서 표시되는 사용자의 별명 (2~8자)");
                }
                
                if (userInfoSchema.getProperties().containsKey("profileImg")) {
                    userInfoSchema.getProperties().get("profileImg")
                            .description("프로필 이미지 URL - 사용자의 프로필 사진 (CloudFront CDN을 통해 제공)");
                }
                
                if (userInfoSchema.getProperties().containsKey("role")) {
                    userInfoSchema.getProperties().get("role")
                            .description("사용자 권한 - 플랫폼 내 역할과 접근 권한을 나타냄");
                }
                
                // 전체 스키마 description 개선
                userInfoSchema.description("사용자 정보 - 로그인 및 프로필 조회 시 반환되는 사용자 데이터");
                
                // 예시 추가
                userInfoSchema.example("""
                    {
                      "id": 123,
                      "email": "user@example.com",
                      "nickname": "홍길동",
                      "profileImage": "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg",
                      "phone": "010-1234-5678",
                      "gender": "MALE",
                      "age": 30,
                      "role": "USER"
                    }
                    """);
            }
        }
    }
}
