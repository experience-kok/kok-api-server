package com.example.auth.exception;

public enum TokenErrorType {
    EXPIRED,                 // 액세스 토큰 만료
    INVALID,                 // 잘못된 토큰 (위조 등)
    REFRESH_INVALID,         // 리프레시 토큰 유효하지 않음
    UNKNOWN                  // 기타 예외
}
