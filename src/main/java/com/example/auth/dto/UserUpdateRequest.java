package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class UserUpdateRequest {

    @Schema(description = "닉네임 (2~100자)", example = "홍길동", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    @Size(min = 2, max = 100, message = "닉네임은 2자 이상 100자 이하로 입력해주세요")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, 
            deprecated = true)
    @Deprecated // 별도의 PATCH API로 대체됨
    private String profileImg;

    @Schema(description = "전화번호", example = "010-1234-5678", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String phone;

    @Schema(description = "성별", example = "male", allowableValues = {"MALE", "FEMALE", "UNKNOWN"}, requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    @Pattern(regexp = "^(MALE|FEMALE|UNKNOWN)?$", message = "성별은 MALE, FEMALE, UNKNOWN 중 하나여야 합니다")
    private String gender;

    @Schema(description = "나이 (정수)", example = "30", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private Integer age;
}