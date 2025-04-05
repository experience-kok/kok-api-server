package com.example.auth.service;

import com.example.auth.dto.KakaoTokenResponse;
import com.example.auth.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    private final WebClient webClient;

    // 인가코드로 액세스 토큰 요청
    public KakaoTokenResponse requestToken(String code, String redirectUri) {
        log.debug("카카오 토큰 요청: code={}, redirectUri={}", code, redirectUri);

        try {
            return webClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                            .with("client_id", kakaoClientId)
                            .with("redirect_uri", redirectUri)
                            .with("code", code))
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .block(Duration.ofSeconds(10));
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("카카오 토큰 요청 실패: {} - 응답 코드: {} - 응답 본문: {}",
                    e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("카카오 인증 서버 연결 중 오류가 발생했습니다: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("카카오 토큰 요청 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("카카오 인증 서버 연결 중 오류가 발생했습니다.", e);
        }
    }
    // 액세스 토큰으로 사용자 정보 요청
    public KakaoUserInfo requestUserInfo(String accessToken) {
        log.debug("카카오 사용자 정보 요청");

        try {
            KakaoUserInfo userInfo = webClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfo.class)
                    .block(Duration.ofSeconds(10));

            log.debug("카카오 사용자 정보 응답 성공: id={}", userInfo.id());
            return userInfo;
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 실패: {}", e.getMessage(), e);
            throw new RuntimeException("카카오 사용자 정보 요청 중 오류가 발생했습니다.", e);
        }
    }
}