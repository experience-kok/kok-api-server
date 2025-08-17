package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.dto.notification.CampaignStatusNotificationRequest;
import com.example.auth.dto.notification.NotificationReadRequest;
import com.example.auth.dto.notification.NotificationResponse;
import com.example.auth.dto.notification.NotificationSummaryResponse;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.exception.TokenErrorType;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.service.NotificationService;
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



@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "실시간 알림 API", description = "사용자 알림 관리 및 실시간 알림 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final TokenUtils tokenUtils;

    @Operation(
        summary = "사용자 알림 목록 조회",
        description = "로그인한 사용자의 알림 목록을 조회합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 알림 목록 페이징 조회\n" +
                      "- 읽음/읽지않음 필터링\n" +
                      "- 최신순 정렬\n\n" +
                      "### 타입 필터\n" +
                      "- **all**: 모든 알림 (기본값)\n" +
                      "- **unread**: 읽지 않은 알림만\n" +
                      "- **read**: 읽은 알림만"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "알림 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "알림 목록",
                    summary = "사용자 알림 목록 조회 성공",
                    value = """
                        {
                          "success": true,
                          "message": "알림 목록 조회 성공",
                          "status": 200,
                          "data": {
                            "content": [
                              {
                                "notificationId": 1,
                                "notificationType": "CAMPAIGN_APPROVED",
                                "notificationTypeDescription": "캠페인 승인",
                                "title": "캠페인이 승인되었습니다",
                                "message": "'신상 음료 체험단 모집' 캠페인이 승인되었습니다. 이제 인플루언서들이 신청할 수 있습니다.",
                                "relatedEntityId": 123,
                                "relatedEntityType": "CAMPAIGN",
                                "isRead": false,
                                "createdAt": "2025-07-29T10:30:00",
                                "readAt": null
                              }
                            ],
                            "pageNumber": 1,
                            "pageSize": 20,
                            "totalPages": 2,
                            "totalElements": 35,
                            "first": true,
                            "last": false
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
            )
        )
    })
    @GetMapping
    public ResponseEntity<?> getUserNotifications(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "알림 타입 필터", example = "all")
            @RequestParam(defaultValue = "all") String type,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1-50)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("사용자 알림 목록 조회 요청: userId={}, type={}, page={}, size={}", userId, type, page, size);

            // 페이지 번호 검증
            if (page < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 번호는 1 이상이어야 합니다.", "INVALID_PAGE", HttpStatus.BAD_REQUEST.value()));
            }

            // 페이지 크기 검증
            if (size < 1 || size > 50) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 크기는 1-50 사이여야 합니다.", "INVALID_PAGE_SIZE", HttpStatus.BAD_REQUEST.value()));
            }

            // 타입 필터 검증
            if (!isValidType(type)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("유효하지 않은 타입 필터입니다. (all, unread, read)", 
                                "INVALID_TYPE", HttpStatus.BAD_REQUEST.value()));
            }

            PageResponse<NotificationResponse> pageResponse = 
                    notificationService.getUserNotifications(userId, type, page - 1, size);

            return ResponseEntity.ok(BaseResponse.success(pageResponse, "알림 목록 조회 성공"));

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
            log.error("알림 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("알림 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "알림 요약 정보 조회",
        description = "사용자의 알림 요약 정보를 조회합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 읽지 않은 알림 개수\n" +
                      "- 알림 기능 활성화 상태\n" +
                      "- 실시간 알림 연결 상태\n\n" +
                      "### 활용 예시\n" +
                      "- 헤더의 알림 아이콘 배지 표시\n" +
                      "- 알림 설정 페이지 상태 표시"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "알림 요약 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "알림 요약",
                    summary = "알림 요약 정보 조회 성공",
                    value = """
                        {
                          "success": true,
                          "message": "알림 요약 조회 성공",
                          "status": 200,
                          "data": {
                            "unreadCount": 5,
                            "notificationEnabled": true,
                            "realtimeConnected": true
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
            )
        )
    })
    @GetMapping("/summary")
    public ResponseEntity<?> getNotificationSummary(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("알림 요약 조회 요청: userId={}", userId);

            NotificationSummaryResponse response = notificationService.getUserNotificationSummary(userId);

            return ResponseEntity.ok(BaseResponse.success(response, "알림 요약 조회 성공"));

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
            log.error("알림 요약 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("알림 요약 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "알림 읽음 처리",
        description = "특정 알림들을 읽음 처리하거나 모든 알림을 읽음 처리합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 특정 알림들만 읽음 처리\n" +
                      "- 모든 알림 일괄 읽음 처리\n" +
                      "- 실시간 알림 요약 업데이트\n\n" +
                      "### 요청 방식\n" +
                      "- **notificationIds**가 있으면 해당 알림들만 처리\n" +
                      "- **notificationIds**가 없으면 모든 알림 처리"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "알림 읽음 처리 성공",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "특정 알림 읽음 처리",
                        summary = "특정 알림들을 읽음 처리한 경우",
                        value = """
                            {
                              "success": true,
                              "message": "알림 읽음 처리가 완료되었습니다",
                              "status": 200,
                              "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "모든 알림 읽음 처리",
                        summary = "모든 알림을 읽음 처리한 경우",
                        value = """
                            {
                              "success": true,
                              "message": "모든 알림 읽음 처리가 완료되었습니다",
                              "status": 200,
                              "data": null
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
            )
        )
    })
    @PostMapping("/read")
    public ResponseEntity<?> markNotificationsAsRead(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Valid @RequestBody NotificationReadRequest request
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("알림 읽음 처리 요청: userId={}, notificationIds={}", userId, request.getNotificationIds());

            notificationService.markNotificationsAsRead(userId, request.getNotificationIds());

            String message = (request.getNotificationIds() == null || request.getNotificationIds().isEmpty()) 
                    ? "모든 알림 읽음 처리가 완료되었습니다"
                    : "알림 읽음 처리가 완료되었습니다";

            return ResponseEntity.ok(BaseResponse.success(null, message));

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
            log.error("알림 읽음 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("알림 읽음 처리 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "특정 알림 상세 조회",
        description = "특정 알림의 상세 정보를 조회합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 알림 상세 정보 조회\n" +
                      "- 본인 알림만 조회 가능\n" +
                      "- 자동 읽음 처리는 하지 않음 (별도 API 호출 필요)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "알림 상세 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "알림 상세",
                    summary = "알림 상세 정보 조회 성공",
                    value = """
                        {
                          "success": true,
                          "message": "알림 조회 성공",
                          "status": 200,
                          "data": {
                            "notificationId": 1,
                            "notificationType": "CAMPAIGN_APPROVED",
                            "notificationTypeDescription": "캠페인 승인",
                            "title": "캠페인이 승인되었습니다",
                            "message": "'신상 음료 체험단 모집' 캠페인이 승인되었습니다. 이제 인플루언서들이 신청할 수 있습니다.",
                            "relatedEntityId": 123,
                            "relatedEntityType": "CAMPAIGN",
                            "isRead": false,
                            "createdAt": "2025-07-29T10:30:00",
                            "readAt": null
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "알림을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
            )
        )
    })
    @GetMapping("/{notificationId}")
    public ResponseEntity<?> getNotification(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "알림 ID", required = true, example = "1")
            @PathVariable Long notificationId
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("알림 상세 조회 요청: userId={}, notificationId={}", userId, notificationId);

            NotificationResponse response = notificationService.getNotification(userId, notificationId);

            return ResponseEntity.ok(BaseResponse.success(response, "알림 조회 성공"));

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
            log.warn("알림 조회 실패 (리소스 없음): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("알림 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("알림 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 상태 알림 전송 (관리자 전용)",
        description = "관리자 프로젝트에서 캠페인 승인/거절 시 호출하는 API입니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 캠페인 승인/거절 알림 생성\n" +
                      "- 실시간 알림 전송\n" +
                      "- 관리자 프로젝트에서만 호출\n\n" +
                      "### 승인 상태\n" +
                      "- **APPROVED**: 캠페인 승인\n" +
                      "- **REJECTED**: 캠페인 거절"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "캠페인 상태 알림 전송 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "알림 전송 성공",
                    summary = "캠페인 상태 알림 전송 성공",
                    value = """
                        {
                          "success": true,
                          "message": "캠페인 상태 알림이 전송되었습니다",
                          "status": 200,
                          "data": null
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/campaign-status")
    public ResponseEntity<?> sendCampaignStatusNotification(
            @Valid @RequestBody CampaignStatusNotificationRequest request
    ) {
        try {
            log.info("캠페인 상태 알림 전송 요청: userId={}, campaignId={}, status={}", 
                    request.getUserId(), request.getCampaignId(), request.getApprovalStatus());

            notificationService.sendCampaignStatusNotification(request);

            return ResponseEntity.ok(BaseResponse.success(null, "캠페인 상태 알림이 전송되었습니다"));

        } catch (Exception e) {
            log.error("캠페인 상태 알림 전송 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 상태 알림 전송 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 선정 알림 테스트 전송",
        description = "캠페인 선정 알림을 테스트로 전송합니다. (개발/테스트 환경용)"
    )
    @PostMapping("/test/campaign-selection")
    public ResponseEntity<?> sendTestCampaignSelectionNotification(
            @Parameter(description = "사용자 ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "캠페인 ID", required = true)
            @RequestParam Long campaignId,
            @Parameter(description = "캠페인 제목", required = true)
            @RequestParam String campaignTitle,
            @Parameter(description = "추가 메시지", required = false)
            @RequestParam(required = false) String additionalMessage
    ) {
        try {
            log.info("캠페인 선정 알림 테스트 전송: userId={}, campaignId={}, campaignTitle={}", 
                    userId, campaignId, campaignTitle);

            notificationService.sendCampaignSelectionNotification(userId, campaignId, campaignTitle, additionalMessage);

            return ResponseEntity.ok(BaseResponse.success(null, "캠페인 선정 테스트 알림이 전송되었습니다"));

        } catch (Exception e) {
            log.error("캠페인 선정 테스트 알림 전송 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 선정 테스트 알림 전송 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "캠페인 미선정 알림 테스트 전송",
        description = "캠페인 미선정 알림을 테스트로 전송합니다. (개발/테스트 환경용)"
    )
    @PostMapping("/test/campaign-not-selected")
    public ResponseEntity<?> sendTestCampaignNotSelectedNotification(
            @Parameter(description = "사용자 ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "캠페인 ID", required = true)
            @RequestParam Long campaignId,
            @Parameter(description = "캠페인 제목", required = true)
            @RequestParam String campaignTitle,
            @Parameter(description = "추가 메시지", required = false)
            @RequestParam(required = false) String additionalMessage
    ) {
        try {
            log.info("캠페인 미선정 알림 테스트 전송: userId={}, campaignId={}, campaignTitle={}", 
                    userId, campaignId, campaignTitle);

            notificationService.sendCampaignNotSelectedNotification(userId, campaignId, campaignTitle, additionalMessage);

            return ResponseEntity.ok(BaseResponse.success(null, "캠페인 미선정 테스트 알림이 전송되었습니다"));

        } catch (Exception e) {
            log.error("캠페인 미선정 테스트 알림 전송 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("캠페인 미선정 테스트 알림 전송 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // === Private Helper Methods ===


    private boolean isValidType(String type) {
        return type != null && (
                type.equals("all") || 
                type.equals("unread") || 
                type.equals("read")
        );
    }
}
