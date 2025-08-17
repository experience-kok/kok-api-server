package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.mission.*;
import com.example.auth.service.MissionManagementService;
import com.example.auth.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
@Tag(name = "미션 관리 API", description = "인플루언서 미션 제출, 검토, 포트폴리오 관리를 위한 REST API")
public class MissionController {

    private final MissionManagementService missionManagementService;

    // ===== 클라이언트용 API =====

    @Operation(
            operationId = "selectMultipleInfluencers",
            summary = "인플루언서 다중 선정",
            description = "캠페인 신청자 중에서 여러 인플루언서를 한번에 선정합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MultipleSelectionRequest.class),
                            examples = @ExampleObject(
                                    name = "인플루언서 다중 선정 요청",
                                    value = """
                                            {
                                              "applicationIds": [12, 45, 78, 91, 123]
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
                            schema = @Schema(ref = "#/components/schemas/MultipleSelectionSuccessResponse"),
                            examples = @ExampleObject(
                                    name = "다중 선정 성공",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "인플루언서 다중 선정이 완료되었습니다.",
                                              "status": 200,
                                              "data": {
                                                "totalRequested": 5,
                                                "successCount": 5,
                                                "failCount": 0,
                                                "successfulSelections": [
                                                  {
                                                    "applicationId": 12,
                                                    "influencerName": "맛집탐험가",
                                                    "status": "SELECTED"
                                                  }
                                                ],
                                                "failedSelections": []
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403", 
                    description = "권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignAccessDeniedErrorResponse")
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
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/MultipleSelectionErrorResponse")
                    )
            )
    })
    @PostMapping("/campaigns/{campaignId}/applications/select")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> selectMultipleInfluencers(
            @Parameter(description = "캠페인 ID", required = true, example = "123")
            @PathVariable Long campaignId,
            @Parameter(description = "선정할 신청 ID 목록이 포함된 요청 데이터")
            @RequestBody @Valid MultipleSelectionRequest request
    ) {
        try {
            Long clientId = AuthUtil.getCurrentUserId();
            MultipleSelectionResponse response = missionManagementService.selectMultipleInfluencers(
                    campaignId, request.getApplicationIds(), clientId);
            
            log.info("인플루언서 다중 선정 완료 - campaignId: {}, 요청 수: {}, 성공 수: {}, 실패 수: {}, clientId: {}", 
                    campaignId, response.getTotalRequested(), response.getSuccessCount(), 
                    response.getFailCount(), clientId);
            
            return ResponseEntity.ok(BaseResponse.success(response, "인플루언서 다중 선정이 완료되었습니다."));
        } catch (Exception e) {
            log.error("인플루언서 다중 선정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "MULTIPLE_SELECTION_ERROR", HttpStatus.BAD_REQUEST.value()));
        }
    }

    @Operation(
            operationId = "getCampaignMissionSubmissions",
            summary = "캠페인별 미션 제출 목록 조회",
            description = "특정 캠페인에 제출된 모든 미션 목록을 조회합니다. 캠페인 생성자만 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignMissionSubmissionsSuccessResponse"),
                            examples = @ExampleObject(
                                    name = "미션 제출 목록",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "미션 목록 조회 성공",
                                              "status": 200,
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "submissionUrl": "https://instagram.com/p/abc123",
                                                  "submissionTitle": "맛집 체험 후기 - 이탈리안 레스토랑",
                                                  "submissionDescription": "파스타와 와인을 체험하고 솔직한 후기를 작성했습니다.",
                                                  "platformType": "인스타그램",
                                                  "submittedAt": "2024-03-15T14:30:00Z",
                                                  "reviewStatus": "APPROVED",
                                                  "clientFeedback": "미션을 잘 수행해주셨습니다.",
                                                  "clientRating": 5,
                                                  "revisionCount": 0,
                                                  "influencer": {
                                                    "id": 45,
                                                    "nickname": "맛집탐험가",
                                                    "profileImageUrl": "https://example.com/profile.jpg"
                                                  },
                                                  "campaign": {
                                                    "id": 123,
                                                    "title": "이탈리안 레스토랑 신메뉴 체험단",
                                                    "campaignType": "인스타그램"
                                                  }
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403", 
                    description = "권한 없음",
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
            )
    })
    @GetMapping("/campaigns/{campaignId}/submissions")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getCampaignMissions(
        @Parameter(description = "캠페인 ID", required = true, example = "123")
        @PathVariable Long campaignId) {
        
        Long clientId = AuthUtil.getCurrentUserId();
        List<MissionSubmissionResponse> submissions = missionManagementService.getMissionSubmissionsByCampaign(campaignId, clientId);
        return ResponseEntity.ok(BaseResponse.success(submissions, "미션 목록 조회 성공"));
    }

    @Operation(
            operationId = "reviewMissionSubmission",
            summary = "미션 검토",
            description = "제출된 미션을 검토하여 승인하거나 수정을 요청합니다. 캠페인 생성자만 가능합니다.\n" +
                    "검토 상태는 'APPROVED(승인)' 또는 'REVISION_REQUESTED(수정요청)'로 설정할 수 있습니다. 'REVISION_REQUESTED'인 경우, 수정 사유를 포함해야 합니다.\n",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MissionReviewRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "미션 승인",
                                            value = """
                                                    {
                                                      "reviewStatus": "APPROVED",
                                                      "clientFeedback": "미션을 잘 수행해주셨습니다.",
                                                      "clientRating": 5
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "수정 요청",
                                            value = """
                                                    {
                                                      "reviewStatus": "REVISION_REQUESTED",
                                                      "clientFeedback": "수정이 필요한 부분이 있습니다.",
                                                      "clientRating": 3,
                                                      "revisionReason": "제품명이 정확히 표기되지 않았습니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "검토 완료",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "검토 완료",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "미션 검토 완료",
                                              "status": 200,
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403", 
                    description = "권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "404", 
                    description = "미션 제출을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "400", 
                    description = "검토 불가능한 상태",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            )
    })
    @PostMapping("/submissions/{submissionId}/review")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> reviewMission(
        @Parameter(description = "미션 제출 ID", required = true, example = "456")
        @PathVariable Long submissionId,
        @Parameter(description = "미션 검토 요청 정보")
        @RequestBody @Valid MissionReviewRequest request) {
        
        Long clientId = AuthUtil.getCurrentUserId();
        missionManagementService.reviewMissionSubmission(submissionId, request, clientId);
        return ResponseEntity.ok(BaseResponse.success(null, "미션 검토 완료"));
    }

    @Operation(
            operationId = "getUserMissionHistory",
            summary = "유저 미션 이력 조회 (클라이언트용)",
            description = "클라이언트가 특정 유저의 전체 미션 이력을 조회합니다."
                    + "\n\n### 권한 요구사항"
                    + "\n- **CLIENT 권한**을 가진 사용자만 사용 가능"
                    + "\n- 인플루언서 선별을 위한 상세 정보 제공"
                    + "\n\n### 응답 정보"
                    + "\n- **공개/비공개 미션 모두 포함**: 해당유저의 모든 미션 이력 조회"
                    + "\n- **상세 검토 정보**: 검토 상태, 피드백, 평점, 수정 횟수 포함"
                    + "\n- **성과 분석 데이터**: 승인률, 평균 평점 등 분석 가능"
                    + "\n\n### 활용 목적"
                    + "\n- 인플루언서 선별 시 과거 성과 평가"
                    + "\n- 미션 수행 품질 및 신뢰도 확인"
                    + "\n- 협업 이력 및 피드백 검토"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/UserMissionHistorySuccessResponse"),
                            examples = @ExampleObject(
                                    name = "유저 미션 이력",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "유저 미션 이력 조회 성공",
                                              "status": 200,
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "campaignTitle": "이탈리안 레스토랑 신메뉴 체험단",
                                                  "campaignType": "인스타그램",
                                                  "submissionTitle": "맛집 체험 후기 - 이탈리안 레스토랑",
                                                  "submissionUrl": "https://instagram.com/p/abc123",
                                                  "platformType": "인스타그램",
                                                  "submittedAt": "2024-03-15T14:30:00Z",
                                                  "reviewedAt": "2024-03-16T10:30:00Z",
                                                  "reviewStatus": "APPROVED",
                                                  "clientRating": 5,
                                                  "clientFeedback": "미션을 잘 수행해주셨습니다.",
                                                  "revisionCount": 0,
                                                  "isPublic": true
                                                },
                                                {
                                                  "id": 2,
                                                  "campaignTitle": "신상 화장품 체험단",
                                                  "campaignType": "블로그",
                                                  "submissionTitle": "신상 파운데이션 솔직 후기",
                                                  "submissionUrl": "https://blog.naver.com/beauty123/post456",
                                                  "platformType": "블로그",
                                                  "submittedAt": "2024-03-10T14:20:00Z",
                                                  "reviewedAt": "2024-03-12T09:15:00Z",
                                                  "reviewStatus": "REVISION_REQUESTED",
                                                  "clientRating": 3,
                                                  "clientFeedback": "키워드가 누락되었습니다.",
                                                  "revisionCount": 1,
                                                  "isPublic": false
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403", 
                    description = "권한 없음 - CLIENT 권한 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "404", 
                    description = "유저를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            )
    })
    @GetMapping("/users/{userId}/history")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getUserMissionHistory(
        @Parameter(description = "조회할 유저 ID", required = true, example = "789")
        @PathVariable Long userId) {
        
        List<UserMissionHistoryResponse> history = missionManagementService.getMyMissionHistory(userId);
        return ResponseEntity.ok(BaseResponse.success(history, "유저 미션 이력 조회 성공"));
    }

    // ===== 인플루언서용 API =====

    @Operation(
            operationId = "submitMission",
            summary = "미션 제출",
            description = "선정된 캠페인의 미션을 수행한 후 결과를 제출합니다. 선정된 인플루언서만 가능합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MissionSubmissionRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "인스타그램 미션 제출",
                                            value = """
                                                    {
                                                      "submissionUrl": "https://instagram.com/p/Cxy789abc",
                                                      "submissionTitle": "맛집 체험 후기 - 홍대 파스타 맛집 추천",
                                                      "submissionDescription": "홍대에 새로 오픈한 이탈리안 레스토랑 체험 후기입니다.",
                                                      "platformType": "인스타그램"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "블로그 미션 제출",
                                            value = """
                                                    {
                                                      "submissionUrl": "https://blog.naver.com/foodlover123/223456789",
                                                      "submissionTitle": "신상 화장품 한 달 사용 후기",
                                                      "submissionDescription": "신제품 파운데이션을 한 달간 사용해본 솔직한 후기입니다.",
                                                      "platformType": "블로그"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "제출 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MissionSubmissionResponse.class),
                            examples = @ExampleObject(
                                    name = "제출 성공",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "미션 제출 완료",
                                              "status": 200,
                                              "data": {
                                                "id": 456,
                                                "submissionUrl": "https://instagram.com/p/Cxy789abc",
                                                "submissionTitle": "맛집 체험 후기 - 홍대 파스타 맛집 추천",
                                                "submissionDescription": "홍대에 새로 오픈한 이탈리안 레스토랑 체험 후기입니다.",
                                                "platformType": "인스타그램",
                                                "submittedAt": "2024-03-15T14:30:00Z",
                                                "reviewStatus": "PENDING",
                                                "revisionCount": 0
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403", 
                    description = "권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "404", 
                    description = "신청을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "400", 
                    description = "제출 불가능한 상태",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            )
    })
    @PostMapping("/applications/{applicationId}/submit")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<?> submitMission(
        @Parameter(description = "캠페인 신청 ID", required = true, example = "789")
        @PathVariable Long applicationId,
        @Parameter(description = "미션 제출 정보")
        @RequestBody @Valid MissionSubmissionRequest request) {
        
        Long userId = AuthUtil.getCurrentUserId();
        MissionSubmissionResponse response = missionManagementService.submitMission(applicationId, request, userId);
        return ResponseEntity.ok(BaseResponse.success(response, "미션 제출 완료"));
    }

    @Operation(
            operationId = "getMyMissionHistory",
            summary = "내 미션 이력 조회",
            description = "본인의 모든 미션 이력을 조회합니다. 제출한 미션, 검토 상태, 클라이언트 평가가 포함됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserMissionHistoryResponse.class)),
                            examples = @ExampleObject(
                                    name = "내 미션 이력",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "미션 이력 조회 성공",
                                              "status": 200,
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "campaignTitle": "이탈리안 레스토랑 신메뉴 체험단",
                                                  "campaignType": "인스타그램",
                                                  "submissionTitle": "맛집 체험 후기 - 이탈리안 레스토랑",
                                                  "submissionUrl": "https://instagram.com/p/abc123",
                                                  "platformType": "인스타그램",
                                                  "completedAt": "2024-03-16T10:30:00Z",
                                                  "reviewStatus": "APPROVED",
                                                  "clientRating": 5,
                                                  "clientFeedback": "미션을 잘 수행해주셨습니다.",
                                                  "revisionCount": 0,
                                                  "isPublic": true
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401", 
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            )
    })
    @GetMapping("/my/history")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<?> getMyMissionHistory() {
        Long userId = AuthUtil.getCurrentUserId();
        List<UserMissionHistoryResponse> history = missionManagementService.getMyMissionHistory(userId);
        return ResponseEntity.ok(BaseResponse.success(history, "미션 이력 조회 성공"));
    }
}
