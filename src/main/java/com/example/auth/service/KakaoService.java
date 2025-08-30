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
    
    @Value("${kakao.timeout:75000}")
    private long kakaoTimeoutMs;

    private final WebClient kakaoWebClient; // 카카오 전용 WebClient 사용

    // 인가코드로 액세스 토큰 요청
    public KakaoTokenResponse requestToken(String code, String redirectUri) {
        log.debug("카카오 토큰 요청: code={}, redirectUri={}", code, redirectUri);

        try {
            return kakaoWebClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                            .with("client_id", kakaoClientId)
                            .with("redirect_uri", redirectUri)
                            .with("code", code))
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .timeout(Duration.ofMillis(kakaoTimeoutMs)) // 설정값 사용
                    .retryWhen(reactor.util.retry.Retry.fixedDelay(2, Duration.ofSeconds(1))
                        .filter(throwable -> {
                            // 네트워크 오류나 5xx 에러에만 재시도
                            if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                var webClientException = (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                return webClientException.getStatusCode().is5xxServerError();
                            }
                            return throwable instanceof java.net.ConnectException || 
                                   throwable instanceof java.util.concurrent.TimeoutException ||
                                   throwable instanceof java.io.IOException;
                        })
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            Throwable failure = retrySignal.failure();
                            log.error("카카오 토큰 요청 재시도 횟수 초과: {}", failure.getMessage());
                            return new RuntimeException("카카오 서버 연결에 계속 실패하고 있습니다. 잠시 후 다시 시도해주세요.", failure);
                        }))
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("카카오 토큰 요청 실패: {} - 응답 코드: {} - 응답 본문: {}",
                    e.getMessage(), e.getStatusCode(), e.getResponseBodyAsString());
            
            // 400 Bad Request 상세 처리
            if (e.getStatusCode().is4xxClientError()) {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody.contains("KOE320") || responseBody.contains("invalid_grant")) {
                    throw new RuntimeException("카카오 인가 코드가 만료되었거나 이미 사용된 코드입니다. 다시 로그인해주세요.", e);
                } else if (responseBody.contains("KOE303") || responseBody.contains("invalid_client")) {
                    throw new RuntimeException("카카오 앱 설정에 오류가 있습니다. 관리자에게 문의해주세요.", e);
                } else if (responseBody.contains("KOE006") || responseBody.contains("invalid_request")) {
                    throw new RuntimeException("카카오 로그인 요청 형식이 잘못되었습니다. 다시 시도해주세요.", e);
                }
                throw new RuntimeException("카카오 로그인 요청이 거부되었습니다: " + responseBody, e);
            }
            throw new RuntimeException("카카오 인증 서버 연결 중 오류가 발생했습니다: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            // TimeoutException 및 기타 예외 처리
            if (e.getCause() instanceof java.util.concurrent.TimeoutException || 
                e instanceof java.util.concurrent.TimeoutException ||
                e.getMessage().contains("timeout") || 
                e.getMessage().contains("408")) {
                log.error("카카오 토큰 요청 타임아웃: {}", e.getMessage());
                throw new RuntimeException("카카오 인증 서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.", e);
            }
            log.error("카카오 토큰 요청 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("카카오 인증 서버 연결 중 오류가 발생했습니다.", e);
        }
    }

    // 액세스 토큰으로 사용자 정보 요청
    public KakaoUserInfo requestUserInfo(String accessToken) {
        log.debug("카카오 사용자 정보 요청");

        try {
            KakaoUserInfo userInfo = kakaoWebClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfo.class)
                    .timeout(Duration.ofMillis(kakaoTimeoutMs)) // 설정값 사용
                    .retryWhen(reactor.util.retry.Retry.fixedDelay(2, Duration.ofSeconds(1))
                        .filter(throwable -> {
                            // 네트워크 오류나 5xx 에러에만 재시도
                            if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                var webClientException = (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                return webClientException.getStatusCode().is5xxServerError();
                            }
                            return throwable instanceof java.net.ConnectException || 
                                   throwable instanceof java.util.concurrent.TimeoutException ||
                                   throwable instanceof java.io.IOException;
                        })
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            Throwable failure = retrySignal.failure();
                            log.error("카카오 토큰 요청 재시도 횟수 초과: {}", failure.getMessage());
                            return new RuntimeException("카카오 서버 연결에 계속 실패하고 있습니다. 잠시 후 다시 시도해주세요.", failure);
                        }))
                    .block();

            log.debug("카카오 사용자 정보 응답 성공: id={}", userInfo.id());
            return userInfo;
        } catch (Exception e) {
            // TimeoutException 및 기타 예외 처리
            if (e.getCause() instanceof java.util.concurrent.TimeoutException || 
                e instanceof java.util.concurrent.TimeoutException ||
                e.getMessage().contains("timeout") || 
                e.getMessage().contains("408")) {
                log.error("카카오 사용자 정보 요청 타임아웃: {}", e.getMessage());
                throw new RuntimeException("카카오 사용자 정보 요청 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.", e);
            }
            log.error("카카오 사용자 정보 요청 실패: {}", e.getMessage(), e);
            throw new RuntimeException("카카오 사용자 정보 요청 중 오류가 발생했습니다.", e);
        }
    }
}