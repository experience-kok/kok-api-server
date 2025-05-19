package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.application.*;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.exception.AccessDeniedException;
import com.example.auth.exception.ResourceNotFoundException;
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
                      "### 신청 상태 설명\n" +
                      "- **pending**: 신청 접수 상태 (기본값)\n" +
                      "- **approved**: 선정된 신청\n" +
                      "- **rejected**: 거절된 신청\n" +
                      "- **completed**: 체험 및 리뷰까지 완료한 신청",
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
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"캠페인 신청이 완료되었습니다.\",\n" +
                               "  \"data\": {\n" +
                               "    \"application\": {\n" +
                               "      \"id\": 15,\n" +
                               "      \"status\": \"pending\",\n" +
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
                        "캠페인 신청이 완료되었습니다."
                    ));
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
                     "- 이미 선정된 신청(approved)은 취소할 수 없습니다.\n" +
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
                               "  \"message\": \"캠페인 신청이 취소되었습니다.\",\n" +
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
            
            return ResponseEntity.ok(BaseResponse.success(null, "캠페인 신청이 취소되었습니다."));
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
        description = "로그인한 사용자가 신청한 캠페인 목록을 페이징하여 조회합니다.\n\n" +
                     "### 응답 형식 선택 (includePaging 파라미터):\n" +
                     "- **true (기본값)**: 페이징 정보를 포함한 전체 응답 구조\n" +
                     "- **false**: 요청한 갯수만큼의 신청 목록 데이터만 반환\n\n" +
                     "### 응답 데이터 구조:\n" +
                     "- **applications**: 신청 목록\n" +
                     "  - **id**: 신청 ID\n" +
                     "  - **status**: 신청 상태 (pending, approved, rejected, completed)\n" +
                     "  - **createdAt**: 신청 생성 시간\n" +
                     "  - **updatedAt**: 최종 수정 시간\n" +
                     "  - **campaign**: 캠페인 정보\n" +
                     "    - **id**: 캠페인 ID\n" +
                     "    - **title**: 캠페인 제목\n" +
                     "  - **user**: 사용자 정보\n" +
                     "    - **id**: 사용자 ID\n" +
                     "    - **nickname**: 사용자 닉네임\n" +
                     "- **pagination**: 페이징 정보 객체\n" +
                     "  - **pageNumber**: 현재 페이지 번호 (0부터 시작)\n" +
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
                    examples = @ExampleObject(
                        name = "페이징 정보 포함 응답",
                        value = "{\n" +
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"신청 목록 조회 성공\",\n" +
                               "  \"data\": {\n" +
                               "    \"applications\": [\n" +
                               "      {\n" +
                               "        \"id\": 15,\n" +
                               "        \"status\": \"pending\",\n" +
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
                               "        \"status\": \"approved\",\n" +
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
                               "      \"pageNumber\": 0,\n" +
                               "      \"pageSize\": 10,\n" +
                               "      \"totalPages\": 1,\n" +
                               "      \"totalElements\": 2,\n" +
                               "      \"first\": true,\n" +
                               "      \"last\": true\n" +
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
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") 
            @RequestParam(required = false, defaultValue = "10") int size,
            @Parameter(description = "페이징 정보 포함 여부 (true: 페이징 정보 포함, false: 콘텐츠만 반환)", example = "true") 
            @RequestParam(required = false, defaultValue = "true") boolean includePaging
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("내 신청 목록 조회 요청: userId={}, page={}, size={}", userId, page, size);
            
            PageResponse<ApplicationResponse> pageResponse = 
                    applicationService.getUserApplications(userId, page, size);
            
            // ApplicationResponse를 구조화된 DTO로 변환
            List<ApplicationListResponseWrapper.ApplicationInfoDTO> applicationList = pageResponse.getContent().stream()
                    .map(ApplicationListResponseWrapper.ApplicationInfoDTO::fromApplicationResponse)
                    .collect(Collectors.toList());
            
            if (includePaging) {
                // 페이징 정보를 포함하는 응답 생성
                ApplicationListResponseWrapper responseWrapper = new ApplicationListResponseWrapper();
                responseWrapper.setApplications(applicationList);
                
                // 페이징 정보 설정
                ApplicationListResponseWrapper.PaginationInfo paginationInfo = 
                    ApplicationListResponseWrapper.PaginationInfo.builder()
                        .pageNumber(pageResponse.getPageNumber())
                        .pageSize(pageResponse.getPageSize())
                        .totalPages(pageResponse.getTotalPages())
                        .totalElements(pageResponse.getTotalElements())
                        .first(pageResponse.isFirst())
                        .last(pageResponse.isLast())
                        .build();
                
                responseWrapper.setPagination(paginationInfo);
                
                return ResponseEntity.ok(BaseResponse.success(responseWrapper, "신청 목록 조회 성공"));
            } else {
                // 페이징 정보 없이 신청 목록만 반환
                return ResponseEntity.ok(BaseResponse.success(applicationList, "신청 목록 조회 성공"));
            }
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
                     "### 응답 설명\n" +
                     "- **hasApplied**: 신청 여부 (true/false)\n" +
                     "- **applicationInfo**: 신청 정보 (신청한 경우에만 제공)"
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
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"신청 상태 확인 완료\",\n" +
                               "  \"data\": {\n" +
                               "    \"application\": {\n" +
                               "      \"id\": 15,\n" +
                               "      \"status\": \"pending\",\n" +
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
                               "  \"status\": \"success\",\n" +
                               "  \"message\": \"신청 상태 확인 완료\",\n" +
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
                    "신청 상태 확인 완료"
                ));
            } else {
                // 신청하지 않은 경우
                return ResponseEntity.ok(BaseResponse.success(
                    ApplicationSingleResponseWrapper.of(
                        java.util.Collections.singletonMap("hasApplied", false)
                    ), 
                    "신청 상태 확인 완료"
                ));
            }
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