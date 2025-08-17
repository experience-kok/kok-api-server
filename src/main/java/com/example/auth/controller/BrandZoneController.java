package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.brandzone.BrandCampaignResponse;
import com.example.auth.dto.brandzone.BrandInfoResponse;
import com.example.auth.dto.brandzone.BrandListResponse;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.service.BrandZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 브랜드존 컨트롤러
 * 브랜드별 캠페인 목록 및 브랜드 정보를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@Tag(name = "브랜드존 API", description = "브랜드별 캠페인 목록 및 브랜드 정보 API")
public class BrandZoneController {

    private final BrandZoneService brandZoneService;

    @Operation(
        summary = "모든 브랜드 목록 조회",
        description = "등록된 모든 브랜드(회사) 목록을 조회합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 모든 브랜드 목록 페이징 조회\n" +
                      "- 브랜드명으로 필터링 가능 (부분 일치)\n" +
                      "- 브랜드별 캠페인 통계 포함\n\n" +
                      "### 필터링\n" +
                      "- **brandName**: 브랜드명으로 필터링 (부분 일치, 대소문자 무시)\n" +
                      "- 필터링 없이 요청하면 전체 목록 반환\n\n" +
                      "### 페이징 특징\n" +
                      "- 기본 페이지 크기: 12개\n" +
                      "- 최신 등록순으로 정렬\n\n" +
                      "### 인증\n" +
                      "- **인증 불필요**: 누구나 접근 가능한 공개 API",
        security = {}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "브랜드 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/BrandListSuccessResponse"),
                examples = {
                    @ExampleObject(
                        name = "전체 브랜드 목록",
                        summary = "전체 브랜드 목록 조회",
                        value = """
                            {
                              "success": true,
                              "message": "브랜드 목록 조회 성공",
                              "status": 200,
                              "data": {
                                "content": [
                                  {
                                    "brandId": 1,
                                    "brandName": "ABC 코스메틱",
                                    "totalCampaigns": 15,
                                    "activeCampaigns": 3
                                  },
                                  {
                                    "brandId": 2,
                                    "brandName": "맛있는 카페",
                                    "totalCampaigns": 8,
                                    "activeCampaigns": 2
                                  }
                                ],
                                "pageNumber": 1,
                                "pageSize": 12,
                                "totalPages": 5,
                                "totalElements": 58,
                                "first": true,
                                "last": false
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "브랜드명 필터링",
                        summary = "브랜드명으로 필터링한 결과",
                        value = """
                            {
                              "success": true,
                              "message": "브랜드 목록 조회 성공",
                              "status": 200,
                              "data": {
                                "content": [
                                  {
                                    "brandId": 1,
                                    "brandName": "ABC 코스메틱",
                                    "totalCampaigns": 15,
                                    "activeCampaigns": 3
                                  }
                                ],
                                "pageNumber": 1,
                                "pageSize": 12,
                                "totalPages": 1,
                                "totalElements": 1,
                                "first": true,
                                "last": true
                              }
                            }
                            """
                    )
                }
            )
        )
    })
    @GetMapping
    public ResponseEntity<?> getAllBrands(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1-50)", example = "12")
            @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "브랜드명 필터링 (부분 일치, 선택사항)", example = "ABC")
            @RequestParam(required = false) String brandName
    ) {
        try {
            log.info("브랜드 목록 조회 요청: page={}, size={}, brandName={}", page, size, brandName);

            // 페이지 번호 검증 (1부터 시작하므로 0으로 변환)
            if (page < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 번호는 1 이상이어야 합니다.", "INVALID_PAGE", HttpStatus.BAD_REQUEST.value()));
            }

            // 페이지 크기 검증
            if (size < 1 || size > 50) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 크기는 1-50 사이여야 합니다.", "INVALID_PAGE_SIZE", HttpStatus.BAD_REQUEST.value()));
            }

            PageResponse<BrandListResponse> pageResponse = brandZoneService.getAllBrands(page - 1, size, brandName);

            return ResponseEntity.ok(BaseResponse.success(pageResponse, "브랜드 목록 조회 성공"));

        } catch (Exception e) {
            log.error("브랜드 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("브랜드 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "특정 브랜드 정보 조회",
        description = "특정 브랜드의 상세 정보를 조회합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 브랜드 기본 정보 (이름, 담당자, 연락처 등)\n" +
                      "- 브랜드 캠페인 통계 (총 캠페인 수, 활성 캠페인 수)\n" +
                      "- 브랜드 등록일 정보\n\n" +
                      "### 포함 정보\n" +
                      "- **totalCampaigns**: 브랜드가 진행한 총 캠페인 수\n" +
                      "- **activeCampaigns**: 현재 모집 중인 캠페인 수\n\n" +
                      "### 인증\n" +
                      "- **인증 불필요**: 누구나 접근 가능한 공개 API",
        security = {}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "브랜드 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/BrandInfoSuccessResponse"),
                examples = @ExampleObject(
                    name = "브랜드 정보",
                    summary = "브랜드 정보 조회 성공",
                    value = """
                        {
                          "success": true,
                          "message": "브랜드 정보 조회 성공",
                          "status": 200,
                          "data": {
                            "brandId": 1,
                            "brandName": "ABC 코스메틱",
                            "contactPerson": "김담당",
                            "phoneNumber": "02-1234-5678",
                            "totalCampaigns": 15,
                            "activeCampaigns": 3
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "브랜드를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
            )
        )
    })
    @GetMapping("/{brandId}")
    public ResponseEntity<?> getBrandInfo(
            @Parameter(description = "브랜드 ID", required = true, example = "1")
            @PathVariable Long brandId
    ) {
        try {
            log.info("브랜드 정보 조회 요청: brandId={}", brandId);

            BrandInfoResponse response = brandZoneService.getBrandInfo(brandId);

            return ResponseEntity.ok(BaseResponse.success(response, "브랜드 정보 조회 성공"));

        } catch (ResourceNotFoundException e) {
            log.warn("브랜드 정보 조회 실패 (리소스 없음): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("브랜드 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("브랜드 정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
