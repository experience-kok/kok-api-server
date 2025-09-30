// UserLoginResult.java
package com.example.auth.dto;

import com.example.auth.domain.User;

public record UserLoginResult(User user, boolean isNew, String tempUserId) {
    // 기존 회원용 생성자 (tempUserId 없음)
    public UserLoginResult(User user, boolean isNew) {
        this(user, isNew, null);
    }
}
