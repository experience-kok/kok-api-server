package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record KakaoUserInfo(
        Long id,
        Map<String, Object> properties,
        @JsonProperty("kakao_account") Map<String, Object> kakao_account
) {}
