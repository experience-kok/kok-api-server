package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.constant.UserRole;
import com.example.auth.domain.Campaign;
import com.example.auth.dto.application.*;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.dto.mission.MultipleSelectionRequest;
import com.example.auth.dto.mission.MultipleSelectionResponse;
import com.example.auth.exception.*;
import com.example.auth.repository.CampaignApplicationRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.service.CampaignApplicationService;
import com.example.auth.service.MissionManagementService;
import com.example.auth.service.MyCampaignService;
import com.example.auth.util.AuthUtil;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

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
    private final MyCampaignService myCampaignService; // 추가
    private final MissionManagementService missionManagementService; // 선정 기능을 위해 추가
    private final TokenUtils tokenUtils;
    private final CampaignApplicationRepository applicationRepository;
    private final CampaignRepository campaignRepository;


    @Operation(
            summary = "캠페인 신청",
            description = "인플루언서가 선택한 캠페인에 참여 신청합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- 캠페인 참여 의사를 표시하고 신청 정보를 등록합니다.\n" +
                    "- 한 사용자는 동일한 캠페인에 중복 신청할 수 없습니다.\n" +
                    "- 신청 마감일이 지난 캠페인은 신청할 수 없습니다.\n\n" +
                    "### 신청 조건\n" +
                    "- **USER(인플루언서) 권한** 필요\n" +
                    "- **프로필 완성도**: 닉네임, 나이, 성별 정보 필수\n" +
                    "- **SNS 연동**: 캠페인 타입에 맞는 SNS 계정 연동 필수\n" +
                    "  - 인스타그램 캠페인 → 인스타그램 계정 연동\n" +
                    "  - 유튜브 캠페인 → 유튜브 계정 연동\n" +
                    "  - 블로그 캠페인 → 블로그 계정 연동\n\n" +
                    "### 응답 필드 설명\n" +
                    "- **applicationStatus**: 신청 상태 (PENDING: 대기중, APPROVED: 선정됨, REJECTED: 거절됨, COMPLETED: 완료됨)\n" +
                    "- **hasApplied**: 신청 여부 (true: 신청함, false: 신청하지 않음)\n\n" +
                    "### 신청 상태 설명\n" +
                    "- **PENDING**: 신청 접수 상태 (기본값)\n" +
                    "- **APPROVED**: 선정된 신청\n" +
                    "- **REJECTED**: 거절된 신청\n" +
                    "- **COMPLETED**: 체험 및 리뷰까지 완료한 신청\n\n" +
                    "### 주요 에러 케이스\n" +
                    "- **PROFILE_INCOMPLETE**: (닉네임/나이/성별 미설정)\n" +
                    "- **SNS_CONNECTION_REQUIRED**:  (SNS 계정 연동 없음)\n" +
                    "- **PLATFORM_MISMATCH**:  (캠페인 요구 플랫폼 불일치)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "신청 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignApplicationSuccessResponse"),
                            examples = @ExampleObject(
                                    name = "성공 응답 예시",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"캠페인 신청이 완료되었어요\",\n" +
                                            "  \"status\": 201,\n" +
                                            "  \"data\": {\n" +
                                            "    \"application\": {\n" +
                                            "      \"id\": 15,\n" +
                                            "      \"applicationStatus\": \"PENDING\",\n" +
                                            "      \"hasApplied\": true,\n" +
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
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 신청 조건 미충족",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "이미 신청한 경우",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "이미 해당 캠페인에 신청하셨어요.",
                                                      "errorCode": "ALREADY_APPLIED",
                                                      "status": 400
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "신청 마감",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "신청이 마감된 캠페인이에요.",
                                                      "errorCode": "APPLICATION_DEADLINE_PASSED",
                                                      "status": 400
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "프로필 미설정",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "프로필을 설정해주세요. 캠페인 신청을 위해 프로필 정보가 필요해요.",
                                                      "errorCode": "PROFILE_INCOMPLETE",
                                                      "status": 400
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "SNS 연동 없음",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "캠페인 신청을 위해 SNS 계정 연동이 필요해요. 프로필에서 SNS 계정을 연동해주세요.",
                                                      "errorCode": "SNS_CONNECTION_REQUIRED",
                                                      "status": 400
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "플랫폼 불일치",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "message": "이 캠페인은 인스타그램 계정이 필요해요. 프로필에서 인스타그램 계정을 연동해주세요.",
                                                      "errorCode": "PLATFORM_MISMATCH",
                                                      "status": 400
                                                    }
                                                    """
                                    )
                            },
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "캠페인 또는 사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping
    public ResponseEntity<?> applyForCampaign(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Valid @RequestBody CampaignApplicationRequest request
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            String userRole = tokenUtils.getRoleFromToken(bearerToken);
            log.info("캠페인 신청 요청: userId={}, userRole={}, campaignId={}", userId, userRole, request.getCampaignId());

            // 추가 권한 검증: USER(인플루언서)만 캠페인 신청 가능
            if (!UserRole.USER.getValue().equals(userRole)) {
                String errorMessage = switch (userRole) {
                    case "CLIENT" -> "기업 회원은 캠페인에 신청할 수 없어요.";
                    case "ADMIN" -> "관리자는 캠페인에 신청할 수 없어요.";
                    default -> "인플루언서만 캠페인에 신청할 수 있어요.";
                };

                log.warn("캠페인 신청 권한 없음: userId={}, userRole={}", userId, userRole);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(BaseResponse.fail(errorMessage, "INSUFFICIENT_ROLE", HttpStatus.FORBIDDEN.value()));
            }

            ApplicationResponse applicationResponse =
                    applicationService.createApplication(request.getCampaignId(), userId);

            ApplicationListResponseWrapper.ApplicationInfoDTO infoDTO =
                    ApplicationListResponseWrapper.ApplicationInfoDTO.fromApplicationResponseWithApplied(applicationResponse);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success(
                            ApplicationSingleResponseWrapper.of(infoDTO),
                            "캠페인 신청이 완료되었어요"
                    ));
        } catch (JwtValidationException e) {
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

            // 사용자 정보 부족에 따른 구체적인 에러 코드 반환
            String errorCode = "INVALID_REQUEST";
            String message = e.getMessage();

            if (message.contains("프로필을 설정해주세요")) {
                errorCode = "PROFILE_INCOMPLETE";
            } else if (message.contains("SNS 계정 연동이 필요해요")) {
                errorCode = "SNS_CONNECTION_REQUIRED";
            } else if (message.contains("계정이 필요해요")) {
                errorCode = "PLATFORM_MISMATCH";
            } else if (message.contains("이미 해당 캠페인에 신청")) {
                errorCode = "ALREADY_APPLIED";
            } else if (message.contains("신청이 마감된")) {
                errorCode = "APPLICATION_DEADLINE_PASSED";
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(message, errorCode, HttpStatus.BAD_REQUEST.value()));
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
                    .body(BaseResponse.fail("캠페인 신청 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "신청 취소",
            description = "인플루언서가 캠페인 신청을 취소합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- 본인이 신청한 캠페인만 취소할 수 있습니다.\n" +
                    "- 이미 선정된 신청(APPROVED)은 취소할 수 없습니다.\n" +
                    "- 취소된 신청은 데이터베이스에서 완전히 삭제됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "취소 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiSuccessResponse"))),
            @ApiResponse(responseCode = "400", description = "이미 선정된 신청은 취소 불가",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인의 신청이 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "신청 정보를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
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
        } catch (JwtValidationException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            String errorCode = e.getErrorType() == TokenErrorType.EXPIRED ? "TOKEN_EXPIRED" : "TOKEN_INVALID";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (UnauthorizedException e) {
            log.warn("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        } catch (ResourceNotFoundException e) {
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
                    .body(BaseResponse.fail("캠페인 신청 취소 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "내 신청 목록 조회",
            description = "로그인한 사용자 역할에 따라 관련된 캠페인 신청 목록을 페이징 형태로 조회합니다. **상시 캠페인도 포함됩니다.**\n\n" +
                    "### 사용자 역할별 조회 내용:\n" +
                    "- **USER (인플루언서)**: 본인이 신청한 캠페인 목록 (상시 캠페인 포함)\n" +
                    "- **CLIENT (기업)**: 내가 만든 캠페인 목록\n\n" +
                    "### 신청 상태 (ApplicationStatus):\n" +
                    "- **APPLIED**: 신청 완료 (모집 중인 캠페인 + 상시 캠페인)\n" +
                    "- **PENDING**: 대기중 (모집 기간 종료된 일반 캠페인)\n" +
                    "- **SELECTED**: 선정됨\n" +
                    "- **COMPLETED**: 완료됨\n\n" +
                    "### 상시 캠페인 특징:\n" +
                    "- **상시 캠페인**: 모집 마감일이 없어 언제든 신청 가능한 캠페인\n" +
                    "- **상태 유지**: 상시 캠페인 신청은 항상 APPLIED 상태를 유지합니다\n" +
                    "- **마감일 표시**: 상시 캠페인은 `applicationEndDate`가 `null`로 표시됩니다\n\n" +
                    "### 캠페인 승인 상태 (ApprovalStatus) - CLIENT 응답시:\n" +
                    "- **PENDING**: 관리자 승인 대기중\n" +
                    "- **APPROVED**: 관리자 승인됨 (활성화)\n" +
                    "- **REJECTED**: 관리자 거절됨\n" +
                    "- **EXPIRED**: 승인됐지만 모집기간이 종료됨 (만료됨)\n\n" +
                    "### 페이징 특징:\n" +
                    "- 기본 페이지 크기: 10개\n" +
                    "- 최신순으로 자동 정렬됩니다\n\n" +
                    "### 필터링 옵션 (applicationStatus):\n" +
                    "- **USER**: APPLIED, PENDING, SELCTED, COMPLETED\n" +
                    "- **CLIENT**: PENDING, APPROVED, REJECTED, EXPIRED"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/MyApplicationsSuccessResponse"),
                            examples = {
                                    @ExampleObject(
                                            name = "USER 역할 - 신청 목록",
                                            value = "{\n" +
                                                    "  \"success\": true,\n" +
                                                    "  \"message\": \"신청 목록을 조회했어요\",\n" +
                                                    "  \"status\": 200,\n" +
                                                    "  \"data\": {\n" +
                                                    "    \"applications\": [\n" +
                                                    "      {\n" +
                                                    "        \"id\": 15,\n" +
                                                    "        \"applicationStatus\": \"APPLIED\",\n" +
                                                    "        \"hasApplied\": true,\n" +
                                                    "        \"campaign\": {\n" +
                                                    "          \"id\": 42,\n" +
                                                    "          \"isAlwaysOpen\": true,\n" +
                                                    "          \"title\": \"상시 모집 카페 체험단\",\n" +
                                                    "          \"thumbnailUrl\": \"https://example.com/thumbnail.jpg\",\n" +
                                                    "          \"applicationEndDate\": null\n" +
                                                    "        },\n" +
                                                    "        \"user\": {\n" +
                                                    "          \"id\": 5,\n" +
                                                    "          \"nickname\": \"인플루언서닉네임\"\n" +
                                                    "        }\n" +
                                                    "      },\n" +
                                                    "      {\n" +
                                                    "        \"id\": 16,\n" +
                                                    "        \"applicationStatus\": \"APPLIED\",\n" +
                                                    "        \"hasApplied\": true,\n" +
                                                    "        \"campaign\": {\n" +
                                                    "          \"id\": 43,\n" +
                                                    "          \"isAlwaysOpen\": false,\n" +
                                                    "          \"title\": \"신상 음료 체험단 모집\",\n" +
                                                    "          \"thumbnailUrl\": \"https://example.com/thumbnail2.jpg\",\n" +
                                                    "          \"applicationEndDate\": \"2025-12-31\"\n" +
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
                                                    "      \"totalPages\": 3,\n" +
                                                    "      \"totalElements\": 25,\n" +
                                                    "      \"first\": true,\n" +
                                                    "      \"last\": false\n" +
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
                                                    "        \"applicationStatus\": \"PENDING\",\n" +
                                                    "        \"hasApplied\": true,\n" +
                                                    "        \"campaign\": {\n" +
                                                    "          \"id\": 50,\n" +
                                                    "          \"title\": \"신제품 런칭 캠페인\",\n" +
                                                    "          \"thumbnailUrl\": \"https://example.com/campaign-thumbnail.jpg\"\n" +
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
                                                    "      \"totalElements\": 5,\n" +
                                                    "      \"first\": true,\n" +
                                                    "      \"last\": true\n" +
                                                    "    }\n" +
                                                    "  }\n" +
                                                    "}"
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1-10)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "신청 상태 필터 (USER: APPLIED, PENDING, SELECTED, COMPLETED | CLIENT: PENDING, APPROVED, REJECTED, EXPIRED)",
                    example = "APPLIED")
            @RequestParam(required = false) String applicationStatus
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            String userRole = tokenUtils.getRoleFromToken(bearerToken);
            log.info("내 신청 목록 조회 요청: userId={}, role={}, page={}, size={}, status={}",
                    userId, userRole, page, size, applicationStatus);

            // 페이지 번호 검증 (1부터 시작하므로 0으로 변환)
            if (page < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 번호는 1 이상이어야 합니다.", "INVALID_PAGE", HttpStatus.BAD_REQUEST.value()));
            }

            // 페이지 크기 검증
            if (size < 1 || size > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 크기는 1-100 사이여야 합니다.", "INVALID_PAGE_SIZE", HttpStatus.BAD_REQUEST.value()));
            }

            PageResponse<ApplicationResponse> pageResponse;

            if ("CLIENT".equals(userRole)) {
                pageResponse = applicationService.getClientCampaigns(userId, page - 1, size, applicationStatus);
            } else {
                // USER 역할인 경우 MyCampaignService의 호환성 메서드 사용
                pageResponse = myCampaignService.getUserApplicationsCompat(userId, page - 1, size, applicationStatus);
            }

            // PageResponse를 ApplicationListResponseWrapper 형태로 변환
            List<ApplicationListResponseWrapper.ApplicationInfoDTO> responseDTOs = pageResponse.getContent().stream()
                    .map(ApplicationListResponseWrapper.ApplicationInfoDTO::fromApplicationResponse)
                    .collect(Collectors.toList());

            ApplicationListResponseWrapper.PaginationInfo paginationInfo =
                    ApplicationListResponseWrapper.PaginationInfo.builder()
                            .pageNumber(pageResponse.getPageNumber() + 1) // 1부터 시작하도록 변환
                            .pageSize(pageResponse.getPageSize())
                            .totalPages(pageResponse.getTotalPages())
                            .totalElements(pageResponse.getTotalElements())
                            .first(pageResponse.isFirst())
                            .last(pageResponse.isLast())
                            .build();

            ApplicationListResponseWrapper wrapper = ApplicationListResponseWrapper.builder()
                    .applications(responseDTOs)
                    .pagination(paginationInfo)
                    .build();

            return ResponseEntity.ok(BaseResponse.success(wrapper, "신청 목록을 조회했어요"));
        } catch (JwtValidationException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            String errorCode = e.getErrorType() == TokenErrorType.EXPIRED ? "TOKEN_EXPIRED" : "TOKEN_INVALID";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (UnauthorizedException e) {
            log.warn("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("내 신청 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("신청 목록 조회 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "캠페인 신청 상태 확인",
            description = "사용자가 특정 캠페인에 신청했는지 여부와 상태를 확인합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- 로그인한 사용자가 특정 캠페인에 이미 신청했는지 확인합니다.\n" +
                    "- 신청한 경우 해당 신청의 상태 정보도 함께 제공합니다.\n\n" +
                    "### 응답 필드 설명\n" +
                    "- **id**: 신청 ID\n" +
                    "- **applicationStatus**: 신청 상태 (PENDING, APPROVED, SELECTED, COMPLETED)\n" +
                    "- **hasApplied**: 신청 여부 (true: 신청함, false: 신청하지 않음)\n" +
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
                                            "  \"message\": \"신청 상태를 확인했어요\",\n" +
                                            "  \"status\": 200,\n" +
                                            "  \"data\": {\n" +
                                            "    \"application\": {\n" +
                                            "      \"id\": 15,\n" +
                                            "      \"applicationStatus\": \"PENDING\",\n" +
                                            "      \"hasApplied\": true,\n" +
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
                                            "  \"message\": \"신청 상태를 확인했어요\",\n" +
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
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
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

            ApplicationResponse applicationInfo = applicationService.getUserApplicationInfo(campaignId, userId);
            boolean hasApplied = (applicationInfo != null);

            log.info("캠페인 신청 상태 확인 결과: userId={}, campaignId={}, hasApplied={}, applicationInfo={}",
                    userId, campaignId, hasApplied, applicationInfo);

            if (hasApplied) {
                ApplicationListResponseWrapper.ApplicationInfoDTO infoDTO =
                        ApplicationListResponseWrapper.ApplicationInfoDTO.fromApplicationResponseForCheck(applicationInfo);

                log.info("신청한 경우 응답 DTO: {}", infoDTO);

                return ResponseEntity.ok(BaseResponse.success(
                        ApplicationSingleResponseWrapper.of(infoDTO),
                        "신청 상태를 확인했어요"
                ));
            } else {
                ApplicationListResponseWrapper.ApplicationInfoDTO notAppliedDTO =
                        ApplicationListResponseWrapper.ApplicationInfoDTO.notApplied();

                log.info("신청하지 않은 경우 응답 DTO: {}", notAppliedDTO);

                return ResponseEntity.ok(BaseResponse.success(
                        ApplicationSingleResponseWrapper.of(notAppliedDTO),
                        "신청 상태를 확인했어요"
                ));
            }
        } catch (JwtValidationException e) {
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
                    .body(BaseResponse.fail("신청 상태 확인 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "캠페인 신청자 목록 조회",
            description = "CLIENT가 자신이 만든 캠페인의 모든 신청자 목록을 조회합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- CLIENT 권한을 가진 사용자만 접근 가능합니다.\n" +
                    "- 본인이 생성한 캠페인의 신청자만 조회할 수 있습니다.\n" +
                    "- 신청 상태별로 필터링하여 조회할 수 있습니다.\n" +
                    "- 각 신청자의 상세 정보와 캠페인 타입에 맞는 SNS 주소를 제공합니다.\n\n" +
                    "### 신청 상태별 조회\n" +
                    "- **전체 조회** (필터 없음): 모든 상태의 신청자 조회\n" +
                    "- **APPLIED**: 신청 중인 인플루언서\n" +
                    "- **PENDING**: 모집 마감 후 선정 대기 중인 인플루언서\n" +
                    "- **SELECTED**: 선정된 인플루언서\n" +
                    "- **COMPLETED**: 미션 완료한 인플루언서\n" +
                    "- **REJECTED**: 거절된 인플루언서  (추가)\n\n" +
                    "### 응답 정보\n" +
                    "- **사용자 기본 정보**: ID, 닉네임, 전화번호, 성별\n" +
                    "- **SNS 정보**: 캠페인 타입(인스타그램/유튜브 등)에 맞는 계정 URL\n" +
                    "- **신청 정보**: 신청 ID\n\n" +
                    "###null 값 가능 필드\n" +
                    "- **profileImage**: 프로필 이미지를 설정하지 않은 경우 `null`\n" +
                    "- **phone**: 전화번호를 입력하지 않은 경우 `null`\n" +
                    "- **gender**: 성별을 설정하지 않은 경우 `\"UNKNOWN\"` 또는 `null`\n" +
                    "- **allSnsUrls**: SNS를 연동하지 않은 경우 빈 배열 `[]`\n\n" +
                    "### 페이징 특징\n" +
                    "- 기본 페이지 크기: 10개\n" +
                    "- 최신 신청순으로 정렬됩니다\n\n" +
                    "### 활용 예시\n" +
                    "```\n" +
                    "전체 신청자: GET /campaigns/123/applicants\n" +
                    "선정된 신청자: GET /campaigns/123/applicants?applicationStatus=SELECTED\n" +
                    "거절된 신청자: GET /campaigns/123/applicants?applicationStatus=REJECTED\n" +
                    "```"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignApplicantListResponse"),
                            examples = @ExampleObject(
                                    name = "신청자 목록 조회 성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"캠페인 신청자 목록을 조회했어요\",\n" +
                                            "  \"status\": 200,\n" +
                                            "  \"data\": {\n" +
                                            "    \"campaign\": {\n" +
                                            "      \"id\": 42,\n" +
                                            "      \"title\": \"신상 음료 체험단 모집\",\n" +
                                            "      \"totalApplicants\": 15\n" +
                                            "    },\n" +
                                            "    \"applicants\": [\n" +
                                            "      {\n" +
                                            "        \"applicationId\": 101,\n" +
                                            "        \"user\": {\n" +
                                            "          \"id\": 5,\n" +
                                            "          \"profileImage\": \"https://example.com/profile2.jpg\",\n" +
                                            "          \"nickname\": \"인플루언서닉네임\",\n" +
                                            "          \"phone\": \"010-1234-5678\",\n" +
                                            "          \"gender\": \"FEMALE\"\n" +
                                            "        },\n" +
                                            "        \"allSnsUrls\": [\n" +
                                            "          {\n" +
                                            "            \"platformType\": \"INSTAGRAM\",\n" +
                                            "            \"snsUrl\": \"https://instagram.com/username\"\n" +
                                            "          },\n" +
                                            "          {\n" +
                                            "            \"platformType\": \"YOUTUBE\",\n" +
                                            "            \"snsUrl\": \"https://youtube.com/c/username\"\n" +
                                            "          }\n" +
                                            "        ],\n" +
                                            "        \"mission\": {\n" +
                                            "          \"missionId\": 123,\n" +
                                            "          \"missionStatus\": \"COMPLETED\",\n" +
                                            "          \"missionUrl\": \"https://instagram.com/p/abc123\"\n" +
                                            "        }\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"applicationId\": 102,\n" +
                                            "        \"user\": {\n" +
                                            "          \"id\": 6,\n" +
                                            "          \"profileImage\": null,\n" +
                                            "          \"nickname\": \"신규인플루언서\",\n" +
                                            "          \"phone\": null,\n" +
                                            "          \"gender\": \"UNKNOWN\"\n" +
                                            "        },\n" +
                                            "        \"allSnsUrls\": [\n" +
                                            "          {\n" +
                                            "            \"platformType\": \"INSTAGRAM\",\n" +
                                            "            \"snsUrl\": \"https://instagram.com/username2\"\n" +
                                            "          }\n" +
                                            "        ],\n" +
                                            "        \"mission\": {\n" +
                                            "          \"missionId\": null,\n" +
                                            "          \"missionStatus\": \"NOT_SUBMITTED\",\n" +
                                            "          \"missionUrl\": null\n" +
                                            "        }\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"applicationId\": 103,\n" +
                                            "        \"user\": {\n" +
                                            "          \"id\": 7,\n" +
                                            "          \"profileImage\": \"https://example.com/profile3.jpg\",\n" +
                                            "          \"nickname\": \"체험단고수\",\n" +
                                            "          \"phone\": \"010-9876-5432\",\n" +
                                            "          \"gender\": \"MALE\"\n" +
                                            "        },\n" +
                                            "        \"allSnsUrls\": [\n" +
                                            "          {\n" +
                                            "            \"platformType\": \"INSTAGRAM\",\n" +
                                            "            \"snsUrl\": \"https://instagram.com/username3\"\n" +
                                            "          }\n" +
                                            "        ],\n" +
                                            "        \"mission\": {\n" +
                                            "          \"missionId\": 124,\n" +
                                            "          \"missionStatus\": \"SUBMITTED\",\n" +
                                            "          \"missionUrl\": \"https://instagram.com/p/def456\"\n" +
                                            "        }\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"pagination\": {\n" +
                                            "      \"pageNumber\": 1,\n" +
                                            "      \"pageSize\": 10,\n" +
                                            "      \"totalPages\": 2,\n" +
                                            "      \"totalElements\": 15,\n" +
                                            "      \"first\": true,\n" +
                                            "      \"last\": false\n" +
                                            "    }\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (CLIENT 권한 없음 또는 본인 캠페인이 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/campaigns/{campaignId}/applicants")
    public ResponseEntity<?> getCampaignApplicants(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "캠페인 ID", required = true, example = "42")
            @PathVariable Long campaignId,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1-100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "신청 상태 필터 (선택사항) - APPLIED: 신청중, PENDING: 대기중, SELECTED: 선정됨, COMPLETED: 완료됨, REJECTED: 반려됨",
                    example = "SELECTED")
            @RequestParam(required = false) String applicationStatus
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            String userRole = tokenUtils.getRoleFromToken(bearerToken);
            log.info("캠페인 신청자 목록 조회 요청: userId={}, campaignId={}, page={}, size={}",
                    userId, campaignId, page, size);

            // CLIENT 권한 확인
            if (!UserRole.CLIENT.getValue().equals(userRole)) {
                log.warn("CLIENT 권한 없음: userId={}, userRole={}", userId, userRole);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(BaseResponse.fail("기업 회원만 신청자 목록을 조회할 수 있어요.", "INSUFFICIENT_ROLE", HttpStatus.FORBIDDEN.value()));
            }

            // 페이지 번호 검증 (1부터 시작하므로 0으로 변환)
            if (page < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 번호는 1 이상이어야 합니다.", "INVALID_PAGE", HttpStatus.BAD_REQUEST.value()));
            }

            // 페이지 크기 검증
            if (size < 1 || size > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 크기는 1-100 사이여야 합니다.", "INVALID_PAGE_SIZE", HttpStatus.BAD_REQUEST.value()));
            }

            // 모든 신청자 조회 (필터링 없음) - CLIENT는 REJECTED도 볼 수 있어야 함
            PageResponse<CampaignApplicantResponse> pageResponse =
                    applicationService.getCampaignApplicants(campaignId, userId, page - 1, size, applicationStatus);

            // 캠페인 정보와 총 신청자 수 조회
            Campaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

            long totalApplicants = applicationRepository.countByCampaignId(campaignId);

            // 응답 생성
            CampaignApplicantListResponse response = CampaignApplicantListResponse.from(
                    campaignId,
                    campaign.getTitle(),
                    totalApplicants,
                    pageResponse
            );

            return ResponseEntity.ok(BaseResponse.success(response, "캠페인 신청자 목록을 조회했어요"));

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
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "FORBIDDEN", HttpStatus.FORBIDDEN.value()));
        } catch (ResourceNotFoundException e) {
            log.warn("리소스 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("캠페인 신청자 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("신청자 목록 조회 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            operationId = "selectMultipleInfluencers",
            summary = "인플루언서 선정",
            description = "캠페인 신청자 중에서 여러 인플루언서를 한번에 선정합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- CLIENT 권한을 가진 사용자만 사용 가능합니다.\n" +
                    "- 본인이 생성한 캠페인의 신청자만 선정할 수 있습니다.\n" +
                    "- 여러 신청자를 한번에 선정하여 효율적인 관리가 가능합니다.\n" +
                    "- PENDING 상태의 신청자만 선정 가능합니다.\n\n" +
                    "### 선정 프로세스\n" +
                    "1. **상태 변경**: 선택된 신청자들을 SELECTED 상태로 변경\n" +
                    "2. **결과 반환**: 성공/실패 결과 제공\n\n" +
                    "### 요청 데이터 예시\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"applicationIds\": [12, 45, 78]\n" +
                    "}\n" +
                    "```",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MultipleSelectionRequest.class),
                            examples = @ExampleObject(
                                    name = "인플루언서 선정 요청",
                                    value = """
                                            {
                                              "applicationIds": [12, 45, 78]
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "선정 성공 (부분 성공 포함)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "선정 처리 성공",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "인플루언서 선정이 완료되었어요.",
                                              "status": 200,
                                              "data": {
                                                "totalRequested": 3,
                                                "successCount": 3,
                                                "failCount": 0,
                                                "successfulSelections": [12, 45, 78],
                                                "failedSelections": []
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (CLIENT 권한 없음 또는 본인 캠페인이 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (신청 ID 오류, 선정 불가능한 상태 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            )
    })
    @PostMapping("/campaigns/{campaignId}/applications/select")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> selectMultipleInfluencers(
            @Parameter(description = "캠페인 ID", required = true, example = "123")
            @PathVariable Long campaignId,
            @Parameter(description = "선정할 신청 ID 목록")
            @RequestBody @Valid MultipleSelectionRequest request
    ) {
        try {
            Long clientId = AuthUtil.getCurrentUserId();

            // 선정 처리
            MultipleSelectionResponse response = missionManagementService.selectMultipleInfluencers(
                    campaignId, request.getApplicationIds(), clientId);

            log.info("인플루언서 선정 완료 - campaignId: {}, 요청 수: {}, 성공 수: {}, 실패 수: {}, clientId: {}",
                    campaignId, response.getTotalRequested(), response.getSuccessCount(),
                    response.getFailCount(), clientId);

            return ResponseEntity.ok(BaseResponse.success(response, "인플루언서 선정이 완료되었어요."));

        } catch (ResourceNotFoundException e) {
            log.warn("리소스 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (AccessDeniedException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "FORBIDDEN", HttpStatus.FORBIDDEN.value()));
        } catch (IllegalStateException e) {
            log.warn("선정 처리 실패 (비즈니스 로직): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "INVALID_SELECTION_STATE", HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            log.error("인플루언서 선정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("인플루언서 선정 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            operationId = "rejectMultipleInfluencers",
            summary = "인플루언서 반려 처리",
            description = "캠페인 신청자 중에서 여러 인플루언서를 반려합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- CLIENT 권한을 가진 사용자만 사용 가능합니다.\n" +
                    "- 본인이 생성한 캠페인의 신청자만 반려할 수 있습니다.\n" +
                    "- 여러 신청자를 한번에 반려하여 효율적인 관리가 가능합니다.\n" +
                    "- PENDING 또는 SELECTED 상태의 신청자만 반려 가능합니다.\n\n" +
                    "### 반려 프로세스\n" +
                    "**결과 반환**: 성공/실패 통계와 상세 결과 제공\n\n" +
                    "### 요청 데이터 예시\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"applicationIds\": [91, 123, 456]\n" +
                    "}\n" +
                    "```",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MultipleSelectionRequest.class),
                            examples = @ExampleObject(
                                    name = "인플루언서 반려 요청",
                                    value = """
                                            {
                                              "applicationIds": [91, 123, 456]
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "반려 성공 (부분 성공 포함)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "반려 처리 성공",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "인플루언서 반려가 완료되었어요.",
                                              "status": 200,
                                              "data": {
                                                "totalRequested": 3,
                                                "successCount": 3,
                                                "failCount": 0,
                                                "successfulSelections": [91, 123, 456],
                                                "failedSelections": []
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (CLIENT 권한 없음 또는 본인 캠페인이 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (신청 ID 오류, 반려 불가능한 상태 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            )
    })
    @PostMapping("/campaigns/{campaignId}/applications/reject")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> rejectMultipleInfluencers(
            @Parameter(description = "캠페인 ID", required = true, example = "123")
            @PathVariable Long campaignId,
            @Parameter(description = "반려할 신청 ID 목록")
            @RequestBody @Valid MultipleSelectionRequest request
    ) {
        try {
            Long clientId = AuthUtil.getCurrentUserId();

            MultipleSelectionResponse response = missionManagementService.rejectMultipleInfluencers(
                    campaignId, request.getApplicationIds(), clientId);

            log.info("인플루언서 반려 완료 - campaignId: {}, 요청 수: {}, 성공 수: {}, 실패 수: {}, clientId: {}",
                    campaignId, response.getTotalRequested(), response.getSuccessCount(),
                    response.getFailCount(), clientId);

            return ResponseEntity.ok(BaseResponse.success(response, "인플루언서 반려가 완료되었어요."));

        } catch (ResourceNotFoundException e) {
            log.warn("리소스 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (AccessDeniedException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "FORBIDDEN", HttpStatus.FORBIDDEN.value()));
        } catch (IllegalStateException e) {
            log.warn("반려 처리 실패 (비즈니스 로직): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "INVALID_REJECTION_STATE", HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            log.error("인플루언서 반려 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("인플루언서 반려 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
