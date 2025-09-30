package com.example.auth.constant;

import lombok.Getter;

@Getter
public enum ConsentType {
    TERMS_OF_SERVICE("서비스 이용약관", "terms", true, "서비스 이용에 필요한 기본 약관입니다."),
    PRIVACY_POLICY("개인정보 처리방침", "privacy", true, "개인정보 수집 및 이용에 대한 동의입니다."),
    MARKETING("마케팅 정보 수신 동의", "marketing", false, "이벤트 및 프로모션 정보를 받으실 수 있습니다."),
    THIRD_PARTY_INFO("개인정보 제3자 제공 동의", "third_party", false, "제휴사에 정보 제공 동의입니다."),
    AGE_VERIFICATION("만 14세 이상 확인", "age", true, "만 14세 이상만 가입 가능합니다.");

    private final String displayName;
    private final String code;
    private final boolean required;
    private final String description;

    ConsentType(String displayName, String code, boolean required, String description) {
        this.displayName = displayName;
        this.code = code;
        this.required = required;
        this.description = description;
    }

    public static ConsentType fromCode(String code) {
        for (ConsentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown consent code: " + code);
    }
}
