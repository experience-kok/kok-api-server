package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.campaign.CampaignListResponseWrapper;
import com.example.auth.dto.campaign.CampaignListSimpleResponse;
import com.example.auth.dto.campaign.CampaignSingleResponseWrapper;
import com.example.auth.dto.campaign.view.*;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.service.CampaignViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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

@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "캠페인 조회 API", description = "캠페인 조회 관련 API")
public class CampaignViewController {

    private final CampaignViewService viewService;
    
    @Operation(
        summary = "캠페인 목록 조회", 
        description = "캠페인 목록을 페이징하여 조회합니다."
                    + "\n\n### 응답 형식 선택 (includePaging 파라미터):"
                    + "\n- **true (기본값)**: 페이징 정보를 포함한 전체 응답 구조"
                    + "\n- **false**: 요청한 갯수만큼의 캠페인 목록 데이터만 반환"
                    + "\n\n### 페이지네이션 응답 설명 (includePaging=true인 경우):"
                    + "\n- **campaigns**: 캠페인 목록"
                    + "\n  - **id**: 캠페인 ID"
                    + "\n  - **campaignType**: 캠페인 타입 (인스타그램, 블로그 등)"
                    + "\n  - **title**: 캠페인 제목"
                    + "\n  - **currentApplicants**: 현재 신청 인원"
                    + "\n  - **maxApplicants**: 최대 신청 가능 인원"
                    + "\n  - **createdAt**: 등록일"
                    + "\n  - **applicationDeadlineDate**: 신청 마감일"
                    + "\n  - **thumbnailUrl**: 썸네일 이미지 URL"
                    + "\n  - **category**: 카테고리 정보"
                    + "\n    - **type**: 카테고리 타입 (방문, 배송 등)"
                    + "\n    - **name**: 카테고리 이름 (카페, 맛집 등)"
                    + "\n- **pagination**: 페이징 정보 객체"
                    + "\n  - **pageNumber**: 현재 페이지 번호 (0부터 시작)"
                    + "\n  - **pageSize**: 페이지 크기 (한 페이지에 표시되는 항목 수)"
                    + "\n  - **totalPages**: 전체 페이지 수"
                    + "\n  - **totalElements**: 전체 항목 수"
                    + "\n  - **first**: 현재 페이지가 첫 번째 페이지인지 여부"
                    + "\n  - **last**: 현재 페이지가 마지막 페이지인지 여부"
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "성공 응답 예시 (includePaging=true)",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"캠페인 목록 조회 성공\",\n" +
                               "  \"data\": {\n" +
                               "    \"campaigns\": [\n" +
                               "      {\n" +
                               "        \"id\": 1,\n" +
                               "        \"campaignType\": \"인스타그램\",\n" +
                               "        \"title\": \"인스타 감성 카페 체험단 모집\",\n" +
                               "        \"currentApplicants\": 12,\n" +
                               "        \"maxApplicants\": 20,\n" +
                               "        \"createdAt\": \"2025-05-01T09:30:00+09:00\",\n" +
                               "        \"applicationDeadlineDate\": \"2025-06-15\",\n" +
                               "        \"thumbnailUrl\": \"https://example.com/images/cafe.jpg\",\n" +
                               "        \"category\": {\n" +
                               "          \"type\": \"방문\",\n" +
                               "          \"name\": \"카페\"\n" +
                               "        }\n" +
                               "      },\n" +
                               "      {\n" +
                               "        \"id\": 2,\n" +
                               "        \"campaignType\": \"블로그\",\n" +
                               "        \"title\": \"유기농 식품 체험단 모집\",\n" +
                               "        \"currentApplicants\": 8,\n" +
                               "        \"maxApplicants\": 15,\n" +
                               "        \"createdAt\": \"2025-05-05T10:15:00+09:00\",\n" +
                               "        \"applicationDeadlineDate\": \"2025-06-20\",\n" +
                               "        \"thumbnailUrl\": \"https://example.com/images/food.jpg\",\n" +
                               "        \"category\": {\n" +
                               "          \"type\": \"배송\",\n" +
                               "          \"name\": \"식품\"\n" +
                               "        }\n" +
                               "      }\n" +
                               "    ],\n" +
                               "    \"pagination\": {\n" +
                               "      \"pageNumber\": 0,\n" +
                               "      \"pageSize\": 10,\n" +
                               "      \"totalPages\": 5,\n" +
                               "      \"totalElements\": 42,\n" +
                               "      \"first\": true,\n" +
                               "      \"last\": false\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공 (includePaging=false)",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "성공 응답 예시 (includePaging=false)",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"캠페인 목록 조회 성공\",\n" +
                               "  \"data\": [\n" +
                               "    {\n" +
                               "      \"id\": 1,\n" +
                               "      \"campaignType\": \"인스타그램\",\n" +
                               "      \"title\": \"인스타 감성 카페 체험단 모집\",\n" +
                               "      \"currentApplicants\": 12,\n" +
                               "      \"maxApplicants\": 20,\n" +
                               "      \"createdAt\": \"2025-05-01T09:30:00+09:00\",\n" +
                               "      \"applicationDeadlineDate\": \"2025-06-15\",\n" +
                               "      \"thumbnailUrl\": \"https://example.com/images/cafe.jpg\",\n" +
                               "      \"category\": {\n" +
                               "        \"type\": \"방문\",\n" +
                               "        \"name\": \"카페\"\n" +
                               "      }\n" +
                               "    },\n" +
                               "    {\n" +
                               "      \"id\": 2,\n" +
                               "      \"campaignType\": \"블로그\",\n" +
                               "      \"title\": \"유기농 식품 체험단 모집\",\n" +
                               "      \"currentApplicants\": 8,\n" +
                               "      \"maxApplicants\": 15,\n" +
                               "      \"createdAt\": \"2025-05-05T10:15:00+09:00\",\n" +
                               "      \"applicationDeadlineDate\": \"2025-06-20\",\n" +
                               "      \"thumbnailUrl\": \"https://example.com/images/food.jpg\",\n" +
                               "      \"category\": {\n" +
                               "        \"type\": \"배송\",\n" +
                               "        \"name\": \"식품\"\n" +
                               "      }\n" +
                               "    }\n" +
                               "  ]\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버 오류"
            )
    })
    @GetMapping
    public ResponseEntity<?> getCampaignList(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "요청할 캠페인 갯수")
            @RequestParam(required = false, defaultValue = "10") int size,

            @Parameter(description = "정렬 기준 (createdAt, applicationDeadlineDate 등)")
            @RequestParam(required = false, defaultValue = "createdAt") String sort,

            @Parameter(description = "마감되지 않은 캠페인만 조회")
            @RequestParam(required = false, defaultValue = "true") boolean onlyActive,

            @Parameter(description = "카테고리 타입 필터 (방문, 배송 등)")
            @RequestParam(required = false) String categoryType,

            @Parameter(description = "캠페인 타입 필터 (인스타그램, 블로그 등)")
            @RequestParam(required = false) String campaignType,

            @Parameter(description = "페이징 정보 포함 여부 (true: 페이징 정보 포함, false: 콘텐츠만 반환)")
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            log.info("캠페인 목록 조회 요청 - page: {}, size: {}, sort: {}, onlyActive: {}, categoryType: {}, campaignType: {}, includePaging: {}",
                    page, size, sort, onlyActive, categoryType, campaignType, includePaging);

            // 서비스 호출하여 목록 조회
            var pageResponse = viewService.getCampaignList(page, size, sort, onlyActive, categoryType, campaignType);
            
            // 직접 캠페인 목록 사용 (중첩 구조 제거)
            List<CampaignListSimpleResponse> campaigns = pageResponse.getContent();

            if (includePaging) {
                // 페이징 정보를 포함한 응답 구조 생성
                CampaignListResponseWrapper responseWrapper = new CampaignListResponseWrapper();
                responseWrapper.setCampaigns(campaigns);
                
                // 페이징 정보 설정
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
                
                return ResponseEntity.ok(BaseResponse.success(responseWrapper, "캠페인 목록 조회 성공"));
            } else {
                // 페이징 정보 없이 캠페인 목록만 반환
                return ResponseEntity.ok(BaseResponse.success(campaigns, "캠페인 목록 조회 성공"));
            }
        } catch (Exception e) {
            log.error("캠페인 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 썸네일 조회", 
        description = "캠페인 썸네일 이미지 URL을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "썸네일 조회 성공 응답 예시",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"썸네일 조회 성공\",\n" +
                               "  \"data\": {\n" +
                               "    \"campaign\": {\n" +
                               "      \"id\": 1,\n" +
                               "      \"thumbnailUrl\": \"https://example.com/images/cafe.jpg\"\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{campaignId}/thumbnail")
    public ResponseEntity<?> getThumbnail(@PathVariable Long campaignId) {
        try {
            ThumbnailResponse response = viewService.getThumbnail(campaignId);
            return ResponseEntity.ok(BaseResponse.success(
                    CampaignSingleResponseWrapper.of(response), 
                    "썸네일 조회 성공"
            ));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인을 찾을 수 없음: campaignId={}", campaignId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("썸네일 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 기본 정보 조회", 
        description = "캠페인 타입, 제목, 신청 인원, 마감일 등의 기본 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "기본 정보 조회 성공 응답 예시",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"기본 정보 조회 성공\",\n" +
                               "  \"data\": {\n" +
                               "    \"campaign\": {\n" +
                               "      \"id\": 1,\n" +
                               "      \"campaignType\": \"인스타그램\",\n" +
                               "      \"title\": \"인스타 감성 카페 체험단 모집\",\n" +
                               "      \"currentApplicants\": 12,\n" +
                               "      \"maxApplicants\": 20,\n" +
                               "      \"applicationDeadlineDate\": \"2025-06-15\",\n" +
                               "      \"category\": {\n" +
                               "        \"type\": \"방문\",\n" +
                               "        \"name\": \"카페\"\n" +
                               "      }\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{campaignId}/basic-info")
    public ResponseEntity<?> getBasicInfo(@PathVariable Long campaignId) {
        try {
            BasicInfoResponse response = viewService.getBasicInfo(campaignId);
            return ResponseEntity.ok(BaseResponse.success(
                    CampaignSingleResponseWrapper.of(response), 
                    "기본 정보 조회 성공"
            ));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인을 찾을 수 없음: campaignId={}", campaignId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("기본 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 제품 및 일정 정보 조회", 
        description = "제공 제품 정보와 모집/선정 일정을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "제품 및 일정 정보 조회 성공 응답 예시",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"제품 및 일정 정보 조회 성공\",\n" +
                               "  \"data\": {\n" +
                               "    \"campaign\": {\n" +
                               "      \"id\": 1,\n" +
                               "      \"productShortInfo\": \"시그니처 음료 2잔 무료 제공\",\n" +
                               "      \"productDetails\": \"인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.\",\n" +
                               "      \"recruitmentStartDate\": \"2025-05-01\",\n" +
                               "      \"recruitmentEndDate\": \"2025-05-15\",\n" +
                               "      \"selectionDate\": \"2025-05-16\",\n" +
                               "      \"reviewDeadlineDate\": \"2025-05-30\"\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{campaignId}/product-schedule")
    public ResponseEntity<?> getProductAndSchedule(@PathVariable Long campaignId) {
        try {
            ProductAndScheduleResponse response = viewService.getProductAndSchedule(campaignId);
            return ResponseEntity.ok(BaseResponse.success(
                    CampaignSingleResponseWrapper.of(response), 
                    "제품 및 일정 정보 조회 성공"
            ));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인을 찾을 수 없음: campaignId={}", campaignId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("제품 및 일정 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 업체 정보 조회", 
        description = "업체/브랜드 정보와 캠페인 등록자(크리에이터) 닉네임을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "업체 정보 조회 성공 응답 예시",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"업체 정보 조회 성공\",\n" +
                               "  \"data\": {\n" +
                               "    \"campaign\": {\n" +
                               "      \"id\": 1,\n" +
                               "      \"companyInfo\": \"2020년에 오픈한 강남 소재의 프리미엄 디저트 카페로, 유기농 재료만을 사용한 건강한 음료를 제공합니다.\",\n" +
                               "      \"creatorNickname\": \"브랜드매니저\"\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{campaignId}/company-info")
    public ResponseEntity<?> getCompanyInfo(@PathVariable Long campaignId) {
        try {
            CompanyInfoResponse response = viewService.getCompanyInfo(campaignId);
            return ResponseEntity.ok(BaseResponse.success(
                    CampaignSingleResponseWrapper.of(response), 
                    "업체 정보 조회 성공"
            ));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인을 찾을 수 없음: campaignId={}", campaignId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("업체 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 미션 가이드 조회", 
        description = "미션 가이드(마크다운 형식)를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "미션 가이드 조회 성공 응답 예시",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"미션 가이드 조회 성공\",\n" +
                               "  \"data\": {\n" +
                               "    \"campaign\": {\n" +
                               "      \"id\": 1,\n" +
                               "      \"missionGuide\": \"1. 카페 방문 시 직원에게 체험단임을 알려주세요.\\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.\\n3. 인스타그램에 사진과 함께 솔직한 후기를 작성해주세요.\"\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{campaignId}/mission-guide")
    public ResponseEntity<?> getMissionGuide(@PathVariable Long campaignId) {
        try {
            MissionGuideResponse response = viewService.getMissionGuide(campaignId);
            return ResponseEntity.ok(BaseResponse.success(
                    CampaignSingleResponseWrapper.of(response), 
                    "미션 가이드 조회 성공"
            ));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인을 찾을 수 없음: campaignId={}", campaignId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("미션 가이드 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 미션 키워드 조회", 
        description = "미션 컨텐츠에 포함되어야 하는 키워드를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "미션 키워드 조회 성공 응답 예시",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"미션 키워드 조회 성공\",\n" +
                               "  \"data\": {\n" +
                               "    \"campaign\": {\n" +
                               "      \"id\": 1,\n" +
                               "      \"missionKeywords\": [\"카페추천\", \"디저트맛집\", \"강남카페\"]\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{campaignId}/mission-keywords")
    public ResponseEntity<?> getMissionKeywords(@PathVariable Long campaignId) {
        try {
            MissionKeywordsResponse response = viewService.getMissionKeywords(campaignId);
            return ResponseEntity.ok(BaseResponse.success(
                    CampaignSingleResponseWrapper.of(response), 
                    "미션 키워드 조회 성공"
            ));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인을 찾을 수 없음: campaignId={}", campaignId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("미션 키워드 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 위치 정보 조회", 
        description = "방문 정보(위치 정보)를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "위치 정보 조회 성공 응답 예시",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"위치 정보 조회 성공\",\n" +
                               "  \"data\": {\n" +
                               "    \"campaign\": {\n" +
                               "      \"id\": 1,\n" +
                               "      \"visitLocations\": [\n" +
                               "        {\n" +
                               "          \"id\": 1,\n" +
                               "          \"address\": \"서울특별시 강남구 테헤란로 123\",\n" +
                               "          \"latitude\": 37.498095,\n" +
                               "          \"longitude\": 127.027610,\n" +
                               "          \"additionalInfo\": \"영업시간: 10:00-22:00, 주차 가능\"\n" +
                               "        }\n" +
                               "      ]\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{campaignId}/location-info")
    public ResponseEntity<?> getLocationInfo(@PathVariable Long campaignId) {
        try {
            LocationInfoResponse response = viewService.getLocationInfo(campaignId);
            return ResponseEntity.ok(BaseResponse.success(
                    CampaignSingleResponseWrapper.of(response), 
                    "위치 정보 조회 성공"
            ));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인을 찾을 수 없음: campaignId={}", campaignId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("위치 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
