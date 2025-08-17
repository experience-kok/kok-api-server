package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.campaign.*;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.service.CampaignViewService;
import com.example.auth.service.SearchAnalyticsService;
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

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "캠페인 조회 API", description = "캠페인 조회 관련 API")
public class CampaignViewController {

    private final CampaignViewService viewService;
    private final SearchAnalyticsService searchAnalyticsService;

    /**
     * 정렬 파라미터 변환
     */
    private String convertSortParameter(String sort) {
        return switch (sort) {
            case "popular" -> "currentApplicants";
            case "deadline" -> "recruitmentEndDate";
            case "latest" -> "createdAt";
            default -> "createdAt";
        };
    }

    // ===== 인기순/마감순 특화 API =====

    @Operation(
            summary = "인기 캠페인 목록 조회",
            description = "신청 인원이 많은 순으로 캠페인을 조회합니다. **상시 캠페인도 포함**됩니다."
                    + "\n\n### 필터링 옵션:"
                    + "\n- **카테고리 타입**: categoryType=방문 또는 categoryType=배송"
                    + "\n- **카테고리명**: categoryName=맛집, 카페, 뷰티, 숙박, 식품, 화장품 등"
                    + "\n- **캠페인 타입**: campaignType=인스타그램, 블로그, 유튜브 등"
                    + "\n\n### 상시 캠페인:"
                    + "\n- **상시 캠페인**: 모집 마감일이 없는 언제든 신청 가능한 캠페인"
                    + "\n- **일반 캠페인**: 모집 마감일이 설정된 기간 한정 캠페인"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignV1ListSuccessResponse"),
                            examples = @ExampleObject(
                                name = "인기 캠페인 목록 조회 성공",
                                summary = "인기순으로 정렬된 캠페인 목록",
                                value = """
                                {
                                  "success": true,
                                  "message": "배송 캠페인 목록 조회 성공",
                                  "status": 200,
                                  "data": {
                                    "campaigns": [
                                      {
                                        "id": 28,
                                        "isAlwaysOpen": true,
                                        "campaignType": "인스타그램",
                                        "title": "상시 모집 뷰티 제품 체험단",
                                        "productShortInfo": "립스틱 + 파운데이션 세트",
                                        "currentApplicants": 25,
                                        "maxApplicants": 50,
                                        "recruitmentEndDate": null,
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/beauty.jpg",
                                        "category": {
                                          "type": "배송",
                                          "name": "화장품"
                                        }
                                      },
                                      {
                                        "id": 27,
                                        "isAlwaysOpen": false,
                                        "campaignType": "블로그",
                                        "title": "프리미엄 스킨케어 세트 체험단",
                                        "productShortInfo": "토너 + 에센스 + 크림 세트",
                                        "currentApplicants": 15,
                                        "maxApplicants": 20,
                                        "recruitmentEndDate": "2027-12-20",
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/skincare.jpg",
                                        "category": {
                                          "type": "배송",
                                          "name": "화장품"
                                        }
                                      }
                                    ],
                                    "pagination": {
                                      "pageNumber": 1,
                                      "pageSize": 10,
                                      "totalPages": 1,
                                      "totalElements": 2,
                                      "first": true,
                                      "last": true
                                    }
                                  }
                                }
                                """
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularCampaigns(
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,

            @Parameter(description = "요청할 캠페인 갯수")
            @RequestParam(required = false, defaultValue = "10") int size,

            @Parameter(description = "카테고리 타입 (방문, 배송)")
            @RequestParam(required = false) String categoryType,

            @Parameter(description = "카테고리명 (맛집, 카페, 뷰티, 숙박, 식품, 화장품 등)")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "캠페인 타입 (인스타그램, 블로그, 유튜브 등)")
            @RequestParam(required = false) String campaignType,

            @Parameter(description = "페이징 정보 포함 여부")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            log.info("인기 캠페인 목록 조회 요청 - page: {}, size: {}, categoryType: {}, categoryName: {}, campaignType: {}, includePaging: {}",
                    page, size, categoryType, categoryName, campaignType, includePaging);

            var pageResponse = viewService.getCampaignListWithFilters(Math.max(0, page - 1), size, "currentApplicants", true, categoryType, categoryName, campaignType);
            List<CampaignListSimpleResponse> campaigns = pageResponse.getContent();

            if (includePaging) {
                CampaignListResponseWrapper responseWrapper = new CampaignListResponseWrapper();
                responseWrapper.setCampaigns(campaigns);

                CampaignListResponseWrapper.PaginationInfo paginationInfo =
                        CampaignListResponseWrapper.PaginationInfo.builder()
                                .pageNumber(pageResponse.getPageNumber())
                                .pageSize(pageResponse.getPageSize())
                                .totalPages(pageResponse.getTotalPages())
                                .totalElements(pageResponse.getTotalElements())
                                .first(pageResponse.isFirst())
                                .last(pageResponse.isLast())
                                .build();

                responseWrapper.setPagination(paginationInfo);

                return ResponseEntity.ok(BaseResponse.success(responseWrapper, "인기 캠페인 목록 조회 성공"));
            } else {
                Map<String, Object> responseData = Map.of("campaigns", campaigns);
                return ResponseEntity.ok(BaseResponse.success(responseData, "인기 캠페인 목록 조회 성공"));
            }
        } catch (Exception e) {
            log.error("인기 캠페인 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("인기 캠페인 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "마감 임박 캠페인 목록 조회",
            description = "신청 마감일이 가까운 순으로 캠페인을 조회합니다. **상시 캠페인은 제외**됩니다."
                    + "\n\n**접근 제한**: 승인된 활성 캠페인만 조회되며, 거절된 캠페인은 목록에서 제외됩니다."
                    + "\n\n### 필터링 옵션:"
                    + "\n- **카테고리 타입**: categoryType=방문 또는 categoryType=배송"
                    + "\n- **카테고리명**: categoryName=맛집, 카페, 뷰티, 숙박, 식품, 화장품 등"
                    + "\n- **캠페인 타입**: campaignType=인스타그램, 블로그, 유튜브 등"
                    + "\n\n### 주의사항:"
                    + "\n- **상시 캠페인**: 마감일이 없으므로 이 API에서는 조회되지 않습니다"
                    + "\n- **일반 캠페인**: 마감일이 설정된 캠페인만 마감 가까운 순으로 정렬됩니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignV1ListSuccessResponse"),
                            examples = @ExampleObject(
                                name = "마감 캠페인 목록 조회 성공",
                                summary = "마감순으로 정렬된 캠페인 목록",
                                value = """
                                {
                                  "success": true,
                                  "message": "마감 캠페인 목록 조회 성공",
                                  "status": 200,
                                  "data": {
                                    "campaigns": [
                                      {
                                        "id": 25,
                                        "isAlwaysOpen": false,
                                        "campaignType": "인스타그램",
                                        "title": "이탈리안 레스토랑 신메뉴 체험단",
                                        "productShortInfo": "파스타 2인분 + 와인 1병 무료 제공",
                                        "currentApplicants": 8,
                                        "maxApplicants": 15,
                                        "recruitmentEndDate": "2027-12-12",
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/%ED%8C%8C%EC%8A%A4%ED%83%80.jpg",
                                        "category": {
                                          "type": "방문",
                                          "name": "맛집"
                                        }
                                      }
                                    ],
                                    "pagination": {
                                      "pageNumber": 1,
                                      "pageSize": 10,
                                      "totalPages": 1,
                                      "totalElements": 1,
                                      "first": true,
                                      "last": true
                                    }
                                  }
                                }
                                """
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/deadline-soon")
    public ResponseEntity<?> getDeadlineSoonCampaigns(
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,

            @Parameter(description = "요청할 캠페인 갯수")
            @RequestParam(required = false, defaultValue = "10") int size,

            @Parameter(description = "카테고리 타입 (방문, 배송)")
            @RequestParam(required = false) String categoryType,

            @Parameter(description = "카테고리명 (맛집, 카페, 뷰티, 숙박, 식품, 화장품 등)")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "캠페인 타입 (인스타그램, 블로그, 유튜브 등)")
            @RequestParam(required = false) String campaignType,

            @Parameter(description = "페이징 정보 포함 여부")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            log.info("마감 임박 캠페인 목록 조회 요청 - page: {}, size: {}, categoryType: {}, categoryName: {}, campaignType: {}, includePaging: {}",
                    page, size, categoryType, categoryName, campaignType, includePaging);

            var pageResponse = viewService.getCampaignListByDeadlineSoonWithFilters(Math.max(0, page - 1), size, categoryType, categoryName, campaignType);
            List<CampaignListSimpleResponse> campaigns = pageResponse.getContent();

            if (includePaging) {
                CampaignListResponseWrapper responseWrapper = new CampaignListResponseWrapper();
                responseWrapper.setCampaigns(campaigns);

                CampaignListResponseWrapper.PaginationInfo paginationInfo =
                        CampaignListResponseWrapper.PaginationInfo.builder()
                                .pageNumber(pageResponse.getPageNumber())
                                .pageSize(pageResponse.getPageSize())
                                .totalPages(pageResponse.getTotalPages())
                                .totalElements(pageResponse.getTotalElements())
                                .first(pageResponse.isFirst())
                                .last(pageResponse.isLast())
                                .build();

                responseWrapper.setPagination(paginationInfo);

                return ResponseEntity.ok(BaseResponse.success(responseWrapper, "마감 임박 캠페인 목록 조회 성공"));
            } else {
                Map<String, Object> responseData = Map.of("campaigns", campaigns);
                return ResponseEntity.ok(BaseResponse.success(responseData, "마감 임박 캠페인 목록 조회 성공"));
            }
        } catch (Exception e) {
            log.error("마감 임박 캠페인 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("마감 임박 캠페인 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "최신 캠페인 목록 조회",
            description = "최신 등록순으로 캠페인을 조회합니다. **상시 캠페인도 포함**됩니다."
                    + "\n\n### 필터링 옵션:"
                    + "\n- **카테고리 타입**: categoryType=방문 또는 categoryType=배송"
                    + "\n- **카테고리명**: categoryName=맛집, 카페, 뷰티, 숙박, 식품, 화장품 등"
                    + "\n- **캠페인 타입**: campaignType=인스타그램, 블로그, 유튜브 등"
                    + "\n\n### 상시 캠페인:"
                    + "\n- **상시 캠페인**: 모집 마감일이 없는 언제든 신청 가능한 캠페인"
                    + "\n- **일반 캠페인**: 모집 마감일이 설정된 기간 한정 캠페인"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignV1ListSuccessResponse"),
                            examples = @ExampleObject(
                                name = "최신 캠페인 목록 조회 성공",
                                summary = "최신순으로 정렬된 캠페인 목록",
                                value = """
                                {
                                  "success": true,
                                  "message": "최신 캠페인 목록 조회 성공",
                                  "status": 200,
                                  "data": {
                                    "campaigns": [
                                      {
                                        "id": 26,
                                        "isAlwaysOpen": true,
                                        "campaignType": "블로그",
                                        "title": "상시 모집 카페 체험단",
                                        "productShortInfo": "음료 1잔 + 디저트 무료 제공",
                                        "currentApplicants": 12,
                                        "maxApplicants": 30,
                                        "recruitmentEndDate": null,
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/cafe.jpg",
                                        "category": {
                                          "type": "방문",
                                          "name": "카페"
                                        }
                                      },
                                      {
                                        "id": 25,
                                        "isAlwaysOpen": false,
                                        "campaignType": "인스타그램",
                                        "title": "이탈리안 레스토랑 신메뉴 체험단",
                                        "productShortInfo": "파스타 2인분 + 와인 1병 무료 제공",
                                        "currentApplicants": 8,
                                        "maxApplicants": 15,
                                        "recruitmentEndDate": "2027-12-12",
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/%ED%8C%8C%EC%8A%A4%ED%83%80.jpg",
                                        "category": {
                                          "type": "방문",
                                          "name": "맛집"
                                        }
                                      }
                                    ],
                                    "pagination": {
                                      "pageNumber": 1,
                                      "pageSize": 10,
                                      "totalPages": 1,
                                      "totalElements": 2,
                                      "first": true,
                                      "last": true
                                    }
                                  }
                                }
                                """
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestCampaigns(
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,

            @Parameter(description = "요청할 캠페인 갯수")
            @RequestParam(required = false, defaultValue = "10") int size,

            @Parameter(description = "카테고리 타입 (방문, 배송)")
            @RequestParam(required = false) String categoryType,

            @Parameter(description = "카테고리명 (맛집, 카페, 뷰티, 숙박, 식품, 화장품 등)")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "캠페인 타입 (인스타그램, 블로그, 유튜브 등)")
            @RequestParam(required = false) String campaignType,

            @Parameter(description = "페이징 정보 포함 여부")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            log.info("최신 캠페인 목록 조회 요청 - page: {}, size: {}, categoryType: {}, categoryName: {}, campaignType: {}, includePaging: {}",
                    page, size, categoryType, categoryName, campaignType, includePaging);

            var pageResponse = viewService.getCampaignListWithFilters(Math.max(0, page - 1), size, "createdAt", true, categoryType, categoryName, campaignType);
            List<CampaignListSimpleResponse> campaigns = pageResponse.getContent();

            if (includePaging) {
                CampaignListResponseWrapper responseWrapper = new CampaignListResponseWrapper();
                responseWrapper.setCampaigns(campaigns);

                CampaignListResponseWrapper.PaginationInfo paginationInfo =
                        CampaignListResponseWrapper.PaginationInfo.builder()
                                .pageNumber(pageResponse.getPageNumber())
                                .pageSize(pageResponse.getPageSize())
                                .totalPages(pageResponse.getTotalPages())
                                .totalElements(pageResponse.getTotalElements())
                                .first(pageResponse.isFirst())
                                .last(pageResponse.isLast())
                                .build();

                responseWrapper.setPagination(paginationInfo);

                return ResponseEntity.ok(BaseResponse.success(responseWrapper, "최신 캠페인 목록 조회 성공"));
            } else {
                Map<String, Object> responseData = Map.of("campaigns", campaigns);
                return ResponseEntity.ok(BaseResponse.success(responseData, "최신 캠페인 목록 조회 성공"));
            }
        } catch (Exception e) {
            log.error("최신 캠페인 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("최신 캠페인 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // ===== 세분화된 캠페인 조회 API =====

    @Operation(
            summary = "방문 캠페인 목록 조회",
            description = "방문형 캠페인 목록을 다양한 조건으로 조회합니다. **상시 캠페인도 포함**됩니다."
                    + "\n\n### 지원하는 카테고리:"
                    + "\n- **전체**: categoryName 파라미터 생략"
                    + "\n- **맛집**: categoryName=맛집"
                    + "\n- **카페**: categoryName=카페"
                    + "\n- **뷰티**: categoryName=뷰티"
                    + "\n- **숙박**: categoryName=숙박"
                    + "\n\n### 플랫폼 필터링:"
                    + "\n- **단일 플랫폼**: campaignTypes=블로그 또는 campaignTypes=인스타그램 또는 campaignTypes=유튜브"
                    + "\n- **복수 플랫폼**: campaignTypes=블로그,인스타그램 (쉼표로 구분)"
                    + "\n- **전체 플랫폼**: campaignTypes 파라미터 생략"
                    + "\n\n### 정렬 옵션:"
                    + "\n- **최신순**: sort=latest (기본값) - 상시 캠페인 포함"
                    + "\n- **인기순**: sort=popular - 상시 캠페인 포함"
                    + "\n- **선정 마감순**: sort=deadline - 상시 캠페인 제외, 마감일 있는 캠페인만"
                    + "\n\n### 상시 캠페인:"
                    + "\n- **상시 캠페인**: 모집 마감일이 없는 언제든 신청 가능한 캠페인"
                    + "\n- **deadline 정렬**: 상시 캠페인은 마감일이 없으므로 해당 정렬에서 제외됩니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignV1ListSuccessResponse"),
                            examples = @ExampleObject(
                                name = "방문형 캠페인 목록 조회 성공",
                                summary = "인기순으로 정렬된 캠페인 목록",
                                value = """
                                {
                                  "success": true,
                                  "message": "방문형 캠페인 목록 조회 성공",
                                  "status": 200,
                                  "data": {
                                    "campaigns": [
                                      {
                                        "id": 26,
                                        "isAlwaysOpen": true,
                                        "campaignType": "블로그",
                                        "title": "상시 모집 카페 체험단",
                                        "productShortInfo": "음료 1잔 + 디저트 무료 제공",
                                        "currentApplicants": 12,
                                        "maxApplicants": 30,
                                        "recruitmentEndDate": null,
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/cafe.jpg",
                                        "category": {
                                          "type": "방문",
                                          "name": "카페"
                                        }
                                      },
                                      {
                                        "id": 25,
                                        "isAlwaysOpen": false,
                                        "campaignType": "인스타그램",
                                        "title": "이탈리안 레스토랑 신메뉴 체험단",
                                        "productShortInfo": "파스타 2인분 + 와인 1병 무료 제공",
                                        "currentApplicants": 8,
                                        "maxApplicants": 15,
                                        "recruitmentEndDate": "2027-12-12",
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/%ED%8C%8C%EC%8A%A4%ED%83%80.jpg",
                                        "category": {
                                          "type": "방문",
                                          "name": "맛집"
                                        }
                                      }
                                    ],
                                    "pagination": {
                                      "pageNumber": 1,
                                      "pageSize": 10,
                                      "totalPages": 1,
                                      "totalElements": 2,
                                      "first": true,
                                      "last": true
                                    }
                                  }
                                }
                                """
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/visit")
    public ResponseEntity<?> getVisitCampaigns(
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,

            @Parameter(description = "요청할 캠페인 갯수")
            @RequestParam(required = false, defaultValue = "10") int size,

            @Parameter(description = "카테고리명 (맛집, 카페, 뷰티, 숙박). 생략시 전체")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "캠페인 플랫폼 (쉼표로 구분): 블로그, 인스타그램, 유튜브")
            @RequestParam(required = false) String campaignTypes,

            @Parameter(description = "정렬 기준 (latest, popular, deadline)")
            @RequestParam(required = false, defaultValue = "latest") String sort,

            @Parameter(description = "페이징 정보 포함 여부")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            log.info("방문 캠페인 목록 조회 요청 - page: {}, size: {}, categoryName: {}, campaignTypes: {}, sort: {}, includePaging: {}",
                    page, size, categoryName, campaignTypes, sort, includePaging);

            PageResponse<CampaignListSimpleResponse> pageResponse;

            // 플랫폼 타입들을 List로 변환 (쉼표로 구분된 문자열 처리)
            List<String> campaignTypeList = null;
            if (campaignTypes != null && !campaignTypes.trim().isEmpty()) {
                campaignTypeList = Arrays.stream(campaignTypes.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                log.info("변환된 캠페인 타입 리스트: {}", campaignTypeList);
            }

            // 정렬 기준에 따라 적절한 서비스 메서드 호출
            if ("deadline".equals(sort)) {
                // 마감 임박순은 별도 메서드 사용
                pageResponse = viewService.getCampaignListByDeadlineSoonWithCampaignTypes(
                        Math.max(0, page - 1), size, "방문", categoryName, campaignTypeList);
            } else {
                // 최신순, 인기순은 통합 메서드 사용
                pageResponse = viewService.getCampaignListWithAllFilters(
                        Math.max(0, page - 1), size, convertSortParameter(sort), true, 
                        "방문", categoryName, campaignTypeList);
            }

            List<CampaignListSimpleResponse> campaigns = pageResponse.getContent();

            if (includePaging) {
                CampaignListResponseWrapper responseWrapper = new CampaignListResponseWrapper();
                responseWrapper.setCampaigns(campaigns);

                CampaignListResponseWrapper.PaginationInfo paginationInfo =
                        CampaignListResponseWrapper.PaginationInfo.builder()
                                .pageNumber(pageResponse.getPageNumber())
                                .pageSize(pageResponse.getPageSize())
                                .totalPages(pageResponse.getTotalPages())
                                .totalElements(pageResponse.getTotalElements())
                                .first(pageResponse.isFirst())
                                .last(pageResponse.isLast())
                                .build();

                responseWrapper.setPagination(paginationInfo);

                return ResponseEntity.ok(BaseResponse.success(responseWrapper, "방문 캠페인 목록 조회 성공"));
            } else {
                Map<String, Object> responseData = Map.of("campaigns", campaigns);
                return ResponseEntity.ok(BaseResponse.success(responseData, "방문 캠페인 목록 조회 성공"));
            }
        } catch (Exception e) {
            log.error("방문 캠페인 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("방문 캠페인 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "배송 캠페인 목록 조회",
            description = "배송형 캠페인 목록을 다양한 조건으로 조회합니다. **상시 캠페인도 포함**됩니다."
                    + "\n\n### 지원하는 카테고리:"
                    + "\n- **전체**: categoryName 파라미터 생략"
                    + "\n- **식품**: categoryName=식품"
                    + "\n- **화장품**: categoryName=화장품"
                    + "\n- **생활용품**: categoryName=생활용품"
                    + "\n- **패션**: categoryName=패션"
                    + "\n- **잡화**: categoryName=잡화"
                    + "\n\n### 플랫폼 필터링:"
                    + "\n- **단일 플랫폼**: campaignTypes=블로그 또는 campaignTypes=인스타그램 또는 campaignTypes=유튜브"
                    + "\n- **복수 플랫폼**: campaignTypes=블로그,인스타그램 (쉼표로 구분)"
                    + "\n- **전체 플랫폼**: campaignTypes 파라미터 생략"
                    + "\n\n### 정렬 옵션:"
                    + "\n- **최신순**: sort=latest (기본값) - 상시 캠페인 포함"
                    + "\n- **인기순**: sort=popular - 상시 캠페인 포함"
                    + "\n- **선정 마감순**: sort=deadline - 상시 캠페인 제외, 마감일 있는 캠페인만"
                    + "\n\n### 상시 캠페인:"
                    + "\n- **상시 캠페인**: 모집 마감일이 없는 언제든 신청 가능한 캠페인"
                    + "\n- **deadline 정렬**: 상시 캠페인은 마감일이 없으므로 해당 정렬에서 제외됩니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignV1ListSuccessResponse"),
                            examples = @ExampleObject(
                                name = "배송형 캠페인 목록 조회 성공",
                                summary = "인기순으로 정렬된 캠페인 목록",
                                value = """
                                {
                                  "success": true,
                                  "message": "배송 캠페인 목록 조회 성공",
                                  "status": 200,
                                  "data": {
                                    "campaigns": [
                                      {
                                        "id": 28,
                                        "isAlwaysOpen": true,
                                        "campaignType": "인스타그램",
                                        "title": "상시 모집 뷰티 제품 체험단",
                                        "productShortInfo": "립스틱 + 파운데이션 세트",
                                        "currentApplicants": 25,
                                        "maxApplicants": 50,
                                        "recruitmentEndDate": null,
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/beauty.jpg",
                                        "category": {
                                          "type": "배송",
                                          "name": "화장품"
                                        }
                                      },
                                      {
                                        "id": 27,
                                        "isAlwaysOpen": false,
                                        "campaignType": "블로그",
                                        "title": "프리미엄 스킨케어 세트 체험단",
                                        "productShortInfo": "토너 + 에센스 + 크림 세트",
                                        "currentApplicants": 15,
                                        "maxApplicants": 20,
                                        "recruitmentEndDate": "2027-12-20",
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/skincare.jpg",
                                        "category": {
                                          "type": "배송",
                                          "name": "화장품"
                                        }
                                      }
                                    ],
                                    "pagination": {
                                      "pageNumber": 1,
                                      "pageSize": 10,
                                      "totalPages": 1,
                                      "totalElements": 2,
                                      "first": true,
                                      "last": true
                                    }
                                  }
                                }
                                """
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/delivery")
    public ResponseEntity<?> getDeliveryCampaigns(
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,

            @Parameter(description = "요청할 캠페인 갯수")
            @RequestParam(required = false, defaultValue = "10") int size,

            @Parameter(description = "카테고리명 (식품, 화장품, 생활용품, 패션, 잡화). 생략시 전체")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "캠페인 플랫폼 (쉼표로 구분): 블로그, 인스타그램, 유튜브")
            @RequestParam(required = false) String campaignTypes,

            @Parameter(description = "정렬 기준 (latest, popular, deadline)")
            @RequestParam(required = false, defaultValue = "latest") String sort,

            @Parameter(description = "페이징 정보 포함 여부")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            log.info("배송 캠페인 목록 조회 요청 - page: {}, size: {}, categoryName: {}, campaignTypes: {}, sort: {}, includePaging: {}",
                    page, size, categoryName, campaignTypes, sort, includePaging);

            PageResponse<CampaignListSimpleResponse> pageResponse;

            // 플랫폼 타입들을 List로 변환 (쉼표로 구분된 문자열 처리)
            List<String> campaignTypeList = null;
            if (campaignTypes != null && !campaignTypes.trim().isEmpty()) {
                campaignTypeList = Arrays.stream(campaignTypes.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                log.info("변환된 캠페인 타입 리스트: {}", campaignTypeList);
            }

            // 정렬 기준에 따라 적절한 서비스 메서드 호출
            if ("deadline".equals(sort)) {
                // 마감 임박순은 별도 메서드 사용
                pageResponse = viewService.getCampaignListByDeadlineSoonWithCampaignTypes(
                        Math.max(0, page - 1), size, "배송", categoryName, campaignTypeList);
            } else {
                // 최신순, 인기순은 통합 메서드 사용
                pageResponse = viewService.getCampaignListWithAllFilters(
                        Math.max(0, page - 1), size, convertSortParameter(sort), true, 
                        "배송", categoryName, campaignTypeList);
            }

            List<CampaignListSimpleResponse> campaigns = pageResponse.getContent();

            if (includePaging) {
                CampaignListResponseWrapper responseWrapper = new CampaignListResponseWrapper();
                responseWrapper.setCampaigns(campaigns);

                CampaignListResponseWrapper.PaginationInfo paginationInfo =
                        CampaignListResponseWrapper.PaginationInfo.builder()
                                .pageNumber(pageResponse.getPageNumber())
                                .pageSize(pageResponse.getPageSize())
                                .totalPages(pageResponse.getTotalPages())
                                .totalElements(pageResponse.getTotalElements())
                                .first(pageResponse.isFirst())
                                .last(pageResponse.isLast())
                                .build();

                responseWrapper.setPagination(paginationInfo);

                return ResponseEntity.ok(BaseResponse.success(responseWrapper, "배송 캠페인 목록 조회 성공"));
            } else {
                Map<String, Object> responseData = Map.of("campaigns", campaigns);
                return ResponseEntity.ok(BaseResponse.success(responseData, "배송 캠페인 목록 조회 성공"));
            }
        } catch (Exception e) {
            log.error("배송 캠페인 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("배송 캠페인 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // ===== 캠페인 상세 조회 API =====

    @Operation(
            summary = "캠페인 썸네일 조회",
            description = "특정 캠페인의 썸네일 이미지 URL을 조회합니다."
                    + "\n\n**접근 제한**: 승인된 캠페인만 조회 가능하며, 거절된 캠페인은 접근할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignThumbnailResponse"),
                            examples = @ExampleObject(
                                name = "캠페인 썸네일 조회 성공",
                                summary = "캠페인 썸네일 조회 성공",
                                value = """
                                        {
                                          "success": true,
                                          "message": "캠페인 썸네일 조회 성공",
                                          "status": 200,
                                          "data": {
                                            "campaignId": 32,
                                            "thumbnailUrl": "https://drxgfm74s70w1.cloudfront.net/campaign-images/%EB%A9%94%EC%9D%B4%ED%81%AC%EC%97%85.jpg"
                                          }
                                        }
                                """
                            ))),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/{campaignId}/thumbnail")
    public ResponseEntity<?> getCampaignThumbnail(
            @Parameter(description = "캠페인 ID")
            @PathVariable Long campaignId
    ) {
        try {
            log.info("캠페인 썸네일 조회 요청 - campaignId: {}", campaignId);

            CampaignThumbnailResponse response = viewService.getCampaignThumbnail(campaignId);
            return ResponseEntity.ok(BaseResponse.success(response, "캠페인 썸네일 조회 성공"));
        } catch (Exception e) {
            log.error("캠페인 썸네일 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail("캠페인을 찾을 수 없습니다.", "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        }
    }

    @Operation(
            summary = "캠페인 기본 정보 조회",
            description = "캠페인의 기본 정보(타입, 카테고리, 제목, 신청 인원, 모집 기간)를 조회합니다."
                    + "\n\n**접근 제한**: 승인된 캠페인만 조회 가능하며, 거절된 캠페인은 접근할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignBasicInfoResponse"),
                            examples = @ExampleObject(
                                name = "캠페인 기본 정보 조회 성공",
                                summary = "캠페인 기본 정보 조회 성공",
                                value = """
                                        {
                                          "success": true,
                                          "message": "캠페인 기본 정보 조회 성공",
                                          "status": 200,
                                          "data": {
                                            "campaignId": 32,
                                            "campaignType": "블로그",
                                            "categoryType": "배송",
                                            "categoryName": "화장품",
                                            "title": "프리미엄 메이크업 팔레트 체험단",
                                            "maxApplicants": 60,
                                            "currentApplicants": 0,
                                            "recruitmentStartDate": "2025-06-09",
                                            "recruitmentEndDate": "2027-12-12"
                                          }
                                        }
                                """
                            ))),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/{campaignId}/basic-info")
    public ResponseEntity<?> getCampaignBasicInfo(
            @Parameter(description = "캠페인 ID")
            @PathVariable Long campaignId
    ) {
        try {
            log.info("캠페인 기본 정보 조회 요청 - campaignId: {}", campaignId);

            CampaignBasicInfoResponse response = viewService.getCampaignBasicInfo(campaignId);
            return ResponseEntity.ok(BaseResponse.success(response, "캠페인 기본 정보 조회 성공"));
        } catch (Exception e) {
            log.error("캠페인 기본 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail("캠페인을 찾을 수 없습니다.", "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        }
    }

    @Operation(
            summary = "캠페인 상세 정보 조회",
            description = "캠페인의 상세 정보(제품/서비스 정보, 선정기준, 일정)를 조회합니다."
                    + "\n\n**접근 제한**: 승인된 캠페인만 조회 가능하며, 거절된 캠페인은 접근할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignDetailInfoResponse"),
                            examples = @ExampleObject(
                                    name = "캠페인 상세 정보 조회 성공",
                                    summary = "캠페인 상세 정보 조회 성공",
                                    value = """
                                           {
                                             "success": true,
                                             "message": "캠페인 상세 정보 조회 성공",
                                             "status": 200,
                                             "data": {
                                               "campaignId": 32,
                                               "isAlwaysOpen": false,
                                               "productShortInfo": "아이섀도우 팔레트 + 립 제품 세트",
                                               "productDetails": "18색 아이섀도우 팔레트와 립스틱, 립글로스로 구성된 메이크업 세트를 제공합니다. 다양한 룩을 연출하며 발색과 지속력을 테스트해보세요.",
                                               "selectionCriteria": "메이크업 관련 블로그 운영, 뷰티 포스팅 경험 필수",
                                               "selectionDate": "2027-12-13"
                                             }
                                           }
                                   """
                            ))),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/{campaignId}/detail-info")
    public ResponseEntity<?> getCampaignDetailInfo(
            @Parameter(description = "캠페인 ID")
            @PathVariable Long campaignId
    ) {
        try {
            log.info("캠페인 상세 정보 조회 요청 - campaignId: {}", campaignId);

            CampaignDetailInfoResponse response = viewService.getCampaignDetailInfo(campaignId);
            return ResponseEntity.ok(BaseResponse.success(response, "캠페인 상세 정보 조회 성공"));
        } catch (Exception e) {
            log.error("캠페인 상세 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail("캠페인을 찾을 수 없습니다.", "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        }
    }

    @Operation(
            summary = "캠페인 미션 가이드 및 정보 조회",
            description = "캠페인의 미션 가이드와 상세 미션 정보를 조회합니다."
                    + "\n\n**반환 정보:**"
                    + "\n- **미션 가이드**: 인플루언서가 수행해야 할 미션 안내"
                    + "\n- **키워드 정보**: 제목, 본문, 지역 키워드"
                    + "\n- **콘텐츠 요구사항**: 영상, 이미지, 텍스트 개수 및 지도 포함 여부"
                    + "\n- **미션 일정**: 미션 시작일과 마감일"
                    + "\n\n**접근 제한**: 승인된 캠페인만 조회 가능하며, 거절된 캠페인은 접근할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignMissionGuideResponse"),
                            examples = @ExampleObject(
                                name = "캠페인 미션 가이드 및 정보 조회 성공",
                                summary = "캠페인의 미션 가이드와 상세 정보 조회 성공",
                                value = """
                                {
                                  "success": true,
                                  "message": "캠페인 미션 가이드 조회 성공",
                                  "status": 200,
                                  "data": {
                                    "campaignId": 22,
                                    "missionInfo": {
                                      "missionGuide": "인스타그램에 멋진 포스팅을 남겨주세요.",
                                      "titleKeywords": ["맛집", "체험단", "후기"],
                                      "bodyKeywords": ["맛있는", "추천", "방문후기"],
                                      "locationKeywords": ["강남", "서울"],
                                      "numberOfVideo": 1,
                                      "numberOfImage": 5,
                                      "numberOfText": 500,
                                      "isMap": true,
                                      "missionStartDate": "2024-01-15",
                                      "missionDeadlineDate": "2024-01-30"
                                    }
                                  }
                                }
                                """
                            ))),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/{campaignId}/mission-guide")
    public ResponseEntity<?> getCampaignMissionGuide(
            @Parameter(description = "캠페인 ID")
            @PathVariable Long campaignId
    ) {
        try {
            log.info("캠페인 미션 가이드 및 정보 조회 요청 - campaignId: {}", campaignId);

            CampaignMissionGuideResponse response = viewService.getCampaignMissionGuide(campaignId);
            return ResponseEntity.ok(BaseResponse.success(response, "캠페인 미션 가이드 조회 성공"));
        } catch (Exception e) {
            log.error("캠페인 미션 가이드 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail("캠페인을 찾을 수 없습니다.", "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        }
    }

    // ===== 캠페인 검색 API =====

    @Operation(
            summary = "캠페인 검색",
            description = "키워드로 캠페인을 검색합니다. **상시 캠페인도 포함**됩니다."
                    + "\n\n### 검색 기능:"
                    + "\n- **키워드**: 캠페인 제목에서 검색"
                    + "\n- **정렬**: 최신순(latest) 또는 인기순(popular)"
                    + "\n- **플랫폼 필터링**: 특정 플랫폼으로 필터링 가능"
                    + "\n- **페이징**: 페이지별 조회 지원"
                    + "\n- **통계 수집**: 검색어를 실시간 인기 검색어에 자동 반영"
                    + "\n\n### 상시 캠페인:"
                    + "\n- **상시 캠페인**: 모집 마감일이 없는 언제든 신청 가능한 캠페인도 검색 결과에 포함됩니다"
                    + "\n- **일반 캠페인**: 모집 마감일이 설정된 기간 한정 캠페인"
                    + "\n\n### 사용 예시:"
                    + "\n- `keyword=맛집&sort=popular` - '맛집' 캠페인을 인기순으로 검색"
                    + "\n- `keyword=화장품&sort=latest&campaignTypes=인스타그램,블로그` - '화장품' 캠페인을 인스타그램+블로그로 필터링하여 최신순 검색"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignV1ListSuccessResponse"),
                            examples = @ExampleObject(
                                name = "캠페인 검색 성공",
                                summary = "검색 키워드에 맞는 캠페인 목록 조회",
                                value = """
                                {
                                  "success": true,
                                  "message": "캠페인 검색 성공",
                                  "status": 200,
                                  "data": {
                                    "campaigns": [
                                      {
                                        "id": 25,
                                        "campaignType": "인스타그램",
                                        "title": "이탈리안 레스토랑 신메뉴 체험단",
                                        "productShortInfo": "파스타 2인분 + 와인 1병 무료 제공",
                                        "currentApplicants": 8,
                                        "maxApplicants": 15,
                                        "recruitmentEndDate": "2027-12-12",
                                        "thumbnailUrl": "https://ckokservice.s3.ap-northeast-2.amazonaws.com/campaign-images/%ED%8C%8C%EC%8A%A4%ED%83%80.jpg",
                                        "category": {
                                          "type": "방문",
                                          "name": "맛집"
                                        }
                                      }
                                    ],
                                    "pagination": {
                                      "pageNumber": 1,
                                      "pageSize": 10,
                                      "totalPages": 1,
                                      "totalElements": 1,
                                      "first": true,
                                      "last": true
                                    }
                                  }
                                }
                                """
                            ))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (키워드 누락)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchCampaigns(
            @Parameter(description = "검색 키워드 (필수)", required = true)
            @RequestParam String keyword,

            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,

            @Parameter(description = "요청할 캠페인 갯수")
            @RequestParam(required = false, defaultValue = "10") int size,

            @Parameter(description = "정렬 기준 (latest: 최신순, popular: 인기순)")
            @RequestParam(required = false, defaultValue = "latest") String sort,

            @Parameter(description = "캠페인 플랫폼 (쉼표로 구분): 인스타그램, 블로그, 유튜브, 틱톡")
            @RequestParam(required = false) String campaignTypes,

            @Parameter(description = "페이징 정보 포함 여부")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            // 키워드 검증
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("검색 키워드는 필수입니다.", "INVALID_KEYWORD", HttpStatus.BAD_REQUEST.value()));
            }

            log.info("캠페인 검색 요청 - keyword: {}, page: {}, size: {}, sort: {}, campaignTypes: {}, includePaging: {}",
                    keyword, page, size, sort, campaignTypes, includePaging);

            // 검색 통계 수집
            searchAnalyticsService.recordSearch(keyword.trim());

            // 플랫폼 타입들을 List로 변환 (쉼표로 구분된 문자열 처리)
            List<String> campaignTypeList = null;
            if (campaignTypes != null && !campaignTypes.trim().isEmpty()) {
                campaignTypeList = Arrays.stream(campaignTypes.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                log.info("변환된 캠페인 타입 리스트: {}", campaignTypeList);
            }

            // 정렬 기준 변환
            String sortField = convertSortParameter(sort);
            boolean isDescending = true; // 인기순, 최신순 모두 내림차순

            var pageResponse = viewService.searchCampaignsWithFilters(
                    keyword.trim(), Math.max(0, page - 1), size, sortField, isDescending, campaignTypeList);

            List<CampaignListSimpleResponse> campaigns = pageResponse.getContent();

            if (includePaging) {
                CampaignListResponseWrapper responseWrapper = new CampaignListResponseWrapper();
                responseWrapper.setCampaigns(campaigns);

                CampaignListResponseWrapper.PaginationInfo paginationInfo =
                        CampaignListResponseWrapper.PaginationInfo.builder()
                                .pageNumber(pageResponse.getPageNumber())
                                .pageSize(pageResponse.getPageSize())
                                .totalPages(pageResponse.getTotalPages())
                                .totalElements(pageResponse.getTotalElements())
                                .first(pageResponse.isFirst())
                                .last(pageResponse.isLast())
                                .build();

                responseWrapper.setPagination(paginationInfo);

                return ResponseEntity.ok(BaseResponse.success(responseWrapper, "캠페인 검색 성공"));
            } else {
                Map<String, Object> responseData = Map.of("campaigns", campaigns);
                return ResponseEntity.ok(BaseResponse.success(responseData, "캠페인 검색 성공"));
            }
        } catch (Exception e) {
            log.error("캠페인 검색 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 검색 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }


}