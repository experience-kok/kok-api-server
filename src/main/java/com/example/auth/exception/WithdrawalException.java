package com.example.auth.exception;

/**
 * 회원 탈퇴 관련 예외
 */
public class WithdrawalException extends RuntimeException {

    public WithdrawalException(String message) {
        super(message);
    }

    public WithdrawalException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 재가입 제한 예외
     */
    public static class RejoinRestrictionException extends WithdrawalException {
        private final long remainingHours;

        public RejoinRestrictionException(String message, long remainingHours) {
            super(message);
            this.remainingHours = remainingHours;
        }

        public long getRemainingHours() {
            return remainingHours;
        }
    }

    /**
     * 탈퇴 불가 예외 (진행 중인 캠페인이 있는 경우 등)
     */
    public static class WithdrawalNotAllowedException extends WithdrawalException {
        public WithdrawalNotAllowedException(String message) {
            super(message);
        }
    }
}
