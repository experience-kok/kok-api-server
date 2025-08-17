package com.example.auth.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "UserRole",
    description = "사용자 권한 - 플랫폼 내에서 사용자의 역할과 접근 권한을 정의합니다",
    enumAsRef = true,
    type = "string",
    allowableValues = {"USER", "CLIENT", "ADMIN"},
    example = "USER"
)
public enum UserRole {
    @Schema(description = "일반 사용자 - 인플루언서로서 캠페인에 신청하고 참여할 수 있는 기본 사용자")
    USER("USER"),

    @Schema(description = "클라이언트(광고주) - 캠페인을 등록하고 관리할 수 있는 기업/브랜드 사용자")
    CLIENT("CLIENT"),

    @Schema(description = "관리자 - 캠페인 승인/거절 및 시스템 전반을 관리할 수 있는 최고 권한 사용자")
    ADMIN("ADMIN");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) {
            return USER;
        }
        
        for (UserRole role : UserRole.values()) {
            if (role.value.equalsIgnoreCase(value) || 
                role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        return USER; // 기본값은 USER
    }

    @Override
    public String toString() {
        return this.value;
    }
}