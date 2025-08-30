package com.example.auth.constant;

import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
public enum SortOption {
    LATEST("latest", "최신순", Sort.by(Sort.Direction.DESC, "createdAt"));

    private final String value;
    private final String description;
    private final Sort sort;

    SortOption(String value, String description, Sort sort) {
        this.value = value;
        this.description = description;
        this.sort = sort;
    }

    public static SortOption fromValue(String value) {
        for (SortOption option : values()) {
            if (option.getValue().equals(value)) {
                return option;
            }
        }
        return LATEST; // 기본값
    }
}
