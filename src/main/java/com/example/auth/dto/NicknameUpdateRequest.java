package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "닉네임 수정 요청 - 사용자의 닉네임만 단독으로 변경할 때 사용", example = """
    {
      "nickname": "홍길동"
    }
    """)
public class NicknameUpdateRequest {
    @Schema(description = "새로운 닉네임 - 플랫폼 내에서 표시될 사용자 별명 (2~8자, 한글/영문/숫자 가능, 중복 불가)", example = "홍길동", required = true)
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 8, message = "닉네임은 2자 이상 8자 이하로 입력해주세요")
    private String nickname;
}
