package com.example.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCodeCacheService {
    
    private final StringRedisTemplate redisTemplate;
    private static final String AUTH_CODE_PREFIX = "kakao_auth_code:";
    
    public boolean isAuthCodeUsed(String authCode) {
        String key = AUTH_CODE_PREFIX + authCode;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    public void markAuthCodeAsUsed(String authCode) {
        String key = AUTH_CODE_PREFIX + authCode;
        // 인가 코드는 10분간 유효하므로 15분간 캐시
        redisTemplate.opsForValue().set(key, "used", Duration.ofMinutes(15));
        log.debug("인가 코드 사용 처리 완료: {}", authCode);
    }
}
