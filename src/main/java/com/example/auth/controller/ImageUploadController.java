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

    @Operation(summary = "이미지 업로드용 Presigned URL 생성", 
               description = "S3에 원본 크기 이미지를 업로드하기 위한 presigned URL을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/presigned-url")
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

            // S3 Presigned URL 생성
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

    @Operation(summary = "특정 크기의 이미지 업로드용 Presigned URL 생성", 
               description = "S3에 특정 크기(480px 또는 720px)의 이미지를 업로드하기 위한 presigned URL을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/presigned-url/{size}")
    public ResponseEntity<?> generateSizedPresignedUrl(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "이미지 크기 (480 또는 720)", required = true)
            @PathVariable Integer size,
            @Parameter(description = "파일 정보", required = true)
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        try {
            // 토큰에서 사용자 ID 추출 (인증)
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            // 지원하는 크기 검증
            if (size != 480 && size != 720) {
                return ResponseEntity.badRequest()
                        .body(BaseResponse.fail("지원하지 않는 이미지 크기입니다. 480 또는 720만 지원합니다.", 
                                "INVALID_SIZE", 400));
            }
            
            log.info("이미지 Presigned URL 요청: userId={}, size={}, fileExtension={}", 
                    userId, size, request.getFileExtension());

            // 크기에 따른 Presigned URL 생성
            String presignedUrl;
            
            if (size == 480) {
                // 프로필 이미지 (480x480)
                presignedUrl = s3Service.generateProfileImagePresignedUrl(request.getFileExtension());
                log.info("프로필 이미지(480x480) Presigned URL 생성 완료: userId={}", userId);
            } else {
                // 커버 이미지 (720px 너비)
                presignedUrl = s3Service.generateCoverImagePresignedUrl(request.getFileExtension());
                log.info("커버 이미지(720px 너비) Presigned URL 생성 완료: userId={}", userId);
            }

            // 응답 데이터 구성 - presignedUrl만 포함
            Map<String, Object> responseData = Map.of(
                    "presignedUrl", presignedUrl
            );

            return ResponseEntity.ok(
                    BaseResponse.success(
                            responseData,
                            size + "px 이미지 업로드용 URL이 성공적으로 생성되었습니다."
                    )
            );
        } catch (Exception e) {
            log.error("Presigned URL 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(BaseResponse.fail("Presigned URL 생성에 실패했습니다.", "URL_GENERATION_FAILED", 400));
        }
    }

    /**
     * 기본 Presigned URL 요청을 위한 DTO
     */
    @Data
    public static class PresignedUrlRequest {
        @NotBlank(message = "파일 확장자는 필수입니다")
        @Pattern(regexp = "^(jpg|jpeg|png)$", message = "지원하지 않는 파일 형식입니다")
        private String fileExtension;
    }
}