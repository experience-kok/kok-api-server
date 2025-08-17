package com.example.auth.dto.like;

import com.example.auth.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 좋아요한 사용자 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "좋아요한 사용자 정보")
public class LikeUserResponse {

    @Schema(description = "사용자 ID", example = "123")
    private Long userId;

    @Schema(description = "닉네임", example = "인플루언서123")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "사용자 역할", example = "USER")
    private String role;

    @Schema(description = "좋아요한 시간", example = "2025-07-29T10:30:00")
    private LocalDateTime likedAt;

    /**
     * User 엔티티로부터 LikeUserResponse 생성
     */
    public static LikeUserResponse fromUser(User user, LocalDateTime likedAt) {
        return LikeUserResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImg())
                .role(user.getRole())
                .likedAt(likedAt)
                .build();
    }
}
