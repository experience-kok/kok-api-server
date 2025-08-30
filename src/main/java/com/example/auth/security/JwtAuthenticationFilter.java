package com.example.auth.security;

import com.example.auth.constant.UserRole;
import com.example.auth.domain.User;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JWT 인증 필터
 * HTTP 요청에서 JWT 토큰을 추출하여 인증 정보를 SecurityContext에 설정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    // 인증이 필요하지 않은 공개 API 경로들
    private final List<String> publicPaths = Arrays.asList(
            "/api/auth/",
            "/api/brands/",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/v3/api-docs/",
            "/v3/api-docs",
            "/api-docs/",
            "/swagger-resources/",
            "/webjars/",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 공개 API는 인증 없이 통과
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // GET 요청이며 캠페인 조회 API인 경우 인증 없이 허용 (단, 진행 상태 조회는 제외)
        if ("GET".equals(request.getMethod()) &&
                (path.startsWith("/api/campaigns") || path.startsWith("/api/v2/campaigns")) &&
                !path.matches(".*\\/status\\/\\d+\\/progress$")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (token != null) {
            try {
                // 토큰 검증
                Claims claims = jwtUtil.validateToken(token);

                // 블랙리스트 확인
                if (tokenService.isBlacklisted(token)) {
                    log.warn("블랙리스트된 토큰으로 요청: {}", path);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 사용자 ID 추출
                String userIdStr = claims.getSubject();
                Long userId = Long.parseLong(userIdStr);

                // 사용자 정보 조회하여 실제 권한 설정
                Optional<User> userOptional = userRepository.findById(userId);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    String roleString = user.getRole();
                    UserRole userRole = UserRole.fromString(roleString);
                    
                    // Spring Security 권한 형태로 변환
                    String authority = "ROLE_" + userRole.name();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userIdStr,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority(authority))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 인증 성공 - userId: {}, role: {}, path: {}", userId, userRole, path);
                } else {
                    log.warn("존재하지 않는 사용자 ID: {}", userId);
                    SecurityContextHolder.clearContext();
                }

            } catch (JwtValidationException e) {
                log.warn("JWT 검증 실패 - path: {}, error: {}", path, e.getMessage());
                SecurityContextHolder.clearContext();
                
                // TokenErrorType에 따른 에러 코드 결정
                String errorCode = switch (e.getErrorType()) {
                    case EXPIRED -> "TOKEN_EXPIRED";
                    case INVALID -> "TOKEN_INVALID";
                    case REFRESH_INVALID -> "TOKEN_INVALID";
                    case UNKNOWN -> "TOKEN_INVALID";
                };
                
                // BaseResponse 패턴에 맞는 401 응답
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json; charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(String.format(
                    "{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\",\"status\":401}",
                    e.getMessage(), errorCode
                ));
                return;
            } catch (Exception e) {
                log.error("JWT 처리 중 예상치 못한 오류 - path: {}, error: {}", path, e.getMessage());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json; charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"인증 처리 중 오류가 발생했습니다.\",\"errorCode\":\"TOKEN_INVALID\",\"status\":401}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 공개 경로인지 확인
     */
    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(path::startsWith);
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
