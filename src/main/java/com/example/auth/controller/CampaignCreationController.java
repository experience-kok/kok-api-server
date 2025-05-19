package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.campaign.CreateCampaignRequest;
import com.example.auth.dto.campaign.CreateCampaignResponse;
import com.example.auth.exception.AccessDeniedException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.service.CampaignCreationService;
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
 * 캠페인 생성을 위한 REST API 컨트롤러
 * 
 * CLIENT 권한을 가진 사용자가 새로운 캠페인을 등록할 수 있는 API 엔드포인트를 제공합니다.
 * 캠페인 생성 요청을 검증하고 서비스 계층으로 전달하며, 결과를 클라이언트에게 응답합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "캠페인 생성 API", description = "캠페인 등록 API")
public class CampaignCreationController {

    private final CampaignCreationService campaignCreationService;
    private final TokenUtils tokenUtils;

    /**
     * 캠페인 등록 API 엔드포인트
     * 
     * CLIENT 권한을 가진 사용자만 새로운 캠페인을 등록할 수 있습니다.
     * 요청된 캠페인 정보는 유효성 검사를 거친 후 서비스 계층으로 전달되어 처리됩니다.
     * 생성된 캠페인은 기본적으로 'PENDING' 상태로 시작하여 관리자의 승인이 필요합니다.
     * 
     * @param bearerToken 사용자 인증 토큰 (JWT)
     * @param request 캠페인 생성 요청 데이터
     * @return 생성된 캠페인 정보와 함께 성공 메시지를 반환
     */
    @Operation(
        summary = "캠페인 등록", 
        description = "새로운 캠페인을 등록합니다. CLIENT 권한을 가진 사용자만 등록 가능합니다.\n\n" +
                      "카테고리 타입은 '방문' 또는 '배송' 중 하나여야 합니다.\n" +
                      "카테고리 이름은 다음 중 하나여야 합니다:\n" +
                      "- 방문 타입: '맛집', '카페', '뷰티', '숙박'\n" +
                      "- 배송 타입: '식품', '화장품', '생활용품', '패션', '잡화'",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateCampaignRequest.class),
                examples = @ExampleObject(
                    name = "카페 체험단 캠페인 예시",
                    value = "{\n" +
                           "  \"thumbnailUrl\": \"https://example.com/images/cafe.jpg\",\n" +
                           "  \"campaignType\": \"인스타그램\",\n" +
                           "  \"title\": \"인스타 감성 카페 체험단 모집\",\n" +
                           "  \"productShortInfo\": \"시그니처 음료 2잔 무료 제공\",\n" +
                           "  \"maxApplicants\": 10,\n" +
                           "  \"productDetails\": \"인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.\",\n" +
                           "  \"recruitmentStartDate\": \"2025-05-01\",\n" +
                           "  \"recruitmentEndDate\": \"2025-05-15\",\n" +
                           "  \"selectionDate\": \"2025-05-16\",\n" +
                           "  \"reviewDeadlineDate\": \"2025-05-30\",\n" +
                           "  \"companyInfo\": \"2020년에 오픈한 강남 소재의 프리미엄 디저트 카페로, 유기농 재료만을 사용한 건강한 음료를 제공합니다.\",\n" +
                           "  \"missionGuide\": \"1. 카페 방문 시 직원에게 체험단임을 알려주세요.\\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.\\n3. 인스타그램에 사진과 함께 솔직한 후기를 작성해주세요.\",\n" +
                           "  \"missionKeywords\": [\"카페추천\", \"디저트맛집\", \"강남카페\"],\n" +
                           "  \"applicationDeadlineDate\": \"2025-05-14\",\n" +
                           "  \"categoryType\": \"방문\",\n" +
                           "  \"categoryName\": \"카페\",\n" +
                           "  \"visitLocations\": [\n" +
                           "    {\n" +
                           "      \"address\": \"서울특별시 강남구 테헤란로 123\",\n" +
                           "      \"latitude\": 37.498095,\n" +
                           "      \"longitude\": 127.027610,\n" +
                           "      \"additionalInfo\": \"영업시간: 10:00-22:00, 주차 가능\"\n" +
                           "    }\n" +
                           "  ]\n" +
                           "}"
                )
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "201", 
                description = "캠페인 등록 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateCampaignResponse.class),
                    examples = @ExampleObject(
                        name = "성공 응답 예시",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"캠페인이 성공적으로 등록되었습니다.\",\n" +
                               "  \"data\": {\n" +
                               "    \"id\": 1,\n" +
                               "    \"thumbnailUrl\": \"https://example.com/images/cafe.jpg\",\n" +
                               "    \"campaignType\": \"인스타그램\",\n" +
                               "    \"title\": \"인스타 감성 카페 체험단 모집\",\n" +
                               "    \"productShortInfo\": \"시그니처 음료 2잔 무료 제공\",\n" +
                               "    \"maxApplicants\": 10,\n" +
                               "    \"recruitmentStartDate\": \"2025-05-01\",\n" +
                               "    \"recruitmentEndDate\": \"2025-05-15\",\n" +
                               "    \"selectionDate\": \"2025-05-16\",\n" +
                               "    \"reviewDeadlineDate\": \"2025-05-30\",\n" +
                               "    \"approvalStatus\": \"PENDING\",\n" +
                               "    \"category\": {\n" +
                               "      \"id\": 1,\n" +
                               "      \"categoryType\": \"방문\",\n" +
                               "      \"categoryName\": \"카페\"\n" +
                               "    },\n" +
                               "    \"user\": {\n" +
                               "      \"id\": 1,\n" +
                               "      \"nickname\": \"브랜드매니저\",\n" +
                               "      \"email\": \"brand@example.com\",\n" +
                               "      \"profileImg\": \"https://example.com/profile.jpg\",\n" +
                               "      \"role\": \"CLIENT\"\n" +
                               "    },\n" +
                               "    \"visitLocations\": [\n" +
                               "      {\n" +
                               "        \"id\": 1,\n" +
                               "        \"address\": \"서울특별시 강남구 테헤란로 123\",\n" +
                               "        \"latitude\": 37.498095,\n" +
                               "        \"longitude\": 127.027610,\n" +
                               "        \"additionalInfo\": \"영업시간: 10:00-22:00, 주차 가능\"\n" +
                               "      }\n" +
                               "    ]\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (CLIENT 권한 필요)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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
                    .body(BaseResponse.success(response, "캠페인이 성공적으로 등록되었습니다."));
        } catch (AccessDeniedException e) {
            log.warn("캠페인 등록 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "FORBIDDEN", HttpStatus.FORBIDDEN.value()));
        } catch (ResourceNotFoundException e) {
            log.warn("리소스를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("캠페인 등록 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 등록 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
