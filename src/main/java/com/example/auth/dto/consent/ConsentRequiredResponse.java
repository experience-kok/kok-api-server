package com.example.auth.dto.consent;

import com.example.auth.dto.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "동의 필요 응답 DTO (신규 회원)")
public class ConsentRequiredResponse {

    @Schema(description = "로그인 타입", example = "consentRequired")
    private String loginType;

    @Schema(description = "임시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String tempToken;

    @Schema(description = "사용자 정보")
    private UserDTO user;
}
