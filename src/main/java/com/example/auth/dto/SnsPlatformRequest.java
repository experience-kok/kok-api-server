package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SnsPlatformRequest {

    @NotBlank(message = "플랫폼 타입은 필수입니다")
    @Pattern(regexp = "^(blog|instagram|youtube)$", message = "플랫폼 타입은 blog, instagram, youtube 중 하나여야 합니다")
    private String platformType;

    @NotBlank(message = "계정 URL은 필수입니다")
    private String accountUrl;

    @NotBlank(message = "계정 이름은 필수입니다")
    private String accountName;

    private Boolean verified;

}