package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.service.S3Service;
import com.example.auth.service.S3Service.PresignedUrlResponse;
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

    @Operation(summary = "이미지 업로드용 Presigned URL 생성", description = "S3에 이미지를 업로드하기 위한 presigned URL을 생성합니다.")
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
            log.info("Presigned URL 요청: userId={}, fileExtension={}", userId, request.getFileExtension());
            
            // S3 Presigned URL 생성 (최소 버전 - 확장자만 사용)
            PresignedUrlResponse presignedUrlResponse = s3Service.generatePresignedUrl(
                request.getFileExtension()
            );
            
            log.info("Presigned URL 생성 완료: userId={}, objectKey={}", userId, presignedUrlResponse.objectKey());
            
            // 응답 데이터 구성 (objectKey 제외)
            Map<String, Object> responseData = Map.of(
                    "presignedUrl", presignedUrlResponse.presignedUrl(),
                    "objectUrl", presignedUrlResponse.objectUrl()
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

    /**
     * Presigned URL 요청을 위한 DTO
     */
    @Data
    public static class PresignedUrlRequest {
        @NotBlank(message = "파일 확장자는 필수입니다")
        @Pattern(regexp = "^(jpg|jpeg|png|gif|bmp|webp|svg)$", message = "지원하지 않는 파일 형식입니다")
        private String fileExtension;
    }
}
