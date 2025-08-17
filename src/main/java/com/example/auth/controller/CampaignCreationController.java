package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.campaign.CreateCampaignRequest;
import com.example.auth.dto.campaign.CreateCampaignResponse;
import com.example.auth.dto.campaign.UpdateCampaignRequest;
import com.example.auth.exception.*;
import com.example.auth.service.CampaignCreationService;
import com.example.auth.service.CampaignUpdateService;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 캠페인 생성 및 수정을 위한 REST API 컨트롤러
 *
 * CLIENT 권한을 가진 사용자가 새로운 캠페인을 등록하고 수정할 수 있는 API 엔드포인트를 제공합니다.
 * 캠페인 생성/수정 요청을 검증하고 서비스 계층으로 전달하며, 결과를 클라이언트에게 응답합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "캠페인 생성 API", description = "캠페인 생성 및 관리 API")
public class CampaignCreationController {

    private final CampaignCreationService campaignCreationService;
    private final CampaignUpdateService campaignUpdateService;
    private final TokenUtils tokenUtils;



    @Operation(
        operationId = "createCampaignV1",
        summary = "캠페인 등록",
        description = "새로운 인플루언서 마케팅 캠페인을 등록합니다.\n\n" +
                      "###  권한 요구사항:\n" +
                      "- **CLIENT 권한**을 가진 사용자만 등록 가능합니다\n" +
                      "- JWT 토큰을 통한 인증이 필요합니다\n\n" +
                      "###  캠페인 일정 설정 가이드:\n" +
                      "캠페인의 각 단계별 날짜를 올바른 순서로 설정해주세요:\n\n" +
                      " **recruitmentStartDate** (모집 시작일)\n" +
                      "   └ 캠페인이 공개되어 인플루언서들이 신청을 시작할 수 있는 날짜\n\n" +
                      " **recruitmentEndDate** (모집 종료일)\n" +
                      "   └ 캠페인 모집 공고가 마감되는 날짜\n" +
                      "   └  신청 마감일과 같거나 이후여야 함\n\n" +
                      " **selectionDate** (참여자 선정일)\n" +
                      "   └ 신청자 중에서 최종 참여자를 선정하여 발표하는 날짜\n" +
                      "   └  모집 종료일 이후여야 함\n\n" +
                      "- **방문형**: 맛집, 카페, 뷰티, 숙박\n" +
                      "- **배송형**: 식품, 화장품, 생활용품, 패션, 잡화\n\n" ,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateCampaignRequest.class),
                examples = {
                        @ExampleObject(
                                name = "방문형 캠페인 예시 (카페)",
                                value = "{\n" +
                                        "  \"isAlwaysOpen\": false,\n" +
                                        "  \"thumbnailUrl\": \"https://example.com/images/cafe.jpg\",\n" +
                                        "  \"campaignType\": \"인스타그램\",\n" +
                                        "  \"title\": \"인스타 감성 카페 체험단 모집\",\n" +
                                        "  \"productShortInfo\": \"시그니처 음료 2잔 무료 제공\",\n" +
                                        "  \"maxApplicants\": 10,\n" +
                                        "  \"productDetails\": \"인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.\",\n" +
                                        "  \"recruitmentStartDate\": \"2025-06-01\",\n" +
                                        "  \"recruitmentEndDate\": \"2025-06-15\",\n" +
                                        "  \"selectionDate\": \"2025-06-16\",\n" +
                                        "  \"selectionCriteria\": \"인스타그램 팔로워 1000명 이상, 카페 리뷰 경험이 있는 분\",\n" +
                                        "  \"missionInfo\": {\n" +
                                        "    \"titleKeywords\": [\"카페추천\", \"인스타감성\"],\n" +
                                        "    \"bodyKeywords\": [\"맛있다\", \"분위기좋다\", \"추천\"],\n" +
                                        "    \"numberOfVideo\": 1,\n" +
                                        "    \"numberOfImage\": 5,\n" +
                                        "    \"numberOfText\": 300,\n" +
                                        "    \"isMap\": true,\n" +
                                        "    \"missionGuide\": \"1. 카페 방문 시 직원에게 체험단임을 알려주세요.\\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.\\n3. 인스타그램에 위치 태그와 함께 솔직한 후기를 작성해주세요.\",\n" +
                                        "    \"missionStartDate\": \"2025-06-17\",\n" +
                                        "    \"missionDeadlineDate\": \"2025-06-30\"\n" +
                                        "  },\n" +
                                        "  \"category\": {\n" +
                                        "    \"type\": \"방문\",\n" +
                                        "    \"name\": \"카페\"\n" +
                                        "  },\n" +
                                        "  \"companyInfo\": {\n" +
                                        "    \"contactPerson\": \"김담당\",\n" +
                                        "    \"phoneNumber\": \"010-1234-5678\"\n" +
                                        "  },\n" +
                                        "  \"visitInfo\": {\n" +
                                        "    \"homepage\": \"https://delicious-cafe.com\",\n" +
                                        "    \"contactPhone\": \"02-123-4567\",\n" +
                                        "    \"visitAndReservationInfo\": \"평일 10시-22시 방문 가능, 사전 예약 필수\",\n" +
                                        "    \"businessAddress\": \"서울특별시 강남구 테헤란로 123\",\n" +
                                        "    \"businessDetailAddress\": \"123빌딩 5층\",\n" +
                                        "    \"lat\": 37.5665,\n" +
                                        "    \"lng\": 126.9780\n" +
                                        "  }\n" +
                                        "}"
                        ),
                    @ExampleObject(
                        name = "배송형 캠페인 예시 (화장품)",
                        value = "{\n" +
                               "  \"isAlwaysOpen\": false,\n" +
                               "  \"thumbnailUrl\": \"https://example.com/images/cosmetic.jpg\",\n" +
                               "  \"campaignType\": \"인스타그램\",\n" +
                               "  \"title\": \"신제품 스킨케어 세트 체험단\",\n" +
                               "  \"productShortInfo\": \"스킨케어 3종 세트 무료 제공\",\n" +
                               "  \"maxApplicants\": 20,\n" +
                               "  \"productDetails\": \"새로 출시된 프리미엄 스킨케어 3종 세트를 체험하고 솔직한 후기를 남겨주실 분들을 모집합니다.\",\n" +
                               "  \"recruitmentStartDate\": \"2025-06-01\",\n" +
                               "  \"recruitmentEndDate\": \"2025-06-15\",\n" +
                               "  \"selectionDate\": \"2025-06-16\",\n" +
                               "  \"selectionCriteria\": \"뷰티/스킨케어 관련 콘텐츠를 주로 포스팅하는 분\",\n" +
                               "  \"missionInfo\": {\n" +
                               "    \"titleKeywords\": [\"스킨케어\", \"뷰티리뷰\"],\n" +
                               "    \"bodyKeywords\": [\"촉촉하다\", \"효과좋다\", \"추천\"],\n" +
                               "    \"numberOfVideo\": 0,\n" +
                               "    \"numberOfImage\": 4,\n" +
                               "    \"numberOfText\": 800,\n" +
                               "    \"isMap\": false,\n" +
                               "    \"missionGuide\": \"1. 제품을 14일 이상 꾸준히 사용해주세요.\\n2. 사용 전후 피부 상태를 사진으로 기록해주세요.\\n3. 솔직한 사용 후기를 인스타그램에 게시해주세요.\",\n" +
                               "    \"missionStartDate\": \"2025-06-17\",\n" +
                               "    \"missionDeadlineDate\": \"2025-07-15\"\n" +
                               "  },\n" +
                               "  \"category\": {\n" +
                               "    \"type\": \"배송\",\n" +
                               "    \"name\": \"화장품\"\n" +
                               "  },\n" +
                               "  \"companyInfo\": {\n" +
                               "    \"contactPerson\": \"박매니저\",\n" +
                               "    \"phoneNumber\": \"010-2345-6789\"\n" +
                               "  }\n" +
                               "}"
                    ),
                    @ExampleObject(
                        name = "상시 캠페인 예시 (카페)",
                        value = "{\n" +
                               "  \"isAlwaysOpen\": true,\n" +
                               "  \"thumbnailUrl\": \"https://example.com/images/always-open-cafe.jpg\",\n" +
                               "  \"campaignType\": \"인스타그램\",\n" +
                               "  \"title\": \"[상시모집] 감성 카페 체험단\",\n" +
                               "  \"productShortInfo\": \"시그니처 음료 + 디저트 무료\",\n" +
                               "  \"maxApplicants\": 50,\n" +
                               "  \"productDetails\": \"언제든지 방문 가능한 상시 체험 캠페인입니다. 감성 카페에서 음료와 디저트를 체험하고 후기를 남겨주세요.\",\n" +
                               "  \"recruitmentStartDate\": \"2025-06-01\",\n" +
                               "  \"selectionCriteria\": \"인스타그램 팔로워 500명 이상\",\n" +
                               "  \"missionInfo\": {\n" +
                               "    \"bodyKeywords\": [\"맛있다\", \"분위기좋다\", \"추천\"],\n" +
                               "    \"numberOfVideo\": 0,\n" +
                               "    \"numberOfImage\": 3,\n" +
                               "    \"numberOfText\": 200,\n" +
                               "    \"isMap\": true,\n" +
                               "    \"missionGuide\": \"1. 카페 방문 후 음료 주문\\n2. 분위기와 음료 사진 촬영\\n3. 인스타그램에 후기 게시\"\n" +
                               "  },\n" +
                               "  \"category\": {\n" +
                               "    \"type\": \"방문\",\n" +
                               "    \"name\": \"카페\"\n" +
                               "  },\n" +
                               "  \"companyInfo\": {\n" +
                               "    \"contactPerson\": \"김카페\",\n" +
                               "    \"phoneNumber\": \"010-3456-7890\"\n" +
                               "  },\n" +
                               "  \"visitInfo\": {\n" +
                               "    \"homepage\": \"https://always-open-cafe.com\",\n" +
                               "    \"contactPhone\": \"02-345-6789\",\n" +
                               "    \"visitAndReservationInfo\": \"상시 방문 가능, 평일 9시-22시, 주말 10시-23시\",\n" +
                               "    \"businessAddress\": \"서울특별시 강남구 카페로 789\",\n" +
                               "    \"businessDetailAddress\": \"카페빌딩 1층\",\n" +
                               "    \"lat\": 37.5665,\n" +
                               "    \"lng\": 126.9780\n" +
                               "  }\n" +
                               "}"
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "캠페인 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignV1SuccessResponse"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 카테고리 오류, 날짜 검증 오류 등",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignCategoryErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 - CLIENT 권한 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignCreationPermissionErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping
    public ResponseEntity<?> createCampaign(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        try {
            // 토큰에서 사용자 ID 추출
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            // 캠페인 생성 서비스 호출
            CreateCampaignResponse response = campaignCreationService.createCampaign(userId, request);

            log.info("캠페인 생성 성공: userId={}, campaignId={}", userId, response.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success(response, "캠페인이 성공적으로 등록되었어요.", HttpStatus.CREATED.value()));

        } catch (JwtValidationException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            String errorCode = e.getErrorType() == TokenErrorType.EXPIRED ? "TOKEN_EXPIRED" : "TOKEN_INVALID";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (UnauthorizedException e) {
            log.warn("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        } catch (AccessDeniedException e) {
            log.warn("캠페인 등록 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "FORBIDDEN", HttpStatus.FORBIDDEN.value()));
        } catch (ResourceNotFoundException e) {
            log.warn("리소스를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "BAD_REQUEST", HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            log.error("캠페인 등록 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 등록 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        operationId = "updateCampaignV1",
        summary = "캠페인 전체 수정",
        description = "기존 캠페인의 정보를 수정합니다.\n\n" +
                      "### 권한 요구사항:\n" +
                      "- **본인이 생성한 캠페인**만 수정 가능합니다\n" +
                      "- JWT 토큰을 통한 인증이 필요합니다\n\n" +
                      "### 수정 제한 규칙:\n" +
                      "\n** 상시 수정 가능:**\n" +
                      "- 썸네일\n" +
                      "- 캠페인 제목\n" +
                      "- 제품/서비스 간략 정보\n" +
                      "- 방문 정보 (공식 홈페이지 주소, 연락처, 방문 및 예약 안내)\n" +
                      "\n** 신청자가 없을 때만 수정 가능:**\n" +
                      "- 캠페인 타입 (인스타그램, 블로그 등)\n" +
                      "- 카테고리 타입/카테고리명\n" +
                      "- 상세 정보 (제품/서비스 상세 정보)\n" +
                      "- 선정 기준\n" +
                      "- 참가자 선정일 (발표일)\n" +
                      "- 미션 정보 (미션 가이드, 미션 키워드, 미션 시작일, 미션 마감일)\n" +
                      "- 캠페인 모집 시작일\n" +
                      "- 캠페인 모집 종료일\n" +
                      "\n**신청자가 있을 때는 증가만 가능:**\n" +
                      "- 최대 지원 가능 인원 (현재 신청자 수보다 적게 설정 불가)\n" +
                      "\n** 항상 수정 불가:**\n" +
                      "- 사업체 정보 (담당자명, 연락처)\n" +
                      "- 위치 정보 (사업장 주소, 좌표)\n\n" +
                      "### 날짜 순서 규칙:\n" +
                      "모집 시작일 ≤ 모집 종료일 ≤ 선정일 ≤ 미션 시작일 ≤ 리뷰 마감일",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateCampaignRequest.class),
                examples = {
                    @ExampleObject(
                        name = "방문형 캠페인 수정 예시",
                        value = "{\n" +
                               "  \"isAlwaysOpen\": false,\n" +
                               "  \"thumbnailUrl\": \"https://example.com/images/new-cafe.jpg\",\n" +
                               "  \"campaignType\": \"인스타그램\",\n" +
                               "  \"title\": \"[수정] 인스타 감성 카페 체험단 모집\",\n" +
                               "  \"productShortInfo\": \"시그니처 음료 3잔 + 디저트 1개 무료 제공\",\n" +
                               "  \"maxApplicants\": 15,\n" +
                               "  \"productDetails\": \"수정된 내용: 인스타 감성 가득한 카페에서 시그니처 음료 3잔과 프리미엄 디저트를 무료로 체험하실 분들을 모집합니다.\",\n" +
                               "  \"recruitmentStartDate\": \"2025-06-01\",\n" +
                               "  \"recruitmentEndDate\": \"2025-06-20\",\n" +
                               "  \"selectionDate\": \"2025-06-21\",\n" +
                               "  \"selectionCriteria\": \"인스타그램 팔로워 2000명 이상, 카페 리뷰 경험 풍부한 분\",\n" +
                               "  \"missionInfo\": {\n" +
                               "    \"titleKeywords\": [\"카페추천\", \"인스타감성\", \"맛집\"],\n" +
                               "    \"bodyKeywords\": [\"맛있다\", \"분위기좋다\", \"추천\", \"재방문\"],\n" +
                               "    \"numberOfVideo\": 2,\n" +
                               "    \"numberOfImage\": 6,\n" +
                               "    \"numberOfText\": 400,\n" +
                               "    \"isMap\": true,\n" +
                               "    \"missionGuide\": \"1. 카페 방문 시 직원에게 체험단임을 알려주세요.\\n2. 음료와 디저트를 맛있게 즐기며 다양한 각도로 사진을 찍어주세요.\\n3. 인스타그램 스토리와 피드에 모두 업로드해주세요.\",\n" +
                               "    \"missionStartDate\": \"2025-06-22\",\n" +
                               "    \"missionDeadlineDate\": \"2025-07-05\"\n" +
                               "  },\n" +
                               "  \"category\": {\n" +
                               "    \"type\": \"방문\",\n" +
                               "    \"name\": \"카페\"\n" +
                               "  },\n" +
                               "  \"companyInfo\": {\n" +
                               "    \"contactPerson\": \"이매니저\",\n" +
                               "    \"phoneNumber\": \"010-9876-5432\"\n" +
                               "  },\n" +
                               "  \"visitInfo\": {\n" +
                               "    \"homepage\": \"https://new-delicious-cafe.com\",\n" +
                               "    \"contactPhone\": \"02-987-6543\",\n" +
                               "    \"visitAndReservationInfo\": \"평일 10시-23시, 주말 9시-24시 방문 가능, 온라인 예약 가능\",\n" +
                               "    \"businessAddress\": \"서울특별시 강남구 테헤란로 456\",\n" +
                               "    \"businessDetailAddress\": \"새빌딩 1층\",\n" +
                               "    \"lat\": 37.5665,\n" +
                               "    \"lng\": 126.9780\n" +
                               "  }\n" +
                               "}"
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "캠페인 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignV1SuccessResponse"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 - 수정 제한, 날짜 오류 등",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignUpdateRestrictionErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 - 본인 캠페인이 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignOwnershipErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PutMapping("/{campaignId}")
    public ResponseEntity<?> updateCampaign(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "수정할 캠페인 ID", required = true)
            @PathVariable Long campaignId,
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        try {
            // 토큰에서 사용자 ID 추출
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            // CreateCampaignRequest를 UpdateCampaignRequest로 변환
            UpdateCampaignRequest updateRequest = convertToUpdateRequest(request);

            // 캠페인 수정 서비스 호출
            CreateCampaignResponse response = campaignUpdateService.updateCampaign(userId, campaignId, updateRequest);

            log.info("캠페인 수정 성공: userId={}, campaignId={}", userId, campaignId);

            return ResponseEntity.ok(BaseResponse.success(response, "캠페인이 성공적으로 수정되었어요."));

        } catch (JwtValidationException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            String errorCode = e.getErrorType() == TokenErrorType.EXPIRED ? "TOKEN_EXPIRED" : "TOKEN_INVALID";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (UnauthorizedException e) {
            log.warn("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        } catch (AccessDeniedException e) {
            log.warn("캠페인 수정 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "FORBIDDEN", HttpStatus.FORBIDDEN.value()));
        } catch (ResourceNotFoundException e) {
            log.warn("리소스를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "BAD_REQUEST", HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            log.error("캠페인 수정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 수정 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * CreateCampaignRequest를 UpdateCampaignRequest로 변환
     */
    private UpdateCampaignRequest convertToUpdateRequest(CreateCampaignRequest createRequest) {
        UpdateCampaignRequest.UpdateCampaignRequestBuilder builder = UpdateCampaignRequest.builder()
                .isAlwaysOpen(createRequest.getIsAlwaysOpen())
                .thumbnailUrl(createRequest.getThumbnailUrl())
                .campaignType(createRequest.getCampaignType())
                .title(createRequest.getTitle())
                .productShortInfo(createRequest.getProductShortInfo())
                .maxApplicants(createRequest.getMaxApplicants())
                .productDetails(createRequest.getProductDetails())
                .recruitmentStartDate(createRequest.getRecruitmentStartDate())
                .recruitmentEndDate(createRequest.getRecruitmentEndDate())
                .selectionDate(createRequest.getSelectionDate())
                .selectionCriteria(createRequest.getSelectionCriteria());

        // 카테고리 정보 변환
        if (createRequest.getCategory() != null) {
            builder.category(UpdateCampaignRequest.CategoryInfo.builder()
                    .type(createRequest.getCategory().getType())
                    .name(createRequest.getCategory().getName())
                    .build());
        }

        // 미션 정보 변환
        if (createRequest.getMissionInfo() != null) {
            builder.missionInfo(UpdateCampaignRequest.MissionInfo.builder()
                    .titleKeywords(createRequest.getMissionInfo().getTitleKeywords())
                    .bodyKeywords(createRequest.getMissionInfo().getBodyKeywords())
                    .numberOfVideo(createRequest.getMissionInfo().getNumberOfVideo())
                    .numberOfImage(createRequest.getMissionInfo().getNumberOfImage())
                    .numberOfText(createRequest.getMissionInfo().getNumberOfText())
                    .isMap(createRequest.getMissionInfo().getIsMap())
                    .missionGuide(createRequest.getMissionInfo().getMissionGuide())
                    .missionStartDate(createRequest.getMissionInfo().getMissionStartDate())
                    .missionDeadlineDate(createRequest.getMissionInfo().getMissionDeadlineDate())
                    .build());
        }

        // 방문 정보 변환
        if (createRequest.getVisitInfo() != null) {
            builder.visitInfo(UpdateCampaignRequest.VisitInfo.builder()
                    .officialWebsite(createRequest.getVisitInfo().getHomepage())
                    .contactNumber(createRequest.getVisitInfo().getContactPhone())
                    .visitReservationInfo(createRequest.getVisitInfo().getVisitAndReservationInfo())
                    .build());
        }

        return builder.build();
    }
}

