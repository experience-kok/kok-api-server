package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.application.*;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.exception.*;
import com.example.auth.service.CampaignApplicationService;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 캠페인 신청 관리 컨트롤러
 * 인플루언서가 캠페인에 신청하고 관리하기 위한 API 엔드포인트를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/campaign-applications")
@RequiredArgsConstructor
@Tag(name = "캠페인 신청 API", description = "인플루언서의 캠페인 신청 및 관리 API")
public class CampaignApplicationController {

    private final CampaignApplicationService applicationService;
    private final TokenUtils tokenUtils;

    @Operation(
        summary = "캠페인 신청", 
        description = "인플루언서가 선택한 캠페인에 참여 신청합니다. INFLUENCER 권한을 가진 사용자만 신청 가능합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 캠페인 참여 의사를 표시하고 신청 정보를 등록합니다.\n" +
                      "- 한 사용자는 동일한 캠페인에 중복 신청할 수 없습니다.\n" +
                      "- 신청 마감일이 지난 캠페인은 신청할 수 없습니다.\n" +
                      "- 승인된 캠페인만 신청할 수 있습니다.\n\n" +
                      "### 응답 필드 설명\n" +
                      "- **hasApplied**: 신청 완료 상태 (신청 성공 시 항상 true)\n" +
                      "- **status**: 신청 상태 (PENDING: 대기중, APPROVED: 선정됨, REJECTED: 거절됨, COMPLETED: 완료됨)\n\n" +
                      "### 신청 상태 설명\n" +
                      "- **PENDING**: 신청 접수 상태 (기본값)\n" +
                      "- **APPROVED**: 선정된 신청\n" +
                      "- **REJECTED**: 거절된 신청\n" +
                      "- **COMPLETED**: 체험 및 리뷰까지 완료한 신청",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CampaignApplicationRequest.class),
                examples = @ExampleObject(
                    name = "기본 신청 예시",
                    value = "{\n" +
                           "  \"campaignId\": 42\n" +
                           "}"
                )
            )
        )
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "201", 
                description = "신청 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "성공 응답 예시",
                        value = "{\n" +
                               "  \"success\": true,\n" +
                               "  \"message\": \"캠페인 신청이 완료되었어요\",\n" +
                               "  \"status\": 201,\n" +
                               "  \"data\": {\n" +
                               "    \"application\": {\n" +
                               "      \"id\": 15,\n" +
                               "      \"status\": \"PENDING\",\n" +
                               "      \"hasApplied\": true,\n" +
                               "      \"createdAt\": \"2023-05-17T14:30:15\",\n" +
                               "      \"updatedAt\": \"2023-05-17T14:30:15\",\n" +
                               "      \"campaign\": {\n" +
                               "        \"id\": 42,\n" +
                               "        \"title\": \"신상 음료 체험단 모집\"\n" +
                               "      },\n" +
                               "      \"user\": {\n" +
                               "        \"id\": 5,\n" +
                               "        \"nickname\": \"인플루언서닉네임\"\n" +
                               "      }\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "유효하지 않은 요청 또는 이미 신청함",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"이미 해당 캠페인에 신청하셨습니다.\",\n" +
                               "  \"errorCode\": \"INVALID_REQUEST\",\n" +
                               "  \"statusCode\": 400\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 실패",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"인증이 필요합니다.\",\n" +
                               "  \"errorCode\": \"UNAUTHORIZED\",\n" +
                               "  \"statusCode\": 401\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "403", 
                description = "권한 없음 (INFLUENCER 권한 필요)",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"캠페인 신청 권한이 없습니다. INFLUENCER 권한이 필요합니다.\",\n" +
                               "  \"errorCode\": \"FORBIDDEN\",\n" +
                               "  \"statusCode\": 403\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "캠페인 또는 사용자를 찾을 수 없음",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"캠페인을 찾을 수 없습니다: 42\",\n" +
                               "  \"errorCode\": \"NOT_FOUND\",\n" +
                               "  \"statusCode\": 404\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버 오류",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"캠페인 신청 중 오류가 발생했습니다.\",\n" +
                               "  \"errorCode\": \"INTERNAL_ERROR\",\n" +
                               "  \"statusCode\": 500\n" +
                               "}"
                    )
                )
            )
    })
    @PostMapping
    public ResponseEntity<?> applyForCampaign(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Valid @RequestBody CampaignApplicationRequest request
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("캠페인 신청 요청: userId={}, campaignId={}", userId, request.getCampaignId());
            
            ApplicationResponse applicationResponse = 
                    applicationService.createApplication(request.getCampaignId(), userId);
            
            // 신청 정보를 구조화된 DTO로 변환
            ApplicationListResponseWrapper.ApplicationInfoDTO infoDTO = 
                ApplicationListResponseWrapper.ApplicationInfoDTO.fromApplicationResponse(applicationResponse);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success(
                        ApplicationSingleResponseWrapper.of(infoDTO), 
                        "캠페인 신청이 완료되었어요"
                    ));
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
        } catch (IllegalStateException e) {
            log.warn("캠페인 신청 실패 (비즈니스 로직): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "INVALID_REQUEST", HttpStatus.BAD_REQUEST.value()));
        } catch (AccessDeniedException e) {
            log.warn("캠페인 신청 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "FORBIDDEN", HttpStatus.FORBIDDEN.value()));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인 신청 실패 (리소스 없음): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("캠페인 신청 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 신청 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "신청 취소", 
        description = "인플루언서가 캠페인 신청을 취소합니다.\n\n" +
                     "### 주요 기능\n" +
                     "- 본인이 신청한 캠페인만 취소할 수 있습니다.\n" +
                     "- 이미 선정된 신청(APPROVED)은 취소할 수 없습니다.\n" +
                     "- 취소된 신청은 데이터베이스에서 완전히 삭제됩니다.\n\n" +
                     "### 취소 대상\n" +
                     "- applicationId: 취소할 신청의 고유 ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "취소 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"캠페인 신청이 취소되었어요\",\n" +
                               "  \"data\": null\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "이미 선정된 신청은 취소 불가",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"이미 선정된 신청은 취소할 수 없습니다.\",\n" +
                               "  \"errorCode\": \"INVALID_REQUEST\",\n" +
                               "  \"statusCode\": 400\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 실패",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"인증이 필요합니다.\",\n" +
                               "  \"errorCode\": \"UNAUTHORIZED\",\n" +
                               "  \"statusCode\": 401\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "403", 
                description = "권한 없음 (본인의 신청이 아님)",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"해당 신청을 취소할 권한이 없습니다.\",\n" +
                               "  \"errorCode\": \"FORBIDDEN\",\n" +
                               "  \"statusCode\": 403\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "신청 정보를 찾을 수 없음",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"신청 정보를 찾을 수 없습니다: 15\",\n" +
                               "  \"errorCode\": \"NOT_FOUND\",\n" +
                               "  \"statusCode\": 404\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버 오류",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"캠페인 신청 취소 중 오류가 발생했습니다.\",\n" +
                               "  \"errorCode\": \"INTERNAL_ERROR\",\n" +
                               "  \"statusCode\": 500\n" +
                               "}"
                    )
                )
            )
    })
    @DeleteMapping("/{applicationId}")
    public ResponseEntity<?> cancelApplication(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "신청 ID", required = true, example = "15")
            @PathVariable Long applicationId
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("캠페인 신청 취소 요청: userId={}, applicationId={}", userId, applicationId);
            
            applicationService.cancelApplication(applicationId, userId);
            
            return ResponseEntity.ok(BaseResponse.success(null, "캠페인 신청이 취소되었어요"));
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
        }catch (ResourceNotFoundException e) {
            log.warn("캠페인 신청 취소 실패 (리소스 없음): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (AccessDeniedException e) {
            log.warn("캠페인 신청 취소 실패 (권한 없음): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "FORBIDDEN", HttpStatus.FORBIDDEN.value()));
        } catch (IllegalStateException e) {
            log.warn("캠페인 신청 취소 실패 (비즈니스 로직): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "INVALID_REQUEST", HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            log.error("캠페인 신청 취소 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 신청 취소 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "내 신청 목록 조회", 
        description = "로그인한 사용자 역할에 따라 관련된 캠페인 신청 목록을 페이징하여 조회합니다.\n\n" +
                     "### 사용자 역할별 조회 내용:\n" +
                     "- **USER (인플루언서)**: 본인이 신청한 캠페인 목록\n" +
                     "- **CLIENT (기업)**: 내가 만든 캠페인 목록\n\n" +
                     "### 신청 상태 (ApplicationStatus):\n" +
                     "- **PENDING**: 신청 접수 (기본값)\n" +
                     "- **APPROVED**: 선정됨\n" +
                     "- **REJECTED**: 거절됨\n" +
                     "- **COMPLETED**: 체험 및 리뷰 완료\n\n" +
                     "### 캠페인 승인 상태 (ApprovalStatus) - CLIENT 응답시:\n" +
                     "- **PENDING**: 관리자 승인 대기중\n" +
                     "- **APPROVED**: 관리자 승인됨 (활성화)\n" +
                     "- **REJECTED**: 관리자 거절됨\n" +
                     "- **EXPIRED**: 승인됐지만 모집기간이 종료됨 (만료됨)\n\n" +
                     "### 응답 형식 선택 (includePaging 파라미터):\n" +
                     "- **true (기본값)**: 페이징 정보를 포함한 전체 응답 구조\n" +
                     "- **false**: 요청한 갯수만큼의 신청 목록 데이터만 반환\n\n" +
                     "### 응답 데이터 구조:\n" +
                     "- **applications**: 목록 (USER: 신청 목록, CLIENT: 캠페인 목록)\n" +
                     "  - **id**: ID (USER: 신청 ID, CLIENT: 캠페인 ID)\n" +
                     "  - **status**: 상태 (USER: 신청 상태, CLIENT: 캠페인 승인 상태)\n" +
                     "  - **hasApplied**: 신청 여부 (항상 true)\n" +
                     "  - **createdAt**: 생성 시간\n" +
                     "  - **updatedAt**: 최종 수정 시간\n" +
                     "  - **campaign**: 캠페인 정보\n" +
                     "    - **id**: 캠페인 ID\n" +
                     "    - **title**: 캠페인 제목\n" +
                     "  - **user**: 사용자 정보\n" +
                     "    - **id**: 사용자 ID (USER: 신청자, CLIENT: 캠페인 생성자)\n" +
                     "    - **nickname**: 사용자 닉네임\n" +
                     "- **pagination**: 페이징 정보 객체\n" +
                     "  - **pageNumber**: 현재 페이지 번호 (1부터 시작)\n" +
                     "  - **pageSize**: 페이지 크기 (한 페이지에 표시되는 항목 수)\n" +
                     "  - **totalPages**: 전체 페이지 수\n" +
                     "  - **totalElements**: 전체 항목 수\n" +
                     "  - **first**: 현재 페이지가 첫 번째 페이지인지 여부\n" +
                     "  - **last**: 현재 페이지가 마지막 페이지인지 여부"
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseResponse.class),
                    examples = {
                        @ExampleObject(
                            name = "USER 역할 - 페이징 정보 포함 응답",
                            value = "{\n" +
                                   "  \"success\": true,\n" +
                                   "  \"message\": \"신청 목록을 조회했어요\",\n" +
                                   "  \"status\": 200,\n" +
                                   "  \"data\": {\n" +
                                   "    \"applications\": [\n" +
                                   "      {\n" +
                                   "        \"id\": 15,\n" +
                                   "        \"status\": \"PENDING\",\n" +
                                   "        \"hasApplied\": true,\n" +
                                   "        \"createdAt\": \"2023-05-17T14:30:15\",\n" +
                                   "        \"updatedAt\": \"2023-05-17T14:30:15\",\n" +
                                   "        \"campaign\": {\n" +
                                   "          \"id\": 42,\n" +
                                   "          \"title\": \"신상 음료 체험단 모집\"\n" +
                                   "        },\n" +
                                   "        \"user\": {\n" +
                                   "          \"id\": 5,\n" +
                                   "          \"nickname\": \"인플루언서닉네임\"\n" +
                                   "        }\n" +
                                   "      },\n" +
                                   "      {\n" +
                                   "        \"id\": 16,\n" +
                                   "        \"status\": \"APPROVED\",\n" +
                                   "        \"hasApplied\": true,\n" +
                                   "        \"createdAt\": \"2023-05-16T10:15:32\",\n" +
                                   "        \"updatedAt\": \"2023-05-18T09:45:22\",\n" +
                                   "        \"campaign\": {\n" +
                                   "          \"id\": 43,\n" +
                                   "          \"title\": \"뷰티 제품 체험단\"\n" +
                                   "        },\n" +
                                   "        \"user\": {\n" +
                                   "          \"id\": 5,\n" +
                                   "          \"nickname\": \"인플루언서닉네임\"\n" +
                                   "        }\n" +
                                   "      }\n" +
                                   "    ],\n" +
                                   "    \"pagination\": {\n" +
                                   "      \"pageNumber\": 1,\n" +
                                   "      \"pageSize\": 10,\n" +
                                   "      \"totalPages\": 1,\n" +
                                   "      \"totalElements\": 2,\n" +
                                   "      \"first\": true,\n" +
                                   "      \"last\": true\n" +
                                   "    }\n" +
                                   "  }\n" +
                                   "}"
                        ),
                        @ExampleObject(
                            name = "CLIENT 역할 - 내가 만든 캠페인 목록",
                            value = "{\n" +
                                   "  \"success\": true,\n" +
                                   "  \"message\": \"신청 목록을 조회했어요\",\n" +
                                   "  \"status\": 200,\n" +
                                   "  \"data\": {\n" +
                                   "    \"applications\": [\n" +
                                   "      {\n" +
                                   "        \"id\": 50,\n" +
                                   "        \"status\": \"PENDING\",\n" +
                                   "        \"hasApplied\": true,\n" +
                                   "        \"createdAt\": \"2023-05-18T09:15:30\",\n" +
                                   "        \"updatedAt\": \"2023-05-18T09:15:30\",\n" +
                                   "        \"campaign\": {\n" +
                                   "          \"id\": 50,\n" +
                                   "          \"title\": \"신제품 런칭 캠페인\"\n" +
                                   "        },\n" +
                                   "        \"user\": {\n" +
                                   "          \"id\": 12,\n" +
                                   "          \"nickname\": \"마케팅담당자\"\n" +
                                   "        }\n" +
                                   "      },\n" +
                                   "      {\n" +
                                   "        \"id\": 51,\n" +
                                   "        \"status\": \"APPROVED\",\n" +
                                   "        \"hasApplied\": true,\n" +
                                   "        \"createdAt\": \"2023-05-15T16:20:45\",\n" +
                                   "        \"updatedAt\": \"2023-05-16T14:30:20\",\n" +
                                   "        \"campaign\": {\n" +
                                   "          \"id\": 51,\n" +
                                   "          \"title\": \"여름 시즌 프로모션\"\n" +
                                   "        },\n" +
                                   "        \"user\": {\n" +
                                   "          \"id\": 12,\n" +
                                   "          \"nickname\": \"마케팅담당자\"\n" +
                                   "        }\n" +
                                   "      },\n" +
                                   "      {\n" +
                                   "        \"id\": 52,\n" +
                                   "        \"status\": \"REJECTED\",\n" +
                                   "        \"hasApplied\": true,\n" +
                                   "        \"createdAt\": \"2023-05-10T11:30:00\",\n" +
                                   "        \"updatedAt\": \"2023-05-12T10:15:30\",\n" +
                                   "        \"campaign\": {\n" +
                                   "          \"id\": 52,\n" +
                                   "          \"title\": \"봄 컬렉션 리뷰\"\n" +
                                   "        },\n" +
                                   "        \"user\": {\n" +
                                   "          \"id\": 12,\n" +
                                   "          \"nickname\": \"마케팅담당자\"\n" +
                                   "        }\n" +
                                   "      },\n" +
                                   "      {\n" +
                                   "        \"id\": 53,\n" +
                                   "        \"status\": \"EXPIRED\",\n" +
                                   "        \"hasApplied\": true,\n" +
                                   "        \"createdAt\": \"2023-04-20T08:45:15\",\n" +
                                   "        \"updatedAt\": \"2023-04-20T08:45:15\",\n" +
                                   "        \"campaign\": {\n" +
                                   "          \"id\": 53,\n" +
                                   "          \"title\": \"이전 시즌 마케팅\"\n" +
                                   "        },\n" +
                                   "        \"user\": {\n" +
                                   "          \"id\": 12,\n" +
                                   "          \"nickname\": \"마케팅담당자\"\n" +
                                   "        }\n" +
                                   "      }\n" +
                                   "    ],\n" +
                                   "    \"pagination\": {\n" +
                                   "      \"pageNumber\": 1,\n" +
                                   "      \"pageSize\": 10,\n" +
                                   "      \"totalPages\": 1,\n" +
                                   "      \"totalElements\": 4,\n" +
                                   "      \"first\": true,\n" +
                                   "      \"last\": true\n" +
                                   "    }\n" +
                                   "  }\n" +
                                   "}"
                        )
                    }
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 실패",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"인증이 필요합니다.\",\n" +
                               "  \"errorCode\": \"UNAUTHORIZED\",\n" +
                               "  \"statusCode\": 401\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버 오류",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"신청 목록 조회 중 오류가 발생했습니다.\",\n" +
                               "  \"errorCode\": \"INTERNAL_ERROR\",\n" +
                               "  \"statusCode\": 500\n" +
                               "}"
                    )
                )
            )
    })
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1") 
            @RequestParam(required = false, defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10") 
            @RequestParam(required = false, defaultValue = "10") int size,
            @Parameter(description = "페이징 정보 포함 여부 (true: 페이징 정보 포함, false: 콘텐츠만 반환)", example = "true") 
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            String userRole = tokenUtils.getRoleFromToken(bearerToken);
            log.info("내 신청 목록 조회 요청: userId={}, role={}, page={}, size={}", userId, userRole, page, size);
            
            PageResponse<ApplicationResponse> pageResponse;
            
            // 사용자 역할에 따라 다른 서비스 메소드 호출
            if ("CLIENT".equals(userRole)) {
                // CLIENT: 자신이 만든 캠페인 목록
                pageResponse = applicationService.getClientCampaigns(userId, Math.max(0, page - 1), size);
            } else {
                // USER: 자신이 신청한 캠페인 목록
                pageResponse = applicationService.getUserApplications(userId, Math.max(0, page - 1), size);
            }
            
            // ApplicationResponse를 구조화된 DTO로 변환
            List<ApplicationListResponseWrapper.ApplicationInfoDTO> applicationList = pageResponse.getContent().stream()
                    .map(ApplicationListResponseWrapper.ApplicationInfoDTO::fromApplicationResponse)
                    .collect(Collectors.toList());
            
            if (includePaging) {
                // 페이징 정보를 포함하는 응답 생성
                ApplicationListResponseWrapper responseWrapper = new ApplicationListResponseWrapper();
                responseWrapper.setApplications(applicationList);
                
                // 페이징 정보 설정 (pageNumber를 1부터 시작하도록 조정)
                ApplicationListResponseWrapper.PaginationInfo paginationInfo = 
                    ApplicationListResponseWrapper.PaginationInfo.builder()
                        .pageNumber(pageResponse.getPageNumber()) // +1 제거
                        .pageSize(pageResponse.getPageSize())
                        .totalPages(pageResponse.getTotalPages())
                        .totalElements(pageResponse.getTotalElements())
                        .first(pageResponse.isFirst())
                        .last(pageResponse.isLast())
                        .build();
                
                responseWrapper.setPagination(paginationInfo);
                
                return ResponseEntity.ok(BaseResponse.success(responseWrapper, "신청 목록을 조회했어요"));
            } else {
                // 페이징 정보 없이 신청 목록만 반환
                return ResponseEntity.ok(BaseResponse.success(applicationList, "신청 목록을 조회했어요"));
            }
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
        }catch (Exception e) {
            log.error("내 신청 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("신청 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 신청 상태 확인", 
        description = "사용자가 특정 캠페인에 신청했는지 여부와 상태를 확인합니다.\n\n" +
                     "### 주요 기능\n" +
                     "- 로그인한 사용자가 특정 캠페인에 이미 신청했는지 확인합니다.\n" +
                     "- 신청한 경우 해당 신청의 상태 정보도 함께 제공합니다.\n\n" +
                     "### 응답 필드 설명\n" +
                     "- **hasApplied**: 신청 여부 (true: 신청함, false: 신청하지 않음)\n" +
                     "- **id, status, createdAt, updatedAt**: 신청한 경우에만 제공되는 상세 정보\n" +
                     "- **campaign, user**: 신청한 경우에만 제공되는 연관 정보\n\n" +
                     "### 사용 예시\n" +
                     "- 캠페인 상세 페이지에서 '신청하기' 버튼의 활성화/비활성화 여부 결정\n" +
                     "- 이미 신청한 캠페인에 대해 신청 상태 표시"
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공 (신청한 경우)",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "신청한 경우 응답",
                        value = "{\n" +
                               "  \"success\": true,\n" +
                               "  \"message\": \"신청 상태 확인 완료\",\n" +
                               "  \"status\": 200,\n" +
                               "  \"data\": {\n" +
                               "    \"application\": {\n" +
                               "      \"id\": 15,\n" +
                               "      \"status\": \"PENDING\",\n" +
                               "      \"hasApplied\": true,\n" +
                               "      \"createdAt\": \"2023-05-17T14:30:15\",\n" +
                               "      \"updatedAt\": \"2023-05-17T14:30:15\",\n" +
                               "      \"campaign\": {\n" +
                               "        \"id\": 42,\n" +
                               "        \"title\": \"신상 음료 체험단 모집\"\n" +
                               "      },\n" +
                               "      \"user\": {\n" +
                               "        \"id\": 5,\n" +
                               "        \"nickname\": \"인플루언서닉네임\"\n" +
                               "      }\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "200", 
                description = "조회 성공 (신청하지 않은 경우)",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "신청하지 않은 경우 응답",
                        value = "{\n" +
                               "  \"success\": true,\n" +
                               "  \"message\": \"신청 상태 확인 완료\",\n" +
                               "  \"status\": 200,\n" +
                               "  \"data\": {\n" +
                               "    \"application\": {\n" +
                               "      \"hasApplied\": false\n" +
                               "    }\n" +
                               "  }\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 실패",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"인증이 필요합니다.\",\n" +
                               "  \"errorCode\": \"UNAUTHORIZED\",\n" +
                               "  \"statusCode\": 401\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "캠페인을 찾을 수 없음",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"캠페인을 찾을 수 없습니다: 42\",\n" +
                               "  \"errorCode\": \"NOT_FOUND\",\n" +
                               "  \"statusCode\": 404\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버 오류",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"fail\",\n" +
                               "  \"message\": \"신청 상태 확인 중 오류가 발생했습니다.\",\n" +
                               "  \"errorCode\": \"INTERNAL_ERROR\",\n" +
                               "  \"statusCode\": 500\n" +
                               "}"
                    )
                )
            )
    })
    @GetMapping("/check")
    public ResponseEntity<?> checkApplicationStatus(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "캠페인 ID", required = true, example = "42") 
            @RequestParam Long campaignId
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("캠페인 신청 상태 확인 요청: userId={}, campaignId={}", userId, campaignId);
            
            // 사용자의 신청 정보 조회 
            ApplicationResponse applicationInfo = applicationService.getUserApplicationInfo(campaignId, userId);
            boolean hasApplied = (applicationInfo != null);
            
            // 구조화된 응답
            if (hasApplied) {
                // 신청 정보를 구조화된 DTO로 변환
                ApplicationListResponseWrapper.ApplicationInfoDTO infoDTO = 
                    ApplicationListResponseWrapper.ApplicationInfoDTO.fromApplicationResponse(applicationInfo);
                
                return ResponseEntity.ok(BaseResponse.success(
                    ApplicationSingleResponseWrapper.of(infoDTO), 
                    "신청 상태를 확인했어요"
                ));
            } else {
                // 신청하지 않은 경우
                return ResponseEntity.ok(BaseResponse.success(
                    ApplicationSingleResponseWrapper.of(
                        ApplicationListResponseWrapper.ApplicationInfoDTO.notApplied()
                    ), 
                    "신청 상태를 확인했어요"
                ));
            }
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
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인 신청 상태 확인 실패 (리소스 없음): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("신청 상태 확인 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("신청 상태 확인 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}