package com.example.auth.service;

/**
 * SNS 플랫폼 연동을 위한 공통 인터페이스
 */
public interface SnsConnectService {
    /**
     * 사용자 ID와 계정 URL을 받아 SNS 플랫폼 연동
     * @param userId 사용자 ID
     * @param url 계정 URL
     * @return 연동된 플랫폼 ID
     */
    Long connect(Long userId, String url);
    
    /**
     * 연동된 SNS 플랫폼 해제
     * @param userId 사용자 ID
     * @param platformId 플랫폼 ID
     */
    void disconnect(Long userId, Long platformId);
    
    /**
     * URL 유효성 검사
     * @param url 계정 URL
     * @return 유효한 URL 여부
     */
    boolean isValidUrl(String url);
}
