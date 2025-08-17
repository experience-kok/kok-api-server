package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "프로필 이미지 수정 요청 - 사용자의 프로필 사진만 단독으로 변경할 때 사용", example = """
    {
      "profileImage": "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg"
    }
    """)
public class ProfileImageUpdateRequest {
    @Schema(description = "새로운 프로필 이미지 URL - S3에 업로드 완료된 이미지의 전체 경로 (CloudFront CDN을 통해 제공됨)", 
            example = "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg", 
            required = true)
    @NotBlank(message = "프로필 이미지 URL은 필수입니다")
    private String profileImage;
}
