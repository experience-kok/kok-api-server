package com.example.auth.constant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "사용자 권한")
public enum UserRole {
    @Schema(description = "일반 사용자")
    USER("USER"),

    @Schema(description = "클라이언트(광고주)")
    CLIENT("CLIENT"),

    @Schema(description = "관리자")
    ADMIN("ADMIN");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromString(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return USER; // 기본값은 USER
    }
}