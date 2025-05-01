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
public class FollowerCountUpdateRequest {

    @Schema(description = "팔로워/구독자 수",
            example = "1000",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false)
    @NotNull(message = "팔로워 수는 필수입니다")
    @Min(value = 0, message = "팔로워 수는 0 이상이어야 합니다")
    private Integer followerCount;
}