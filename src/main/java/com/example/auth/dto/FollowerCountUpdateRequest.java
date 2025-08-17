package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팔로워 수 업데이트 요청 - SNS 플랫폼의 팔로워/구독자 수를 수동으로 업데이트할 때 사용", example = """
    {
      "followerCount": 1000
    }
    """)
public class FollowerCountUpdateRequest {

    @Schema(description = "팔로워/구독자 수 - 해당 SNS 플랫폼의 현재 팔로워 또는 구독자 수 (캠페인 매칭 및 인플루언서 등급 산정에 사용)", 
            example = "1000", 
            minimum = "0",
            required = true)
    @NotNull(message = "팔로워 수는 필수입니다")
    @Min(value = 0, message = "팔로워 수는 0 이상이어야 합니다")
    private Integer followerCount;
}
