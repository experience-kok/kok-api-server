package com.example.auth.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncKakaoService {
    
    private final KakaoService kakaoService;
    
    @Async
    public CompletableFuture<String> processKakaoLoginAsync(String code, String redirectUri) {
        try {
            // 비동기로 카카오 로그인 처리
            var tokenResponse = kakaoService.requestToken(code, redirectUri);
            var userInfo = kakaoService.requestUserInfo(tokenResponse.accessToken());
            
            return CompletableFuture.completedFuture("SUCCESS");
        } catch (Exception e) {
            log.error("비동기 카카오 로그인 처리 중 오류: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
