package com.example.auth.interceptor;

import com.example.auth.exception.TokenErrorType;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.security.JwtUtil;
import com.example.auth.service.TokenService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    // API 패턴을 기반으로 인증이 필요하지 않은 경로들 지정
    private final List<String> publicApis = Arrays.asList(
            "/api/auth/login-redirect",
            "/api/auth/kakao",
            "/api/auth/refresh"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // 공개 API 건너뜀
        if (isPublicApi(path)) {
            return true;
        }

        // API가 아닌 경우 건너뜀
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null) {
            log.warn("인증 토큰이 없습니다: {}", path);
            throw new UnauthorizedException("인증 토큰이 필요합니다.");
        }

        try {
            // 만료 여부 먼저 확인 (ExpiredJwtException 발생 가능)
            jwtUtil.validateToken(token);

            // 만료되지 않았다면 블랙리스트 확인
            if (tokenService.isBlacklisted(token)) {
                log.warn("블랙리스트된 토큰입니다: {}", path);
                throw new UnauthorizedException("로그아웃된 토큰입니다.");
            }

            log.debug("인증 성공: {}", path);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰입니다: {}", path);
            throw new JwtValidationException("토큰이 만료되었습니다.", TokenErrorType.EXPIRED);
        } catch (Exception e) {
            log.error("토큰 검증 중 오류 발생: {}", e.getMessage());
            throw new UnauthorizedException("유효하지 않은 토큰입니다.");
        }
    }
    private boolean isPublicApi(String path) {
        return publicApis.stream().anyMatch(path::startsWith);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }
}