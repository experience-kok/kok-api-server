package com.example.auth.dto.withdrawal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 회원 탈퇴 완료 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원 탈퇴 완료 응답")
public class WithdrawalResponse {

    @Schema(description = "탈퇴 처리 시간", example = "2025-08-06T15:30:00+09:00")
    private ZonedDateTime withdrawnAt;

    @Schema(description = "재가입 가능 시간", example = "2025-08-07T15:30:00+09:00")
    private ZonedDateTime canRejoinAt;

    @Schema(description = "탈퇴 사유", example = "서비스를 더 이상 이용하지 않아서")
    private String withdrawalReason;

    @Schema(description = "안내 메시지", example = "탈퇴가 완료되었습니다. 24시간 후 재가입이 가능합니다.")
    private String message;
}
