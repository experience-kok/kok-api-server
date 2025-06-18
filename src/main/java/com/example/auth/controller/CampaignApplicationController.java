package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.constant.UserRole;
import com.example.auth.domain.Campaign;
import com.example.auth.dto.application.*;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.exception.*;
import com.example.auth.repository.CampaignApplicationRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.service.CampaignApplicationService;
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
    private final CampaignApplicationRepository applicationRepository;
    private final CampaignRepository campaignRepository;

    @Operation(
            summary = "캠페인 신청",
            description = "인플루언서가 선택한 캠페인에 참여 신청합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- 캠페인 참여 의사를 표시하고 신청 정보를 등록합니다.\n" +
                    "- 한 사용자는 동일한 캠페인에 중복 신청할 수 없습니다.\n" +
                    "- 신청 마감일이 지난 캠페인은 신청할 수 없습니다.\n\n" +
                    "### 응답 필드 설명\n" +
                    "- **applicationStatus**: 신청 상태 (PENDING: 대기중, APPROVED: 선정됨, REJECTED: 거절됨, COMPLETED: 완료됨)\n" +
                    "- **hasApplied**: 신청 여부 (true: 신청함, false: 신청하지 않음)\n\n" +
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
            description = "로그인한 사용자 역할에 따라 관련된 캠페인 신청 목록을 페이징 형태로 조회합니다.\n\n" +
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
                    "### 페이징 특징:\n" +
                    "- 기본 페이지 크기: 10개\n" +
                    "- 최신순으로 자동 정렬됩니다\n\n" +
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
                                                    "  \"data\": {\n" +
                                                    "    \"applications\": [\n" +
                                                    "      {\n" +
                                                    "        \"id\": 15,\n" +
                                                    "        \"applicationStatus\": \"PENDING\",\n" +
                                                    "        \"hasApplied\": true,\n" +
                                                    "        \"campaign\": {\n" +
                                                    "          \"id\": 42,\n" +
                                                    "          \"title\": \"신상 음료 체험단 모집\",\n" +
                                                    "          \"thumbnailUrl\": \"https://example.com/thumbnail.jpg\"\n" +
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
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1-10)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "신청 상태 필터 (USER: PENDING/APPROVED/REJECTED/COMPLETED, CLIENT: PENDING/APPROVED/REJECTED/EXPIRED)",
                    example = "PENDING")
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
                pageResponse = applicationService.getUserApplications(userId, page - 1, size, applicationStatus);
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
                    .body(BaseResponse.fail("신청 상태 확인 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "캠페인 신청자 목록 조회",
            description = "CLIENT가 자신이 만든 캠페인의 신청자 목록을 조회합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- CLIENT 권한을 가진 사용자만 접근 가능합니다.\n" +
                    "- 본인이 생성한 캠페인의 신청자만 조회할 수 있습니다.\n" +
                    "- 각 신청자의 상세 정보와 SNS 플랫폼 정보를 제공합니다.\n\n" +
                    "### 응답 정보\n" +
                    "- **사용자 기본 정보**: ID, 닉네임, 전화번호\n" +
                    "- **SNS 플랫폼 정보**: 플랫폼 타입, 계정 URL, 팔로워 수\n" +
                    "- **신청 정보**: 신청 ID, 신청 상태, 신청 시간\n\n" +
                    "### 필터링 옵션 (applicationStatus)\n" +
                    "- **PENDING**: 대기 중인 신청\n" +
                    "- **APPROVED**: 선정된 신청\n" +
                    "- **REJECTED**: 거절된 신청\n" +
                    "- **COMPLETED**: 완료된 신청\n\n" +
                    "### 페이징 특징\n" +
                    "- 기본 페이지 크기: 10개\n" +
                    "- 최신 신청순으로 정렬됩니다"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
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
                                            "        \"applicationStatus\": \"pending\",\n" +
                                            "        \"user\": {\n" +
                                            "          \"id\": 5,\n" +
                                            "          \"nickname\": \"인플루언서닉네임\",\n" +
                                            "          \"phone\": \"010-1234-5678\"\n" +
                                            "        },\n" +
                                            "        \"snsPlatforms\": [\n" +
                                            "          {\n" +
                                            "            \"platformType\": \"INSTAGRAM\",\n" +
                                            "            \"accountUrl\": \"https://instagram.com/username\",\n" +
                                            "            \"followerCount\": 10000\n" +
                                            "          },\n" +
                                            "          {\n" +
                                            "            \"platformType\": \"YOUTUBE\",\n" +
                                            "            \"accountUrl\": \"https://youtube.com/c/username\",\n" +
                                            "            \"followerCount\": 5000\n" +
                                            "          }\n" +
                                            "        ]\n" +
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
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (CLIENT 권한 없음 또는 본인 캠페인이 아님)"),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
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
            @Parameter(description = "신청 상태 필터 (PENDING/APPROVED/REJECTED/COMPLETED)", example = "PENDING")
            @RequestParam(required = false) String applicationStatus
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            String userRole = tokenUtils.getRoleFromToken(bearerToken);
            log.info("캠페인 신청자 목록 조회 요청: userId={}, campaignId={}, page={}, size={}, status={}", 
                    userId, campaignId, page, size, applicationStatus);

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
                    .body(BaseResponse.fail("신청자 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
