package com.example.auth.config;

import com.example.auth.constant.Gender;
import com.example.auth.constant.PlatformType;
import com.example.auth.constant.UserRole;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ))
                .info(new Info()
                        .title("체험콕 서비스")
                        .version("v1.0.0")
                        .description("체험단 모집 및 관리 서비스 API"))
                .servers(List.of(
                        new io.swagger.v3.oas.models.servers.Server().url("https://chkok.kr").description("운영 서버"),
                        new io.swagger.v3.oas.models.servers.Server().url("http://localhost:8080").description("로컬 서버")
                ));
    }

    @Bean
    public OpenApiCustomizer enumCustomizer() {
        return openApi -> {
            // 모든 스키마를 가져와서 Enum 타입을 찾아 처리
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            if (schemas == null) {
                schemas = new TreeMap<>();
                openApi.getComponents().setSchemas(schemas);
            }

            // Gender enum 처리
            Schema<String> genderSchema = new Schema<>();
            genderSchema.setType("string");
            genderSchema.setEnum(Arrays.stream(Gender.values())
                    .map(Gender::getValue)
                    .collect(Collectors.toList()));
            genderSchema.setDescription("성별 값: MALE(남성), FEMALE(여성), UNKNOWN(알 수 없음)");
            schemas.put("Gender", genderSchema);

            // PlatformType enum 처리
            Schema<String> platformTypeSchema = new Schema<>();
            platformTypeSchema.setType("string");
            platformTypeSchema.setEnum(Arrays.stream(PlatformType.values())
                    .map(PlatformType::getValue)
                    .collect(Collectors.toList()));
            platformTypeSchema.setDescription("SNS 플랫폼 유형: BLOG(블로그), INSTAGRAM(인스타그램), YOUTUBE(유튜브)");
            schemas.put("PlatformType", platformTypeSchema);

            // UserRole enum 처리
            Schema<String> userRoleSchema = new Schema<>();
            userRoleSchema.setType("string");
            userRoleSchema.setEnum(Arrays.stream(UserRole.values())
                    .map(UserRole::getValue)
                    .collect(Collectors.toList()));
            userRoleSchema.setDescription("사용자 권한: USER(일반 사용자), CLIENT(클라이언트), ADMIN(관리자)");
            schemas.put("UserRole", userRoleSchema);
        };
    }
    
    @Bean
    public OpenApiCustomizer responseCustomizer() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            if (schemas == null) {
                schemas = new TreeMap<>();
                openApi.getComponents().setSchemas(schemas);
            }

            // BaseResponse.Success Schema
            Schema<Object> successResponseSchema = new Schema<Object>()
                    .type("object")
                    .addProperty("success", new Schema<Boolean>().type("boolean").example(true))
                    .addProperty("message", new Schema<String>().type("string").example("요청이 성공적으로 처리되었습니다."))
                    .addProperty("status", new Schema<Integer>().type("integer").example(200))
                    .addProperty("data", new Schema<Object>().type("object"));
            schemas.put("BaseResponseSuccess", successResponseSchema);

            // BaseResponse.Error Schema
            Schema<Object> errorResponseSchema = new Schema<Object>()
                    .type("object")
                    .addProperty("success", new Schema<Boolean>().type("boolean").example(false))
                    .addProperty("message", new Schema<String>().type("string").example("요청 처리 중 오류가 발생했습니다."))
                    .addProperty("status", new Schema<Integer>().type("integer").example(400))
                    .addProperty("errorCode", new Schema<String>().type("string").example("ERROR_CODE"));
            schemas.put("BaseResponseError", errorResponseSchema);
        };
    }
}
