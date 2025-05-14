package com.example.auth.dto;

import com.example.auth.config.UserDtoConfig;
import com.example.auth.constant.Gender;
import com.example.auth.constant.UserRole;
import com.example.auth.domain.User;
import com.example.auth.service.S3Service;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 데이터 공통 응답 DTO
 * 로그인 및 프로필 조회 시 일관된 응답 구조 제공
 */
@Slf4j
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
     * enum 타입은 대문자로 변환하여 반환
     */
    public static UserDTO fromEntity(User user) {
        // 성별 값이 있는 경우 대문자로 변환
        String genderUpperCase = null;
        if (user.getGender() != null) {
            try {
                // Gender enum으로 변환 후 name()을 사용하여 대문자 이름 가져오기
                genderUpperCase = Gender.fromString(user.getGender()).name();
            } catch (Exception e) {
                // 변환 중 오류 발생 시 원본 값 유지
                genderUpperCase = user.getGender().toUpperCase();
            }
        }
        
        // 역할 값이 있는 경우 항상 대문자로 반환
        String roleUpperCase = user.getRole();
        if (roleUpperCase != null && !roleUpperCase.equals(roleUpperCase.toUpperCase())) {
            roleUpperCase = roleUpperCase.toUpperCase();
        }
        
        // 프로필 이미지 URL을 CloudFront URL로 변환
        String profileImageUrl = user.getProfileImg();
        try {
            // S3Service 인스턴스 가져오기
            S3Service s3Service = UserDtoConfig.UserDtoHelper.getS3Service();
            if (s3Service != null && profileImageUrl != null) {
                profileImageUrl = s3Service.getImageUrl(profileImageUrl);
                log.debug("프로필 이미지 URL 변환: {} -> {}", user.getProfileImg(), profileImageUrl);
            }
        } catch (Exception e) {
            log.warn("프로필 이미지 URL 변환 중 오류 발생: {}", e.getMessage());
            // 오류 발생 시 원본 URL 사용
        }
        
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                profileImageUrl, // CloudFront URL로 변환된 이미지 URL
                user.getPhone(),
                genderUpperCase,
                user.getAge(),
                roleUpperCase
        );
    }
}