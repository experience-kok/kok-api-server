package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.service.S3Service;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "이미지 업로드 API", description = "이미지 업로드 관련 API")
public class ImageUploadController {

    private final S3Service s3Service;
    private final TokenUtils tokenUtils;

    @Operation(summary = "일반 이미지 업로드용 Presigned URL 생성", 
               description = "S3에 원본 크기 이미지를 업로드하기 위한 presigned URL을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/PresignedUrlSuccessResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Presigned URL 생성 성공",
                                summary = "일반 이미지 업로드용 URL 생성 성공",
                                value = "{\n" +
                                       "  \"success\": true,\n" +
                                       "  \"message\": \"이미지 업로드용 URL이 성공적으로 생성되었습니다.\",\n" +
                                       "  \"status\": 200,\n" +
                                       "  \"data\": {\n" +
                                       "    \"presignedUrl\": \"https://ckokservice.s3.ap-northeast-2.amazonaws.com/common/1720612800000-abc123def456.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...\"\n" +
                                       "  }\n" +
                                       "}"
                            ))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = {
                                @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "지원하지 않는 파일 형식",
                                    summary = "허용되지 않은 파일 확장자인 경우",
                                    value = "{\n" +
                                           "  \"success\": false,\n" +
                                           "  \"message\": \"지원하지 않는 파일 형식입니다. jpg, jpeg, png만 허용됩니다.\",\n" +
                                           "  \"errorCode\": \"INVALID_FILE_TYPE\",\n" +
                                           "  \"status\": 400\n" +
                                           "}"
                                ),
                                @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "필수 필드 누락",
                                    summary = "fileExtension이 누락된 경우",
                                    value = "{\n" +
                                           "  \"success\": false,\n" +
                                           "  \"message\": \"파일 확장자는 필수입니다.\",\n" +
                                           "  \"errorCode\": \"VALIDATION_ERROR\",\n" +
                                           "  \"status\": 400\n" +
                                           "}"
                                )
                            })),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "토큰 인증 실패",
                                value = "{\n" +
                                       "  \"success\": false,\n" +
                                       "  \"message\": \"유효하지 않은 토큰입니다.\",\n" +
                                       "  \"errorCode\": \"TOKEN_INVALID\",\n" +
                                       "  \"status\": 401\n" +
                                       "}"
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "URL 생성 실패",
                                summary = "S3 서비스 오류로 URL 생성에 실패한 경우",
                                value = "{\n" +
                                       "  \"success\": false,\n" +
                                       "  \"message\": \"Presigned URL 생성에 실패했습니다.\",\n" +
                                       "  \"errorCode\": \"URL_GENERATION_FAILED\",\n" +
                                       "  \"status\": 500\n" +
                                       "}"
                            )))
    })
    @PostMapping("/common/presigned-url")
    public ResponseEntity<?> generatePresignedUrl(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "파일 정보", required = true)
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        try {
            // 토큰에서 사용자 ID 추출 (인증)
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("원본 이미지 Presigned URL 요청: userId={}, fileExtension={}", userId, request.getFileExtension());

            // S3 Presigned URL 생성 (일반 폴더)
            String presignedUrl = s3Service.generatePresignedUrl(request.getFileExtension());

            log.info("원본 이미지 Presigned URL 생성 완료: userId={}", userId);

            // 응답 데이터 구성 - presignedUrl만 포함
            Map<String, Object> responseData = Map.of(
                    "presignedUrl", presignedUrl
            );

            return ResponseEntity.ok(
                    BaseResponse.success(
                            responseData,
                            "이미지 업로드용 URL이 성공적으로 생성되었습니다."
                    )
            );
        } catch (Exception e) {
            log.error("Presigned URL 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(BaseResponse.fail("Presigned URL 생성에 실패했습니다.", "URL_GENERATION_FAILED", 400));
        }
    }

    @Operation(summary = "프로필 이미지 업로드용 Presigned URL 생성", 
               description = "프로필 이미지(100x100 자동 리사이징)를 업로드하기 위한 presigned URL을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/PresignedUrlSuccessResponse"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/profile/presigned-url")
    public ResponseEntity<?> generateProfileImagePresignedUrl(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "파일 정보", required = true)
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        try {
            // 토큰에서 사용자 ID 추출 (인증)
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("프로필 이미지 Presigned URL 요청: userId={}, fileExtension={}", userId, request.getFileExtension());

            // 직접 경로를 구성하여 프로필 이미지 전용 폴더에 업로드하도록 URL 생성
            String uuid = java.util.UUID.randomUUID().toString();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = String.format("%s-%s.%s", timestamp, uuid, request.getFileExtension());
            String objectKey = String.format("profile-images/%s", filename);
            
            // 프로필 이미지용 Presigned URL 생성
            String presignedUrl = s3Service.generatePresignedUrlForKey(objectKey);

            log.info("프로필 이미지 Presigned URL 생성 완료: userId={}, key={}", userId, objectKey);

            // 응답 데이터 구성
            Map<String, Object> responseData = Map.of(
                    "presignedUrl", presignedUrl
            );

            return ResponseEntity.ok(
                    BaseResponse.success(
                            responseData,
                            "프로필 이미지 업로드용 URL이 성공적으로 생성되었습니다. 이미지는 100x100 픽셀로 자동 리사이징됩니다."
                    )
            );
        } catch (Exception e) {
            log.error("프로필 이미지 Presigned URL 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("Presigned URL 생성에 실패했습니다.", "URL_GENERATION_FAILED", 500));
        }
    }

    @Operation(summary = "캠페인 이미지 업로드용 Presigned URL 생성", 
               description = "캠페인 이미지(720x720 자동 리사이징)를 업로드하기 위한 presigned URL을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/PresignedUrlSuccessResponse"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping("/campaign/presigned-url")
    public ResponseEntity<?> generateCampaignImagePresignedUrl(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "파일 정보", required = true)
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        try {
            // 토큰에서 사용자 ID 추출 (인증)
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("캠페인 이미지 Presigned URL 요청: userId={}, fileExtension={}", userId, request.getFileExtension());

            // 직접 경로를 구성하여 캠페인 이미지 전용 폴더에 업로드하도록 URL 생성
            String uuid = java.util.UUID.randomUUID().toString();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = String.format("%s-%s.%s", timestamp, uuid, request.getFileExtension());
            String objectKey = String.format("campaign-images/%s", filename);
            
            // 캠페인 이미지용 Presigned URL 생성
            String presignedUrl = s3Service.generatePresignedUrlForKey(objectKey);

            log.info("캠페인 이미지 Presigned URL 생성 완료: userId={}, key={}", userId, objectKey);

            // 응답 데이터 구성
            Map<String, Object> responseData = Map.of(
                    "presignedUrl", presignedUrl
            );

            return ResponseEntity.ok(
                    BaseResponse.success(
                            responseData,
                            "캠페인 이미지 업로드용 URL이 성공적으로 생성되었습니다. 이미지는 720x720 픽셀로 자동 리사이징됩니다."
                    )
            );
        } catch (Exception e) {
            log.error("캠페인 이미지 Presigned URL 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("Presigned URL 생성에 실패했습니다.", "URL_GENERATION_FAILED", 500));
        }
    }

    /**
     * 기본 Presigned URL 요청을 위한 DTO
     */
    @Data
    @Schema(description = "Presigned URL 생성 요청")
    public static class PresignedUrlRequest {
        @Schema(description = "파일 확장자", example = "jpg", allowableValues = {"jpg", "jpeg", "png"}, required = true)
        @NotBlank(message = "파일 확장자는 필수입니다")
        @Pattern(regexp = "^(jpg|jpeg|png)$", message = "지원하지 않는 파일 형식입니다")
        private String fileExtension;
    }
}