package com.example.auth.constant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Gender", description = "성별 값")
public enum Gender {
    @Schema(description = "남성")
    MALE("male"),

    @Schema(description = "여성")
    FEMALE("female"),

    @Schema(description = "알 수 없음")
    UNKNOWN("unknown");

    private final String value;

    Gender(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Gender fromString(String value) {
        for (Gender gender : Gender.values()) {
            if (gender.value.equalsIgnoreCase(value)) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}