package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * 비밀번호 암호화를 위한 PasswordEncoder Bean과 기본 보안 설정을 제공합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
     * 특정 경로는 인증 없이 접근 가능하도록 설정하고,
     * 나머지는 JWT 기반 인증을 사용합니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // 공개 API - 인증 불필요
                .requestMatchers("/api/brands/**").permitAll()
                .requestMatchers("/api/auth/login-redirect", "/api/auth/kakao", "/api/auth/signup", "/api/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                // 나머지 모든 요청은 현재 애플리케이션 레벨에서 JWT로 처리
                .anyRequest().permitAll()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
        
        return http.build();
    }
}
