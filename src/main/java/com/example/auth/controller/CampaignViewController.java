package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.constant.UserRole;
import com.example.auth.dto.campaign.*;
import com.example.auth.dto.campaign.view.*;
import com.example.auth.service.CampaignViewService;
import com.example.auth.service.SearchAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "캠페인 조회 API", description = "캠페인 조회 관련 API")
public class CampaignViewController {

    private final CampaignViewService viewService;
    private final SearchAnalyticsService searchAnalyticsService;

    // ===== 인기순/마감순 특화 API =====

    @Operation(
            summary = "인기 캠페인 목록 조회",
            description = "신청 인원이 많은 순으로 캠페인을 조회합니다."
                    + "\n\n### 필터링 옵션:"
                    + "\n- **카테고리 타입**: categoryType=방문 또는 categoryType=배송"
                    + "\n- **카테고리명**: categoryName=맛집, 카페, 뷰티, 숙박, 식품, 화장품 등"
                    + "\n- **캠페인 타입**: campaignType=인스타그램, 블로그, 유튜브 등"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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
            description = "신청 마감일이 가까운 순으로 캠페인을 조회합니다."
                    + "\n\n### 필터링 옵션:"
                    + "\n- **카테고리 타입**: categoryType=방문 또는 categoryType=배송"
                    + "\n- **카테고리명**: categoryName=맛집, 카페, 뷰티, 숙박, 식품, 화장품 등"
                    + "\n- **캠페인 타입**: campaignType=인스타그램, 블로그, 유튜브 등"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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
            description = "최신 등록순으로 캠페인을 조회합니다."
                    + "\n\n### 필터링 옵션:"
                    + "\n- **카테고리 타입**: categoryType=방문 또는 categoryType=배송"
                    + "\n- **카테고리명**: categoryName=맛집, 카페, 뷰티, 숙박, 식품, 화장품 등"
                    + "\n- **캠페인 타입**: campaignType=인스타그램, 블로그, 유튜브 등"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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
            description = "방문형 캠페인 목록을 다양한 조건으로 조회합니다."
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
                    + "\n- **최신순**: sort=latest (기본값)"
                    + "\n- **선정 마감순**: sort=deadline"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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

            @Parameter(description = "정렬 기준 (latest, deadline)")
            @RequestParam(required = false, defaultValue = "latest") String sort,

            @Parameter(description = "페이징 정보 포함 여부")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            log.info("방문 캠페인 목록 조회 요청 - page: {}, size: {}, categoryName: {}, campaignTypes: {}, sort: {}, includePaging: {}",
                    page, size, categoryName, campaignTypes, sort, includePaging);

            var pageResponse = viewService.getFilteredCampaignList(
                    Math.max(0, page - 1), size, "방문", categoryName, campaignTypes, sort);

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
            description = "배송형 캠페인 목록을 다양한 조건으로 조회합니다."
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
                    + "\n- **최신순**: sort=latest (기본값)"
                    + "\n- **선정 마감순**: sort=deadline"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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

            @Parameter(description = "정렬 기준 (latest, deadline)")
            @RequestParam(required = false, defaultValue = "latest") String sort,

            @Parameter(description = "페이징 정보 포함 여부")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            log.info("배송 캠페인 목록 조회 요청 - page: {}, size: {}, categoryName: {}, campaignTypes: {}, sort: {}, includePaging: {}",
                    page, size, categoryName, campaignTypes, sort, includePaging);

            var pageResponse = viewService.getFilteredCampaignList(
                    Math.max(0, page - 1), size, "배송", categoryName, campaignTypes, sort);

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
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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
            summary = "캠페인 미션 가이드 조회",
            description = "캠페인의 미션 가이드 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{campaignId}/mission-guide")
    public ResponseEntity<?> getCampaignMissionGuide(
            @Parameter(description = "캠페인 ID")
            @PathVariable Long campaignId
    ) {
        try {
            log.info("캠페인 미션 가이드 조회 요청 - campaignId: {}", campaignId);

            CampaignMissionGuideResponse response = viewService.getCampaignMissionGuide(campaignId);
            return ResponseEntity.ok(BaseResponse.success(response, "캠페인 미션 가이드 조회 성공"));
        } catch (Exception e) {
            log.error("캠페인 미션 가이드 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail("캠페인을 찾을 수 없습니다.", "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        }
    }

    @Operation(
            summary = "캠페인 필수 키워드 조회",
            description = "캠페인의 필수 포함 키워드 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{campaignId}/keywords")
    public ResponseEntity<?> getCampaignKeywords(
            @Parameter(description = "캠페인 ID")
            @PathVariable Long campaignId
    ) {
        try {
            log.info("캠페인 필수 키워드 조회 요청 - campaignId: {}", campaignId);

            CampaignKeywordsResponse response = viewService.getCampaignKeywords(campaignId);
            return ResponseEntity.ok(BaseResponse.success(response, "캠페인 필수 키워드 조회 성공"));
        } catch (Exception e) {
            log.error("캠페인 필수 키워드 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail("캠페인을 찾을 수 없습니다.", "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        }
    }

    // ===== 캠페인 검색 API =====

    @Operation(
            summary = "캠페인 검색",
            description = "키워드로 캠페인을 검색합니다."
                    + "\n\n### 검색 기능:"
                    + "\n- **키워드**: 캠페인 제목에서 검색"
                    + "\n- **정렬**: 최신순으로 고정"
                    + "\n- **페이징**: 페이지별 조회 지원"
                    + "\n- **통계 수집**: 검색어를 실시간 인기 검색어에 자동 반영"
                    + "\n\n### 사용 예시:"
                    + "\n- `keyword=맛집` - '맛집'이 포함된 캠페인 검색"
                    + "\n- `keyword=화장품` - '화장품' 캠페인을 최신순으로 검색"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (키워드 누락)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchCampaigns(
            @Parameter(description = "검색 키워드 (필수)", required = true)
            @RequestParam String keyword,

            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,

            @Parameter(description = "요청할 캠페인 갯수")
            @RequestParam(required = false, defaultValue = "10") int size,

            @Parameter(description = "페이징 정보 포함 여부")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            // 키워드 검증
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("검색 키워드는 필수입니다.", "INVALID_KEYWORD", HttpStatus.BAD_REQUEST.value()));
            }

            log.info("캠페인 검색 요청 - keyword: {}, page: {}, size: {}, includePaging: {}",
                    keyword, page, size, includePaging);

            // 검색 통계 수집
            searchAnalyticsService.recordSearch(keyword.trim());

            // 최신순으로 고정
            var pageResponse = viewService.searchCampaigns(
                    keyword.trim(), Math.max(0, page - 1), size, "latest");

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