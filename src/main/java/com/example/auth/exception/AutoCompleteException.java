package com.example.auth.exception;

/**
 * 자동완성 기능 관련 예외
 */
public class AutoCompleteException extends RuntimeException {
    
    public AutoCompleteException(String message) {
        super(message);
    }
    
    public AutoCompleteException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Redis 연결 오류
     */
    public static class RedisConnectionException extends AutoCompleteException {
        public RedisConnectionException(String message, Throwable cause) {
            super("Redis 연결 오류: " + message, cause);
        }
    }
    
    /**
     * 캐시 데이터 없음
     */
    public static class CacheEmptyException extends AutoCompleteException {
        public CacheEmptyException(String cacheKey) {
            super("캐시 데이터가 없습니다: " + cacheKey);
        }
    }
    
    /**
     * 캐시 갱신 실패
     */
    public static class CacheRefreshException extends AutoCompleteException {
        public CacheRefreshException(String message, Throwable cause) {
            super("캐시 갱신 실패: " + message, cause);
        }
    }
}
