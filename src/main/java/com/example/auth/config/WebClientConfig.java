package com.example.auth.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${webclient.timeout:30000}")
    private int timeout;
    
    @Value("${webclient.max-memory-size:16777216}")
    private int maxMemorySize;
    
    @Value("${crawler.user-agent:Mozilla/5.0}")
    private String userAgent;

    @Bean
    public WebClient webClient() {
        // 메모리 제한 설정 (기본값: 256KB, 여기서는 16MB로 설정)
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxMemorySize))
                .build();

        // HTTP 클라이언트 설정 (타임아웃 등)
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS)));

        // WebClient 생성
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    @Bean
    public WebClient kakaoWebClient() {
        // 카카오 API 전용 WebClient (더 긴 타임아웃과 연결 풀 최적화)
        HttpClient kakaoHttpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 연결 타임아웃 30초
                .responseTimeout(Duration.ofSeconds(75)) // 응답 타임아웃 75초 (여유있게)
                .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(75, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(75, TimeUnit.SECONDS)))
                // 연결 풀 설정
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true);

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(kakaoHttpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader("User-Agent", "체험콕/1.0 (https://chkok.kr)")
                .defaultHeader("Connection", "keep-alive")
                .build();
    }
}