package com.example.auth.util;

import com.example.auth.exception.UnauthorizedException;
import com.example.auth.security.JwtUtil;
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
            Claims claims = jwtUtil.getClaims(token);
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰");
            throw new UnauthorizedException("토큰이 만료되었습니다.");
        } catch (Exception e) {
            log.error("토큰 파싱 중 오류: {}", e.getMessage());
            throw new UnauthorizedException("유효하지 않은 토큰입니다.");
        }
    }
}