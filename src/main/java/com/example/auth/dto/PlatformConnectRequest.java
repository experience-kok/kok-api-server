package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SNS 플랫폼 연동 요청 - 인플루언서가 자신의 SNS 계정을 연결할 때 사용하는 데이터 구조", example = """
    {
      "type": "INSTAGRAM",
      "url": "https://www.instagram.com/username"
    }
    """)
public class PlatformConnectRequest {

    @Schema(description = "플랫폼 타입 - 연동할 SNS 플랫폼의 종류 (각 타입별로 팔로워 수 크롤링 지원)", 
            example = "INSTAGRAM", 
            allowableValues = {"BLOG", "INSTAGRAM", "YOUTUBE"}, 
            required = true)
    @NotNull(message = "플랫폼 타입은 필수입니다")
    @Pattern(regexp = "BLOG|INSTAGRAM|YOUTUBE", message = "플랫폼 타입은 BLOG, INSTAGRAM, YOUTUBE 중 하나여야 합니다")
    private String type;

    @Schema(description = "SNS 플랫폼 URL - 사용자의 공개 프로필 페이지 주소 (팔로워 수 크롤링 및 인증에 사용)", 
            example = "https://www.instagram.com/username", 
            required = true)
    @NotBlank(message = "URL은 필수입니다")
    @Pattern(regexp = "https?://.*", message = "유효한 URL 형식이어야 합니다")
    private String url;
}
