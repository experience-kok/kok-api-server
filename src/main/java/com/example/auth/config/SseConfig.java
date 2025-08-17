package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ExecutorService;

/**
 * SSE (Server-Sent Events) 설정
 * 실시간 알림을 위한 SSE 연결을 구성합니다.
 */
@Configuration
public class SseConfig {

    /**
     * SSE 연결 저장소
     * userId -> SseEmitter 매핑
     * Thread-Safe한 ConcurrentHashMap 사용
     */
    @Bean
    public ConcurrentHashMap<String, SseEmitter> sseEmitters() {
        return new ConcurrentHashMap<>();
    }

    /**
     * SSE 하트비트 전송을 위한 스케줄러
     * 연결 유지를 위해 주기적으로 하트비트 전송
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService sseHeartbeatScheduler() {
        return Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "sse-heartbeat-");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * SSE 알림 전송을 위한 ExecutorService
     * 비동기 알림 전송 처리
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService sseNotificationExecutor() {
        return Executors.newFixedThreadPool(5, r -> {
            Thread thread = new Thread(r, "sse-notification-");
            thread.setDaemon(true);
            return thread;
        });
    }
}
