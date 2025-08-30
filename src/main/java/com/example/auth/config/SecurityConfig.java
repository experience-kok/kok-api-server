package com.example.auth.config;

import com.example.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * JWT 기반 인증을 위한 보안 설정을 제공합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean
     * BCrypt 해싱 함수를 사용하여 안전한 비밀번호 저장을 제공합니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 설정
     * JWT 기반 인증을 사용하며, 특정 경로는 인증 없이 접근 가능하도록 설정합니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 공개 API - 인증 불필요
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/brands/**").permitAll()
                // 공지사항 및 체험콕 글 조회 - 인증 불필요
                .requestMatchers("GET", "/api/notices/**").permitAll()
                .requestMatchers("GET", "/api/kokposts/**").permitAll()
                // Swagger UI 관련 경로 모두 허용
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/v3/api-docs").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                // 캠페인 진행 상태 조회는 인증 필요 (먼저 설정)
                .requestMatchers("/api/campaigns/status/*/progress").authenticated()
                // GET 캠페인 조회 API는 인증 불필요
                .requestMatchers("GET", "/api/campaigns/**").permitAll()
                .requestMatchers("GET", "/api/v2/campaigns/**").permitAll()
                .requestMatchers("GET", "/api/banners/**").permitAll()
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
