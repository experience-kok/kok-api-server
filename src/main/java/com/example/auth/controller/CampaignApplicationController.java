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
            description = "인플루언서가 선택한 캠페인에 참여 신청합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- 캠페인 참여 의사를 표시하고 신청 정보를 등록합니다.\n" +
                    "- 한 사용자는 동일한 캠페인에 중복 신청할 수 없습니다.\n" +
                    "- 신청 마감일이 지난 캠페인은 신청할 수 없습니다.\n\n" +
                    "### 응답 필드 설명\n" +
                    "- **applicationStatus**: 신청 상태 (PENDING: 대기중, APPROVED: 선정됨, REJECTED: 거절됨, COMPLETED: 완료됨)\n\n" +
                    "### 신청 상태 설명\n" +
                    "- **PENDING**: 신청 접수 상태 (기본값)\n" +
                    "- **APPROVED**: 선정된 신청\n" +
                    "- **REJECTED**: 거절된 신청\n" +
                    "- **COMPLETED**: 체험 및 리뷰까지 완료한 신청"
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
                                            "      \"applicationStatus\": \"PENDING\",\n" +
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
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 또는 이미 신청함"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "캠페인 또는 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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

            ApplicationListResponseWrapper.ApplicationInfoDTO infoDTO =
                    ApplicationListResponseWrapper.ApplicationInfoDTO.fromApplicationResponse(applicationResponse);

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
                    "- 취소된 신청은 데이터베이스에서 완전히 삭제됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "이미 선정된 신청은 취소 불가"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인의 신청이 아님)"),
            @ApiResponse(responseCode = "404", description = "신청 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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
                    .body(BaseResponse.fail("캠페인 신청 취소 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "내 신청 목록 조회",
            description = "로그인한 사용자 역할에 따라 관련된 캠페인 신청 목록을 조회합니다.\n\n" +
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
                    "### 특징:\n" +
                    "- 페이징 없이 전체 목록을 반환합니다\n" +
                    "- 최신순으로 자동 정렬됩니다\n" +
                    "- 최대 200개까지 조회 가능합니다\n\n" +
                    "### 필터링 옵션 (applicationStatus):\n" +
                    "- USER: PENDING, APPROVED, REJECTED, COMPLETED\n" +
                    "- CLIENT: PENDING, APPROVED, REJECTED, EXPIRED"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "USER 역할 - 신청 목록",
                                            value = "{\n" +
                                                    "  \"success\": true,\n" +
                                                    "  \"message\": \"신청 목록을 조회했어요\",\n" +
                                                    "  \"status\": 200,\n" +
                                                    "  \"data\": [\n" +
                                                    "    {\n" +
                                                    "      \"id\": 15,\n" +
                                                    "      \"applicationStatus\": \"PENDING\",\n" +
                                                    "      \"campaign\": {\n" +
                                                    "        \"id\": 42,\n" +
                                                    "        \"title\": \"신상 음료 체험단 모집\"\n" +
                                                    "      },\n" +
                                                    "      \"user\": {\n" +
                                                    "        \"id\": 5,\n" +
                                                    "        \"nickname\": \"인플루언서닉네임\"\n" +
                                                    "      }\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"id\": 16,\n" +
                                                    "      \"applicationStatus\": \"APPROVED\",\n" +
                                                    "      \"campaign\": {\n" +
                                                    "        \"id\": 43,\n" +
                                                    "        \"title\": \"뷰티 제품 체험단\"\n" +
                                                    "      },\n" +
                                                    "      \"user\": {\n" +
                                                    "        \"id\": 5,\n" +
                                                    "        \"nickname\": \"인플루언서닉네임\"\n" +
                                                    "      }\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    ),
                                    @ExampleObject(
                                            name = "CLIENT 역할 - 내가 만든 캠페인 목록",
                                            value = "{\n" +
                                                    "  \"success\": true,\n" +
                                                    "  \"message\": \"신청 목록을 조회했어요\",\n" +
                                                    "  \"status\": 200,\n" +
                                                    "  \"data\": [\n" +
                                                    "    {\n" +
                                                    "      \"id\": 50,\n" +
                                                    "      \"applicationStatus\": \"PENDING\",\n" +
                                                    "      \"campaign\": {\n" +
                                                    "        \"id\": 50,\n" +
                                                    "        \"title\": \"신제품 런칭 캠페인\"\n" +
                                                    "      },\n" +
                                                    "      \"user\": {\n" +
                                                    "        \"id\": 12,\n" +
                                                    "        \"nickname\": \"마케팅담당자\"\n" +
                                                    "      }\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "신청 상태 필터 (USER: PENDING/APPROVED/REJECTED/COMPLETED, CLIENT: PENDING/APPROVED/REJECTED/EXPIRED)", 
                      example = "PENDING")
            @RequestParam(required = false) String applicationStatus
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            String userRole = tokenUtils.getRoleFromToken(bearerToken);
            log.info("내 신청 목록 조회 요청: userId={}, role={}, status={}", userId, userRole, applicationStatus);

            List<ApplicationResponse> applicationList;

            if ("CLIENT".equals(userRole)) {
                applicationList = applicationService.getClientCampaignsList(userId, applicationStatus);
            } else {
                applicationList = applicationService.getUserApplicationsList(userId, applicationStatus);
            }

            // 최대 200개로 제한
            if (applicationList.size() > 200) {
                applicationList = applicationList.subList(0, 200);
                log.warn("신청 목록이 200개를 초과하여 제한됨: userId={}, role={}, totalSize={}", 
                        userId, userRole, applicationList.size());
            }

            List<ApplicationListResponseWrapper.ApplicationInfoDTO> responseDTOs = applicationList.stream()
                    .map(ApplicationListResponseWrapper.ApplicationInfoDTO::fromApplicationResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(BaseResponse.success(responseDTOs, "신청 목록을 조회했어요"));
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
                    "- **id**: 신청 ID\n" +
                    "- **applicationStatus**: 신청 상태 (PENDING, APPROVED, REJECTED, COMPLETED)\n" +
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
                                            "      \"applicationStatus\": \"PENDING\",\n" +
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
                                            "    \"application\": {}\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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

            if (hasApplied) {
                ApplicationListResponseWrapper.ApplicationInfoDTO infoDTO =
                        ApplicationListResponseWrapper.ApplicationInfoDTO.fromApplicationResponse(applicationInfo);

                return ResponseEntity.ok(BaseResponse.success(
                        ApplicationSingleResponseWrapper.of(infoDTO),
                        "신청 상태를 확인했어요"
                ));
            } else {
                return ResponseEntity.ok(BaseResponse.success(
                        ApplicationSingleResponseWrapper.of(
                                ApplicationListResponseWrapper.ApplicationInfoDTO.notApplied()
                        ),
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
                    .body(BaseResponse.fail("신청 상태 확인 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}