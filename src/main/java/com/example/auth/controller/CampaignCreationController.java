package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.campaign.CreateCampaignRequest;
import com.example.auth.dto.campaign.CreateCampaignResponse;
import com.example.auth.exception.*;
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
@Tag(name = "캠페인 생성 API", description = "캠페인 생성 및 관리 API")
public class CampaignCreationController {

    private final CampaignCreationService campaignCreationService;
    private final TokenUtils tokenUtils;

    /**
     * 캠페인 등록 API 엔드포인트
     */
    @Operation(
        summary = "캠페인 등록", 
        description = "새로운 인플루언서 마케팅 캠페인을 등록합니다.\n\n" +
                      "###  권한 요구사항:\n" +
                      "- **CLIENT 권한**을 가진 사용자만 등록 가능합니다\n" +
                      "- JWT 토큰을 통한 인증이 필요합니다\n\n" +
                      "###  캠페인 일정 설정 가이드:\n" +
                      "캠페인의 각 단계별 날짜를 올바른 순서로 설정해주세요:\n\n" +
                      " **recruitmentStartDate** (모집 시작일)\n" +
                      "   └ 캠페인이 공개되어 인플루언서들이 신청을 시작할 수 있는 날짜\n\n" +
                      "**applicationDeadlineDate** (신청 마감일)\n" +
                      "   └ 인플루언서들이 캠페인에 신청할 수 있는 최종 날짜\n" +
                      "   └ 모집 시작일 이후여야 함\n\n" +
                      " **recruitmentEndDate** (모집 종료일)\n" +
                      "   └ 캠페인 모집 공고가 마감되는 날짜\n" +
                      "   └  신청 마감일과 같거나 이후여야 함\n\n" +
                      " **selectionDate** (참여자 선정일)\n" +
                      "   └ 신청자 중에서 최종 참여자를 선정하여 발표하는 날짜\n" +
                      "   └  모집 종료일 이후여야 함\n\n" +
                      "**reviewDeadlineDate** (리뷰 제출 마감일)\n" +
                      "   └ 선정된 인플루언서들이 체험 후 리뷰를 완료해야 하는 최종 날짜\n" +
                      "   └  선정일 이후여야 함 (충분한 체험 기간 고려)\n\n" +
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
                               "  \"thumbnailUrl\": \"https://example.com/images/cafe.jpg\",\n" +
                               "  \"campaignType\": \"인스타그램\",\n" +
                               "  \"title\": \"인스타 감성 카페 체험단 모집\",\n" +
                               "  \"productShortInfo\": \"시그니처 음료 2잔 무료 제공\",\n" +
                               "  \"maxApplicants\": 10,\n" +
                               "  \"productDetails\": \"인스타 감성 가득한 카페에서 시그니처 음료 2잔과 디저트 1개를 무료로 체험하실 분들을 모집합니다.\",\n" +
                               "  \"recruitmentStartDate\": \"2025-06-01\",\n" +
                               "  \"recruitmentEndDate\": \"2025-06-15\",\n" +
                               "  \"applicationDeadlineDate\": \"2025-06-14\",\n" +
                               "  \"selectionDate\": \"2025-06-16\",\n" +
                               "  \"reviewDeadlineDate\": \"2025-06-30\",\n" +
                               "  \"selectionCriteria\": \"인스타그램 팔로워 1000명 이상, 카페 리뷰 경험이 있는 분\",\n" +
                               "  \"missionGuide\": \"1. 카페 방문 시 직원에게 체험단임을 알려주세요.\\n2. 음료와 디저트를 맛있게 즐기며 사진을 찍어주세요.\\n3. 인스타그램에 사진과 함께 솔직한 후기를 작성해주세요.\",\n" +
                               "  \"missionKeywords\": [\"카페추천\", \"디저트맛집\", \"강남카페\"],\n" +
                               "  \"category\": {\n" +
                               "    \"type\": \"방문\",\n" +
                               "    \"name\": \"카페\"\n" +
                               "  },\n" +
                               "  \"companyInfo\": {\n" +
                               "    \"companyName\": \"맛있는 카페\",\n" +
                               "    \"businessRegistrationNumber\": \"123-45-67890\",\n" +
                               "    \"contactPerson\": \"김담당\",\n" +
                               "    \"phoneNumber\": \"010-1234-5678\"\n" +
                               "  }\n" +
                               "}"
                    ),
                    @ExampleObject(
                        name = "배송형 캠페인 예시 (화장품)",
                        value = "{\n" +
                               "  \"thumbnailUrl\": \"https://example.com/images/cosmetic.jpg\",\n" +
                               "  \"campaignType\": \"인스타그램\",\n" +
                               "  \"title\": \"신제품 스킨케어 세트 체험단\",\n" +
                               "  \"productShortInfo\": \"스킨케어 3종 세트 무료 제공\",\n" +
                               "  \"maxApplicants\": 20,\n" +
                               "  \"productDetails\": \"새로 출시된 프리미엄 스킨케어 3종 세트를 체험하고 솔직한 후기를 남겨주실 분들을 모집합니다.\",\n" +
                               "  \"recruitmentStartDate\": \"2025-06-01\",\n" +
                               "  \"recruitmentEndDate\": \"2025-06-15\",\n" +
                               "  \"applicationDeadlineDate\": \"2025-06-14\",\n" +
                               "  \"selectionDate\": \"2025-06-16\",\n" +
                               "  \"reviewDeadlineDate\": \"2025-07-15\",\n" +
                               "  \"selectionCriteria\": \"뷰티/스킨케어 관련 콘텐츠를 주로 포스팅하는 분\",\n" +
                               "  \"missionGuide\": \"1. 제품을 14일 이상 꾸준히 사용해주세요.\\n2. 사용 전후 피부 상태를 사진으로 기록해주세요.\\n3. 솔직한 사용 후기를 인스타그램에 게시해주세요.\",\n" +
                               "  \"missionKeywords\": [\"스킨케어\", \"뷰티리뷰\", \"신제품체험\"],\n" +
                               "  \"category\": {\n" +
                               "    \"type\": \"배송\",\n" +
                               "    \"name\": \"화장품\"\n" +
                               "  },\n" +
                               "  \"companyInfo\": {\n" +
                               "    \"companyName\": \"뷰티코스메틱\",\n" +
                               "    \"businessRegistrationNumber\": \"234-56-78901\",\n" +
                               "    \"contactPerson\": \"박매니저\",\n" +
                               "    \"phoneNumber\": \"010-2345-6789\"\n" +
                               "  }\n" +
                               "}"
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "캠페인 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 카테고리 타입/이름, 날짜 오류 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (CLIENT 권한 필요)"),
            @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음 (잘못된 타입 또는 이름)"),
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
                    .body(BaseResponse.success(response, "캠페인이 성공적으로 등록되었습니다.", HttpStatus.CREATED.value()));



        }// 추가해야 할 catch 블록들:

        catch (JwtValidationException e) {
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
                    .body(BaseResponse.fail("캠페인 등록 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * 캠페인 수정 API 엔드포인트
     */
    @Operation(
        summary = "캠페인 수정", 
        description = "기존 캠페인 정보를 수정합니다.\n\n" +
                      "### 수정 조건:\n" +
                      "- 본인이 생성한 캠페인만 수정 가능\n" +
                      "- 승인된 캠페인은 수정할 수 없음\n" +
                      "- 수정 후 승인 상태가 PENDING으로 변경됨\n\n" +
                      "### 카테고리 지정:\n" +
                      "- ID가 아닌 **타입과 이름**으로 지정\n" +
                      "- 방문형: 맛집, 카페, 뷰티, 숙박\n" +
                      "- 배송형: 식품, 화장품, 생활용품, 패션, 잡화"
    )
    @PutMapping("/{campaignId}")
    public ResponseEntity<?> updateCampaign(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId,
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        try {
            // 토큰에서 사용자 ID 추출
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            // 캠페인 수정 서비스 호출
            CreateCampaignResponse response = campaignCreationService.updateCampaign(userId, campaignId, request);
            
            log.info("캠페인 수정 성공: userId={}, campaignId={}", userId, campaignId);
            
            return ResponseEntity.ok(
                    BaseResponse.success(response, "캠페인이 성공적으로 수정되었습니다."));
                    
        }
        // 추가해야 할 catch 블록들:

        catch (JwtValidationException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            String errorCode = e.getErrorType() == TokenErrorType.EXPIRED ? "TOKEN_EXPIRED" : "TOKEN_INVALID";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (UnauthorizedException e) {
            log.warn("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        }catch (AccessDeniedException e) {
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
                    .body(BaseResponse.fail("캠페인 수정 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
