package com.example.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            if (request.getRequestURI().startsWith("/api/")) {
                // API 응답인 경우만 로깅
                int status = responseWrapper.getStatus();
                if (status >= 400) { // 오류 응답만 로깅
                    byte[] content = responseWrapper.getContentAsByteArray();
                    String responseBody = new String(content, StandardCharsets.UTF_8);
                    log.debug("응답 상태 코드: {} - 응답 본문: {}", status, responseBody);
                }
            }
            responseWrapper.copyBodyToResponse();
        }
    }
}