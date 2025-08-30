package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.mission.*;
import com.example.auth.service.MissionManagementService;
import com.example.auth.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
@Tag(name = "미션 관리 API", description = "인플루언서 미션 제출, 검토, 포트폴리오 관리를 위한 API")
public class MissionController {

    private final MissionManagementService missionManagementService;

    // ===== 클라이언트용 API =====


    @Operation(
            operationId = "getCampaignMissions",
            summary = "캠페인 미션 제출 목록 조회",
            description = "클라이언트가 해당 캠페인의 제출된 모든 미션을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MissionSubmissionResponse.class),
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
                                                  "user": {
                                                    "id": 45,
                                                    "nickname": "맛집탐험가",
                                                    "gender": "MALE",
                                                    "profileImage": "https://example.com/profile.jpg"
                                                  },
                                                  "campaign": {
                                                    "id": 123,
                                                    "title": "이탈리안 레스토랑 신메뉴 체험단",
                                                    "campaignType": "인스타그램"
                                                  },
                                                  "mission": {
                                                    "missionUrl": "https://instagram.com/p/abc123",
                                                    "submittedAt": "2024-03-15T14:30:00Z",
                                                    "reviewedAt": "2024-03-16T10:30:00Z",
                                                    "clientFeedback": "미션을 잘 수행해주셨습니다."
                                                  }
                                                },
                                                {
                                                  "id": 2,
                                                  "user": {
                                                    "id": 67,
                                                    "nickname": "뷰티리뷰어",
                                                    "gender": "FEMALE",
                                                    "profileImage": "https://example.com/profile2.jpg"
                                                  },
                                                  "campaign": {
                                                    "id": 123,
                                                    "title": "이탈리안 레스토랑 신메뉴 체험단",
                                                    "campaignType": "인스타그램"
                                                  },
                                                  "mission": {
                                                    "missionUrl": "https://blog.naver.com/foodlover/123",
                                                    "submittedAt": "2024-03-16T09:15:00Z",
                                                    "reviewedAt": null,
                                                    "clientFeedback": null
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
    @GetMapping("/campaigns/{campaignId}/missions")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getCampaignMissions(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "캠페인 ID", required = true, example = "123")
            @PathVariable Long campaignId) {

        Long clientId = AuthUtil.getCurrentUserId();
        List<MissionSubmissionResponse> submissions = missionManagementService.getMissionSubmissionsByCampaign(campaignId, clientId);
        return ResponseEntity.ok(BaseResponse.success(submissions, "미션 목록 조회 성공"));
    }

    @Operation(
            operationId = "reviewMissionSubmission",
            summary = "미션 검토",
            description = """
                    제출된 미션을 검토하여 승인하거나 수정을 요청합니다. 캠페인 생성자만 가능합니다.
                    승인시 revisionReason 없이, 수정 요청시 revisionReason 포함하여 요청하세요.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MissionReviewRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "미션 승인",
                                            value = """
                                                    {
                                                      "clientFeedback": "미션을 잘 수행해주셨습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "수정 요청",
                                            value = """
                                                    {
                                                      "clientFeedback": "수정이 필요한 부분이 있습니다.",
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
    @PostMapping("/{missionId}/review")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> reviewMission(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "미션 제출 ID", required = true, example = "456")
            @PathVariable Long missionId,
            @Parameter(description = "미션 검토 요청 정보")
            @RequestBody @Valid MissionReviewRequest request) {

        Long clientId = AuthUtil.getCurrentUserId();
        missionManagementService.reviewMissionSubmission(missionId, request, clientId);
        return ResponseEntity.ok(BaseResponse.success(null, "미션 검토 완료"));
    }

    @Operation(
            operationId = "getUserMissionHistory",
            summary = "유저 미션 이력 조회 (클라이언트용)",
            description = """
                    클라이언트가 특정 유저의 전체 미션 이력을 조회합니다.
                    
                    ### 권한 요구사항
                    - **CLIENT 권한**을 가진 사용자만 사용 가능
                    - 인플루언서 선별을 위한 상세 정보 제공
                    
                    ### 응답 정보
                    - **공개/비공개 미션 모두 포함**: 해당유저의 모든 미션 이력 조회
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.example.auth.dto.mission.ClientUserMissionHistoryDataWrapper.class),
                            examples = @ExampleObject(
                                    name = "유저 미션 이력",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "유저 미션 이력 조회 성공",
                                              "status": 200,
                                              "data": {
                                                "histories": [
                                                  {
                                                    "id": 1,
                                                    "campaign": {
                                                      "title": "이탈리안 레스토랑 신메뉴 체험단",
                                                      "category": "맛집"
                                                    },
                                                    "mission": {
                                                      "missionUrl": "https://instagram.com/p/abc123",
                                                      "completionDate": "2024-03-16T10:30:00Z",
                                                      "isCompleted": true
                                                    }
                                                  },
                                                  {
                                                    "id": 2,
                                                    "campaign": {
                                                      "title": "신상 화장품 체험단",
                                                      "category": "화장품"
                                                    },
                                                    "mission": {
                                                      "missionUrl": "https://blog.naver.com/beauty123/post456",
                                                      "completionDate": null,
                                                      "isCompleted": false
                                                    }
                                                  }
                                                ]
                                              }
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
    @GetMapping("/{userId}/history")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getUserMissionHistory(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "조회할 유저 ID", required = true, example = "789")
            @PathVariable Long userId) {

        List<ClientUserMissionHistoryResponse> history = missionManagementService.getClientUserMissionHistory(userId);
        ClientUserMissionHistoryDataWrapper responseData = ClientUserMissionHistoryDataWrapper.of(history);
        return ResponseEntity.ok(BaseResponse.success(responseData, "유저 미션 이력 조회 성공"));
    }

    // ===== 인플루언서용 API =====

    @Operation(
            operationId = "submitMission",
            summary = "미션 제출",
            description = "선정된 캠페인의 미션을 수행한 후 결과를 제출합니다. 선정된 인플루언서만 가능합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MissionSubmissionRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "인스타그램 미션 제출",
                                            value = """
                                                    {
                                                      "missionUrl": "https://instagram.com/p/Cxy789abc"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "블로그 미션 제출",
                                            value = """
                                                    {
                                                      "missionUrl": "https://blog.naver.com/foodlover123/223456789"
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
                            examples = @ExampleObject(
                                    name = "제출 성공",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "미션 제출 완료",
                                              "status": 200,
                                              "data": {
                                                "id": 456,
                                                "mission": {
                                                  "missionUrl": "https://instagram.com/p/Cxy789abc",
                                                  "submittedAt": "2024-03-15T14:30:00Z",
                                                  "clientFeedback": null
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 - INFLUENCER 권한 필요",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "해당 캠페인에 선정되지 않았습니다.",
                                              "status": 403,
                                              "errorCode": "ACCESS_DENIED",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "신청을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "캠페인 신청을 찾을 수 없습니다.",
                                              "status": 404,
                                              "errorCode": "NOT_FOUND",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "제출 불가능한 상태",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "미션 제출 기간이 아닙니다.",
                                              "status": 400,
                                              "errorCode": "INVALID_SUBMISSION_PERIOD",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/{applicationId}/submit")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<?> submitMission(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "캠페인 신청 ID", required = true, example = "789")
            @PathVariable Long applicationId,
            @Parameter(description = "미션 제출 정보")
            @RequestBody @Valid MissionSubmissionRequest request) {

        Long userId = AuthUtil.getCurrentUserId();
        InfluencerMissionSubmissionResponse response = missionManagementService.submitMission(applicationId, request, userId);
        return ResponseEntity.ok(BaseResponse.success(response, "미션 제출 완료"));
    }

    @Operation(
            operationId = "getMyMissionHistory",
            summary = "내 미션 이력 조회",
            description = """
                    본인의 모든 미션 이력을 조회합니다. 
                    - **완료된 미션**: 제출한 미션, 검토 상태, 클라이언트 평가 포함 (revisionReason: null)
                    - **수정 요청 받은 미션**: 수정 요청 사유 포함 (revisionReason: "사유")
                    - **검토 대기 중인 미션**: 아직 검토되지 않은 미션들 포함
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.example.auth.dto.mission.UserMissionHistoryDataWrapper.class),
                            examples = @ExampleObject(
                                    name = "내 미션 이력",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "미션 이력 조회 성공",
                                              "status": 200,
                                              "data": {
                                                "histories": [
                                                  {
                                                    "id": 1,
                                                    "campaign": {
                                                      "title": "이탈리안 레스토랑 신메뉴 체험단",
                                                      "category": "맛집"
                                                    },
                                                    "mission": {
                                                      "missionUrl": "https://instagram.com/p/abc123",
                                                      "isCompleted": true,
                                                      "completionDate": "2024-03-16T10:30:00Z",
                                                      "clientReview": "미션을 잘 수행해주셨습니다!",
                                                      "revisionReason": null
                                                    }
                                                  },
                                                  {
                                                    "id": 2,
                                                    "campaign": {
                                                      "title": "신상 화장품 체험단",
                                                      "category": "화장품"
                                                    },
                                                    "mission": {
                                                      "missionUrl": "https://blog.naver.com/beauty123/post456",
                                                      "isCompleted": true,
                                                      "completionDate": "2024-03-15T14:20:00Z",
                                                      "clientReview": "상세한 리뷰 감사합니다.",
                                                      "revisionReason": null
                                                    }
                                                  },
                                                  {
                                                    "id": 3,
                                                    "campaign": {
                                                      "title": "카페 체험단",
                                                      "category": "카페"
                                                    },
                                                    "mission": {
                                                      "missionUrl": "https://instagram.com/p/def456",
                                                      "isCompleted": false,
                                                      "completionDate": null,
                                                      "clientReview": "수정이 필요합니다.",
                                                      "revisionReason": "제품명이 정확히 표기되지 않았습니다."
                                                    }
                                                  }
                                                ]
                                              }
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
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "인증이 필요합니다.",
                                              "status": 401,
                                              "errorCode": "UNAUTHORIZED",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 - INFLUENCER 권한 필요",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "INFLUENCER 권한이 필요합니다.",
                                              "status": 403,
                                              "errorCode": "ACCESS_DENIED",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/my/history")
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<?> getMyMissionHistory(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken) {

        Long userId = AuthUtil.getCurrentUserId();
        List<UserMissionHistoryResponse> history = missionManagementService.getMyMissionHistory(userId);
        UserMissionHistoryDataWrapper responseData = UserMissionHistoryDataWrapper.of(history);
        return ResponseEntity.ok(BaseResponse.success(responseData, "미션 이력 조회 성공"));
    }

}
