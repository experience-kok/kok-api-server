package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageUpdateRequest {
    @NotBlank(message = "프로필 이미지 URL은 필수입니다")
    private String profileImage;
}
