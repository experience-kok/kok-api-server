package com.example.auth.dto.withdrawal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 탈퇴 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 탈퇴 요청")
public class WithdrawalRequest {

    @Schema(description = "탈퇴 사유", example = "서비스를 더 이상 이용하지 않아서")
    @Size(max = 500, message = "탈퇴 사유는 500자 이내로 입력해주세요.")
    private String withdrawalReason;
}
