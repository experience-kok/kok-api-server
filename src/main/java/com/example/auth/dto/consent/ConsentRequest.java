package com.example.auth.dto.consent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "동의 완료 요청 DTO")
public class ConsentRequest {

    @NotBlank(message = "임시 토큰은 필수입니다")
    @Schema(description = "임시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String tempToken;

    @NotNull(message = "동의 항목은 필수입니다")
    @Schema(description = "동의 항목", example = "{\"termsAgreed\": true, \"privacyPolicyAgreed\": true}")
    private Map<String, Boolean> agreements;
}
