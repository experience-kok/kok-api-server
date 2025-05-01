package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeConnectRequest {
    @NotBlank(message = "유튜브 채널 URL을 입력해주세요")
    @Pattern(regexp = "^https?://(www\\.)?youtube\\.com/(channel|c|user)/[^/]+/?.*$|^https?://(www\\.)?youtube\\.com/@[^/]+/?.*$", 
            message = "올바른 유튜브 채널 URL 형식이 아닙니다")
    private String youtubeUrl;
}
