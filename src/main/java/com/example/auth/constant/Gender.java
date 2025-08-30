package com.example.auth.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    MALE("MALE"),
    FEMALE("FEMALE"),
    UNKNOWN("UNKNOWN");

    private final String value;

    Gender(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static Gender fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        
        for (Gender gender : Gender.values()) {
            if (gender.value.equalsIgnoreCase(value) || 
                gender.name().equalsIgnoreCase(value)) {
                return gender;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return this.value;
    }
}