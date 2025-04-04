package com.example.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    public void saveRefreshToken(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set("REFRESH:" + userId, refreshToken, Duration.ofDays(7));
    }

    public String getRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get("REFRESH:" + userId);
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete("REFRESH:" + userId);
    }

    public void blacklistAccessToken(String accessToken, long expireMillis) {
        redisTemplate.opsForValue().set("BLACKLIST:" + accessToken, "true", Duration.ofMillis(expireMillis));
    }

    public boolean isBlacklisted(String accessToken) {
        return redisTemplate.hasKey("BLACKLIST:" + accessToken);
    }
}
