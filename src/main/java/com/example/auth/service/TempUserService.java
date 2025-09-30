package com.example.auth.service;

import com.example.auth.dto.TempUserData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TempUserService {

    private static final String TEMP_USER_PREFIX = "temp_user:";
    private static final long TEMP_USER_TTL_MINUTES = 10; // 10분

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * 임시 사용자 데이터를 Redis에 저장
     * @param tempUserData 임시 사용자 데이터
     * @return 저장된 임시 사용자 ID (UUID)
     */
    public String saveTempUser(TempUserData tempUserData) {
        String tempUserId = UUID.randomUUID().toString();
        tempUserData.setTempUserId(tempUserId);
        
        try {
            String key = TEMP_USER_PREFIX + tempUserId;
            String jsonValue = objectMapper.writeValueAsString(tempUserData);
            
            redisTemplate.opsForValue().set(key, jsonValue, TEMP_USER_TTL_MINUTES, TimeUnit.MINUTES);
            
            log.info("임시 사용자 Redis 저장 완료: tempUserId={}, socialId={}, TTL={}분", 
                    tempUserId, tempUserData.getSocialId(), TEMP_USER_TTL_MINUTES);
            
            return tempUserId;
        } catch (JsonProcessingException e) {
            log.error("임시 사용자 데이터 JSON 변환 실패: {}", e.getMessage(), e);
            throw new RuntimeException("임시 사용자 저장 실패", e);
        }
    }

    /**
     * Redis에서 임시 사용자 데이터 조회
     * @param tempUserId 임시 사용자 ID
     * @return 임시 사용자 데이터 (없으면 null)
     */
    public TempUserData getTempUser(String tempUserId) {
        try {
            String key = TEMP_USER_PREFIX + tempUserId;
            String jsonValue = redisTemplate.opsForValue().get(key);
            
            if (jsonValue == null) {
                log.warn("임시 사용자 데이터 없음 (만료됨): tempUserId={}", tempUserId);
                return null;
            }
            
            TempUserData tempUserData = objectMapper.readValue(jsonValue, TempUserData.class);
            log.info("임시 사용자 Redis 조회 성공: tempUserId={}, socialId={}", 
                    tempUserId, tempUserData.getSocialId());
            
            return tempUserData;
        } catch (JsonProcessingException e) {
            log.error("임시 사용자 데이터 JSON 파싱 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Redis에서 임시 사용자 데이터 삭제
     * @param tempUserId 임시 사용자 ID
     */
    public void deleteTempUser(String tempUserId) {
        String key = TEMP_USER_PREFIX + tempUserId;
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.info("임시 사용자 Redis 삭제 완료: tempUserId={}", tempUserId);
        } else {
            log.warn("임시 사용자 Redis 삭제 실패 (이미 없음): tempUserId={}", tempUserId);
        }
    }

    /**
     * 임시 사용자 데이터 존재 여부 확인
     * @param tempUserId 임시 사용자 ID
     * @return 존재 여부
     */
    public boolean existsTempUser(String tempUserId) {
        String key = TEMP_USER_PREFIX + tempUserId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
