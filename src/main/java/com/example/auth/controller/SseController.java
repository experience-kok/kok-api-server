package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.exception.TokenErrorType;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.service.SseEmitterService;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE (Server-Sent Events) 연결 컨트롤러
 * 실시간 알림을 위한 SSE 연결을 관리합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Tag(name = "SSE 연결 API", description = "실시간 알림을 위한 SSE 연결 관리 API")
public class SseController {

    private final SseEmitterService sseEmitterService;
    private final TokenUtils tokenUtils;

    @Operation(
        summary = "SSE 연결 생성",
        description = "실시간 알림을 받기 위한 SSE 연결을 생성합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 사용자별 개별 SSE 연결 생성\n" +
                      "- 자동 하트비트를 통한 연결 유지\n" +
                      "- 연결 타임아웃: 30분\n" +
                      "- 기존 연결이 있으면 자동 종료 후 새 연결 생성\n\n" +
                      "### 이벤트 타입\n" +
                      "- **connect**: 연결 성공 확인\n" +
                      "- **notification**: 새로운 알림\n" +
                      "- **notification-summary**: 알림 요약 업데이트\n"+
                      "```"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "SSE 연결 성공",
            content = @Content(
                mediaType = "text/event-stream",
                examples = @ExampleObject(
                    name = "SSE 스트림",
                    summary = "SSE 이벤트 스트림 예시",
                    value = """
                        event: connect
                        data: SSE 연결이 성공적으로 설정되었습니다.
                        id: 170392560
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(
            @Parameter(
                description = "Bearer 토큰 (Authorization 헤더)", 
                required = true,
                example = ""
            )
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            // 토큰 추출 (Authorization 헤더에서)
            String bearerToken = authHeader;
            
            if (bearerToken == null || bearerToken.trim().isEmpty()) {
                log.warn("SSE 연결 시도 - 토큰이 없음");
                return ResponseEntity.status(401).build();
            }

            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("SSE 연결 요청: userId={}", userId);

            SseEmitter emitter = sseEmitterService.createEmitter(userId.toString());

            return ResponseEntity.ok()
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .header("X-Accel-Buffering", "no") // nginx 버퍼링 비활성화
                    .body(emitter);

        } catch (JwtValidationException e) {
            log.warn("SSE 연결 토큰 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (UnauthorizedException e) {
            log.warn("SSE 연결 인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            log.error("SSE 연결 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(
        summary = "SSE 연결 상태 조회",
        description = "현재 사용자의 SSE 연결 상태를 확인합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "연결 상태 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "연결 상태",
                    summary = "SSE 연결 상태 정보",
                    value = """
                        {
                          "success": true,
                          "message": "SSE 연결 상태 조회 성공",
                          "status": 200,
                          "data": {
                            "connected": true,
                            "connectionCount": 42,
                            "userId": "123",
                            "timestamp": "2025-07-29T10:30:00"
                          }
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/status")
    public ResponseEntity<?> getConnectionStatus(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            boolean connected = sseEmitterService.isConnected(userId.toString());
            int totalConnections = sseEmitterService.getConnectionCount();

            ConnectionStatusResponse data = new ConnectionStatusResponse(
                connected, 
                totalConnections, 
                userId.toString(),
                java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(BaseResponse.success(data, "SSE 연결 상태 조회 성공"));

        } catch (JwtValidationException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            String errorCode = e.getErrorType() == TokenErrorType.EXPIRED ? "TOKEN_EXPIRED" : "TOKEN_INVALID";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("SSE 상태 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("SSE 상태 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "SSE 연결 강제 종료",
        description = "현재 사용자의 SSE 연결을 강제로 종료합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "연결 종료 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "연결 종료",
                    summary = "SSE 연결 종료 성공",
                    value = """
                        {
                          "success": true,
                          "message": "SSE 연결이 성공적으로 종료되었습니다",
                          "status": 200,
                          "data": {
                            "message": "SSE 연결이 성공적으로 종료되었습니다.",
                            "userId": "123",
                            "timestamp": "2025-07-29T10:30:00"
                          }
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("SSE 연결 강제 종료 요청: userId={}", userId);

            sseEmitterService.disconnectUser(userId.toString());

            DisconnectResponse data = new DisconnectResponse(
                "SSE 연결이 성공적으로 종료되었습니다.",
                userId.toString(),
                java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(BaseResponse.success(data, "SSE 연결이 성공적으로 종료되었습니다"));

        } catch (JwtValidationException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            String errorCode = e.getErrorType() == TokenErrorType.EXPIRED ? "TOKEN_EXPIRED" : "TOKEN_INVALID";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("SSE 연결 종료 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("SSE 연결 종료 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // === Response DTOs ===

    public record ConnectionStatusResponse(
        boolean connected,
        int connectionCount,
        String userId,
        String timestamp
    ) {}

    public record DisconnectResponse(
        String message,
        String userId,
        String timestamp
    ) {}
}
