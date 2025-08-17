package com.example.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

/**
 * SSE Emitter 관리 서비스
 * SSE 연결 생성, 관리, 메시지 전송을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private final ConcurrentHashMap<String, SseEmitter> sseEmitters;
    private final ScheduledExecutorService sseHeartbeatScheduler;
    private final ExecutorService sseNotificationExecutor;
    private final ObjectMapper objectMapper;

    // SSE 연결 타임아웃 (30분)
    private static final Long DEFAULT_TIMEOUT = 30L * 60L * 1000L;
    
    // 하트비트 전송 간격 (30초)
    private static final long HEARTBEAT_INTERVAL = 30L;

    @PostConstruct
    public void startHeartbeat() {
        // 주기적으로 하트비트 전송하여 연결 유지
        sseHeartbeatScheduler.scheduleAtFixedRate(
            this::sendHeartbeatToAll,
            HEARTBEAT_INTERVAL,
            HEARTBEAT_INTERVAL,
            TimeUnit.SECONDS
        );
        log.info("SSE 하트비트 스케줄러 시작됨 ({}초 간격)", HEARTBEAT_INTERVAL);
    }

    @PreDestroy
    public void cleanup() {
        // 모든 SSE 연결 정리
        sseEmitters.values().forEach(SseEmitter::complete);
        sseEmitters.clear();
        log.info("모든 SSE 연결이 정리되었습니다.");
    }

    /**
     * 새로운 SSE 연결 생성
     */
    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        
        // 기존 연결이 있다면 종료
        SseEmitter oldEmitter = sseEmitters.put(userId, emitter);
        if (oldEmitter != null) {
            oldEmitter.complete();
            log.info("기존 SSE 연결 종료됨: userId={}", userId);
        }

        // 연결 이벤트 핸들러 설정
        emitter.onCompletion(() -> {
            sseEmitters.remove(userId);
            log.info("SSE 연결 완료됨: userId={}", userId);
        });

        emitter.onTimeout(() -> {
            sseEmitters.remove(userId);
            log.info("SSE 연결 타임아웃: userId={}", userId);
        });

        emitter.onError((ex) -> {
            sseEmitters.remove(userId);
            log.error("SSE 연결 오류: userId={}, error={}", userId, ex.getMessage());
        });

        // 연결 확인 메시지 전송 (503 에러 방지)
        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data("SSE 연결이 성공적으로 설정되었습니다.")
                .id(String.valueOf(System.currentTimeMillis())));
            
            log.info("새로운 SSE 연결 생성됨: userId={}, 현재 연결 수={}", userId, sseEmitters.size());
        } catch (IOException e) {
            sseEmitters.remove(userId);
            log.error("SSE 초기 메시지 전송 실패: userId={}, error={}", userId, e.getMessage());
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 특정 사용자에게 알림 메시지 전송 (비동기)
     */
    public void sendNotification(String userId, Object data, String eventName) {
        SseEmitter emitter = sseEmitters.get(userId);
        if (emitter == null) {
            log.debug("SSE 연결이 없음: userId={}", userId);
            return;
        }

        // CompletableFuture로 비동기 처리
        CompletableFuture.runAsync(() -> {
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(jsonData)
                    .id(String.valueOf(System.currentTimeMillis())));
                
                log.info("SSE 알림 전송 성공: userId={}, event={}", userId, eventName);
                
            } catch (IOException e) {
                log.error("SSE 알림 전송 실패: userId={}, event={}, error={}", userId, eventName, e.getMessage());
                // 연결이 끊어진 경우 제거
                sseEmitters.remove(userId);
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("SSE 알림 데이터 직렬화 실패: userId={}, event={}, error={}", userId, eventName, e.getMessage());
            }
        }, sseNotificationExecutor);
    }

    /**
     * 특정 사용자에게 알림 전송 (기본 이벤트명)
     */
    public void sendNotification(String userId, Object data) {
        sendNotification(userId, data, "notification");
    }

    /**
     * 특정 사용자에게 알림 요약 정보 전송
     */
    public void sendNotificationSummary(String userId, Object data) {
        sendNotification(userId, data, "notification-summary");
    }

    /**
     * 모든 연결된 사용자에게 메시지 전송 (브로드캐스트)
     */
    public void sendToAll(Object data, String eventName) {
        if (sseEmitters.isEmpty()) {
            log.debug("전송할 SSE 연결이 없음");
            return;
        }

        try {
            String jsonData = objectMapper.writeValueAsString(data);
            
            sseEmitters.forEach((userId, emitter) -> {
                CompletableFuture.runAsync(() -> {
                    try {
                        emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(jsonData)
                            .id(String.valueOf(System.currentTimeMillis())));
                        
                    } catch (IOException e) {
                        log.error("SSE 브로드캐스트 전송 실패: userId={}, error={}", userId, e.getMessage());
                        sseEmitters.remove(userId);
                        emitter.completeWithError(e);
                    }
                }, sseNotificationExecutor);
            });
            
            log.info("SSE 브로드캐스트 전송 완료: event={}, 대상={}", eventName, sseEmitters.size());
            
        } catch (Exception e) {
            log.error("SSE 브로드캐스트 데이터 직렬화 실패: event={}, error={}", eventName, e.getMessage());
        }
    }

    /**
     * 모든 연결에 하트비트 전송
     */
    private void sendHeartbeatToAll() {
        if (sseEmitters.isEmpty()) {
            return;
        }

        sseEmitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data("ping")
                    .id(String.valueOf(System.currentTimeMillis())));
                
            } catch (IOException e) {
                log.debug("SSE 하트비트 전송 실패 (연결 제거): userId={}", userId);
                sseEmitters.remove(userId);
                emitter.completeWithError(e);
            }
        });

        log.debug("SSE 하트비트 전송 완료: 연결 수={}", sseEmitters.size());
    }

    /**
     * 특정 사용자의 연결 상태 확인
     */
    public boolean isConnected(String userId) {
        return sseEmitters.containsKey(userId);
    }

    /**
     * 현재 연결된 사용자 수 조회
     */
    public int getConnectionCount() {
        return sseEmitters.size();
    }

    /**
     * 특정 사용자의 연결 강제 종료
     */
    public void disconnectUser(String userId) {
        SseEmitter emitter = sseEmitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE 연결 강제 종료됨: userId={}", userId);
        }
    }
}
