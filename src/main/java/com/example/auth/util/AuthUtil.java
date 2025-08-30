package com.example.auth.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 인증 관련 유틸리티 클래스
 */
public class AuthUtil {

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     * 
     * @return 현재 사용자 ID
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Object principal = authentication.getPrincipal();
        
        // JWT에서 사용자 ID 추출
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("유효하지 않은 사용자 ID 형식입니다.");
            }
        }
        
        // UserDetails 구현체에서 사용자명(ID) 추출
        if (principal instanceof UserDetails) {
            try {
                return Long.parseLong(((UserDetails) principal).getUsername());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("유효하지 않은 사용자 ID 형식입니다.");
            }
        }
        
        throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
    }

    /**
     * 현재 인증된 사용자의 정보를 반환합니다.
     * 
     * @return Authentication 객체
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 현재 사용자가 인증되어 있는지 확인합니다.
     * 
     * @return 인증 여부
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * 현재 사용자가 특정 권한을 가지고 있는지 확인합니다.
     * 
     * @param authority 확인할 권한
     * @return 권한 보유 여부
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = getCurrentAuthentication();
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    /**
     * 현재 사용자가 특정 역할을 가지고 있는지 확인합니다.
     * 
     * @param role 확인할 역할 (ROLE_ 접두사 없이)
     * @return 역할 보유 여부
     */
    public static boolean hasRole(String role) {
        return hasAuthority("ROLE_" + role);
    }
}
