package com.example.auth.dto;

import com.example.auth.constant.Gender;
import com.example.auth.constant.UserRole;
import com.example.auth.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 데이터 공통 응답 DTO
 * 로그인 및 프로필 조회 시 일관된 응답 구조 제공
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    @Schema(description = "사용자 ID", example = "1", nullable = false)
    private Long id;

    @Schema(description = "이메일", example = "user@example.com", nullable = true)
    private String email;

    @Schema(description = "닉네임", example = "홍길동", nullable = false)
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", nullable = true)
    private String profileImage;

    @Schema(description = "전화번호", example = "010-1234-5678", nullable = true)
    private String phone;

    @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE", "UNKNOWN"}, nullable = true)
    private String gender;

    @Schema(description = "나이", example = "30", nullable = true)
    private Integer age;

    @Schema(description = "권한", example = "USER", allowableValues = {"USER", "CLIENT", "ADMIN"}, nullable = false)
    private String role;

    /**
     * User 엔티티로부터 UserDTO 생성
     */
    public static UserDTO fromEntity(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImg(),
                user.getPhone(),
                user.getGender(),
                user.getAge(),
                user.getRole()
        );
    }
}