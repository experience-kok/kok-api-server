package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.campaign.CampaignProgressResponse;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.service.CampaignViewService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/campaigns/status")
@RequiredArgsConstructor
@Tag(name = "캠페인 상태 API", description = "캠페인 진행 상태 및 통계 정보를 제공하는 API")
public class CampaignStatusController {

    private final CampaignViewService campaignViewService;

    @Operation(
            summary = "캠페인 진행 상태 조회",
            description = "캠페인의 현재 진행 상태를 확인할 수 있습니다."
                    + "\n\n### 진행 단계:"
                    + "\n- **모집중(RECRUITING)**: 현재 지원자를 모집하고 있는 상태 (모집 마감일 이전)"
                    + "\n- **지원자 모집 완료(RECRUITMENT_COMPLETED)**: 모집 마감 후 선정 대기 중"
                    + "\n- **참가자 선정 완료(SELECTION_COMPLETED)**: 인플루언서 선정 완료, 미션 시작 대기"
                    + "\n- **미션 진행중(MISSION_IN_PROGRESS)**: 선정된 인플루언서들이 미션 수행 중"
                    + "\n- **콘텐츠 검토 대기(CONTENT_REVIEW_PENDING)**: 미션 제출 완료, 클라이언트 검토 필요"
                    + "\n\n### 상시 캠페인(ALWAYS_OPEN):"
                    + "\n- 상시 캠페인의 경우 '상시 참여 가능한 캠페인이에요' 메시지 반환",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/CampaignProgressSuccessResponse"),
                            examples = {
                                    @ExampleObject(
                                            name = "모집중",
                                            summary = "현재 지원자를 모집하고 있는 상태",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "캠페인 진행 상태 조회 성공",
                                                      "status": 200,
                                                      "data": {
                                                        "campaignId": 100,
                                                        "campaignTitle": "신상 카페 음료 체험단",
                                                        "isAlwaysOpen": false,
                                                        "progress": {
                                                          "status": "RECRUITING",
                                                          "message": "지원자를 모집하고 있어요."
                                                        }
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "지원자 모집 완료",
                                            summary = "신청자의 지원자 모집 완료 상태 조회",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "캠페인 진행 상태 조회 성공",
                                                      "status": 200,
                                                      "data": {
                                                        "campaignId": 456,
                                                        "campaignTitle": "신상 화장품 체험단",
                                                        "isAlwaysOpen": false,
                                                        "progress": {
                                                          "status": "RECRUITMENT_COMPLETED",
                                                          "message": "참가자 선정 결과를 기다려주세요."
                                                        }
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "미션 진행중",
                                            summary = "캠페인 소유자의 미션 진행중 상태 조회",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "캠페인 진행 상태 조회 성공",
                                                      "status": 200,
                                                      "data": {
                                                        "campaignId": 123,
                                                        "campaignTitle": "이탈리안 레스토랑 신메뉴 체험단",
                                                        "isAlwaysOpen": false,
                                                        "progress": {
                                                          "status": "MISSION_IN_PROGRESS",
                                                          "message": "참가자들이 미션을 수행 중이에요."
                                                        }
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "상시 캠페인",
                                            summary = "상시 캠페인의 응답 예시",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "캠페인 진행 상태 조회 성공",
                                                      "status": 200,
                                                      "data": {
                                                        "campaignId": 789,
                                                        "campaignTitle": "상시 모집 카페 체험단",
                                                        "isAlwaysOpen": true,
                                                        "progress": {
                                                          "status": "ALWAYS_OPEN",
                                                          "message": "상시 참여 가능한 캠페인이에요."
                                                        }
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "콘텐츠 검토 대기",
                                            summary = "콘텐츠 검토 대기 상태 조회",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "캠페인 진행 상태 조회 성공",
                                                      "status": 200,
                                                      "data": {
                                                        "campaignId": 321,
                                                        "campaignTitle": "홈카페 원두 체험단",
                                                        "isAlwaysOpen": false,
                                                        "progress": {
                                                          "status": "CONTENT_REVIEW_PENDING",
                                                          "message": "제출된 콘텐츠를 검토 중이에요."
                                                        }
                                                      }
                                                    }
                                                    """
                                    )
                            })),
            @ApiResponse(responseCode = "403", description = "권한 없음 - 신청하지 않은 캠페인 조회 시도",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = @ExampleObject(
                                    name = "접근 거부",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "신청한 캠페인의 진행 상태만 조회할 수 있습니다.",
                                              "status": 403,
                                              "errorCode": "ACCESS_DENIED"
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = @ExampleObject(
                                    name = "캠페인 없음",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "캠페인을 찾을 수 없습니다.",
                                              "status": 404,
                                              "errorCode": "NOT_FOUND"
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = @ExampleObject(
                                    name = "서버 오류",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "캠페인 진행 상태 조회 중 오류가 발생했습니다.",
                                              "status": 500,
                                              "errorCode": "INTERNAL_ERROR"
                                            }
                                            """
                            )))
    })
    @GetMapping("/{campaignId}/progress")
    public ResponseEntity<?> getCampaignProgressStatus(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "캠페인 ID", required = true, example = "123")
            @PathVariable Long campaignId
    ) {
        try {
            // 인증 정보 확인
            if (!AuthUtil.isAuthenticated()) {
                log.warn("인증되지 않은 사용자의 캠페인 진행 상태 조회 시도 - campaignId: {}", campaignId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
            }

            Long currentUserId = AuthUtil.getCurrentUserId();
            log.info("캠페인 진행 상태 조회 요청 - campaignId: {}, userId: {}", campaignId, currentUserId);

            CampaignProgressResponse response = campaignViewService.getCampaignProgressStatus(campaignId, currentUserId);
            return ResponseEntity.ok(BaseResponse.success(response, "캠페인 진행 상태 조회 성공"));

        } catch (IllegalStateException e) {
            // AuthUtil에서 발생하는 인증 관련 예외 처리
            log.error("인증 정보 처리 오류 - campaignId: {}, 오류: {}", campaignId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("로그인이 필요합니다.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        } catch (com.example.auth.exception.AccessDeniedException e) {
            log.error("캠페인 진행 상태 조회 권한 없음 - campaignId: {}, 사유: {}", campaignId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "ACCESS_DENIED", HttpStatus.FORBIDDEN.value()));
        } catch (ResourceNotFoundException e) {
            log.error("캠페인 진행 상태 조회 실패 - campaignId: {}, 사유: {}", campaignId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("캠페인 진행 상태 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 진행 상태 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}