package com.example.auth.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이메일 로그인 요청")
public class EmailLoginRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    @Schema(description = "이메일 주소", example = "user@example.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Schema(description = "비밀번호", example = "Password123!")
    private String password;
}
