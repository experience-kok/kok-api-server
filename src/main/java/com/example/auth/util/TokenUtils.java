package com.example.auth.util;

import com.example.auth.domain.User;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.exception.TokenErrorType;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.security.JwtUtil;
import com.example.auth.service.TokenService;
import com.example.auth.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 토큰 처리 관련 유틸리티 클래스
 * 컨트롤러에서 반복되는 토큰 파싱 로직을 캡슐화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUtils {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final UserService userService;

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출하고 사용자 ID를 반환
     *
     * @param bearerToken Authorization 헤더 값 (Bearer 포함)
     * @return 토큰에서 추출한 사용자 ID
     * @throws UnauthorizedException 토큰이 유효하지 않은 경우
     */
    public Long getUserIdFromToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            log.warn("유효하지 않은 토큰 형식");
            throw new UnauthorizedException("유효하지 않은 토큰 형식입니다.");
        }

        String token = bearerToken.replace("Bearer ", "");

        try {
            // 만료 여부 먼저 확인 (ExpiredJwtException을 catch해서 TOKEN_EXPIRED로 처리)
            Claims claims = jwtUtil.getClaims(token);

            // 블랙리스트 체크는 만료 확인 후에 수행
            if (tokenService.isBlacklisted(token)) {
                log.warn("블랙리스트에 포함된 토큰");
                throw new UnauthorizedException("로그아웃된 토큰입니다.");
            }

            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰");
            throw new JwtValidationException("토큰이 만료되었습니다.", TokenErrorType.EXPIRED);
        } catch (Exception e) {
            log.error("토큰 파싱 중 오류: {}", e.getMessage());
            throw new UnauthorizedException("유효하지 않은 토큰입니다.");
        }
    }
    
    /**
     * Authorization 헤더에서 Bearer 토큰을 추출하고 DB에서 사용자 역할을 조회하여 반환
     *
     * @param bearerToken Authorization 헤더 값 (Bearer 포함)
     * @return DB에서 조회한 사용자 역할
     * @throws UnauthorizedException 토큰이 유효하지 않거나 사용자를 찾을 수 없는 경우
     */
    public String getRoleFromToken(String bearerToken) {
        Long userId = getUserIdFromToken(bearerToken);
        
        // DB에서 사용자 정보 조회
        User user = userService.findUserById(userId);
        if (user == null) {
            log.warn("토큰의 사용자 ID로 사용자를 찾을 수 없음: userId={}", userId);
            throw new UnauthorizedException("존재하지 않는 사용자입니다.");
        }
        
        String role = user.getRole();
        log.debug("DB에서 조회한 사용자 역할: userId={}, role={}", userId, role);
        
        return role;
    }
    
    /**
     * 토큰이 유효한지 확인 (컨트롤러에서 상태코드 확인용)
     *
     * @param bearerToken Authorization 헤더 값 (Bearer 포함)
     * @return 토큰 유효 여부
     */
    public boolean isValidToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return false;
        }

        String token = bearerToken.replace("Bearer ", "");

        // 블랙리스트 체크
        if (tokenService.isBlacklisted(token)) {
            return false;
        }

        try {
            jwtUtil.validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}