package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUpdateRequest {
    @NotBlank(message = "이미지 URL은 필수입니다")
    @Pattern(regexp = "^https?://.*$", message = "올바른 URL 형식이어야 합니다")
    private String imageUrl;
}
