package com.example.auth.dto;

import com.example.auth.config.UserDtoConfig;
import com.example.auth.constant.Gender;
import com.example.auth.constant.UserRole;
import com.example.auth.domain.User;
import com.example.auth.service.S3Service;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 데이터 공통 응답 DTO
 * 로그인 및 프로필 조회 시 일관된 응답 구조 제공
 */
@Slf4j
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보 - 로그인 및 프로필 조회 시 반환되는 사용자 데이터", example = """
    {
      "id": 123,
      "email": "user@example.com",
      "nickname": "홍길동",
      "profileImage": "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg",
      "phone": "010-1234-5678",
      "gender": "MALE",
      "age": 30,
      "role": "USER"
    }
    """)
public class UserDTO {
    @Schema(description = "사용자 고유 식별자 - 시스템에서 사용자를 구분하는 유일한 ID", example = "123", required = true)
    private Long id;

    @Schema(description = "이메일 주소 - 로그인 및 알림 발송에 사용되는 사용자의 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임 - 플랫폼 내에서 표시되는 사용자의 별명 (2~8자)", example = "홍길동", required = true)
    private String nickname;

    @Schema(description = "프로필 이미지 URL - 사용자의 프로필 사진 (CloudFront CDN을 통해 제공)", example = "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg")
    private String profileImage;

    @Schema(description = "전화번호 - 캠페인 선정 시 연락용으로 사용되는 연락처 (선택사항)", example = "010-1234-5678")
    private String phone;

    @Schema(description = "성별 - 통계 분석 및 맞춤형 캠페인 추천용 (선택사항)", example = "MALE")
    private Gender gender;

    @Schema(description = "나이 - 연령대별 캠페인 타겟팅 및 통계 분석용 (만 나이 기준, 선택사항)", example = "30")
    private Integer age;

    @Schema(description = "사용자 권한 - 플랫폼 내 역할과 접근 권한을 나타냄", example = "USER", required = true)
    private UserRole role;

    public static UserDTO fromEntity(User user) {
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
        
        // UserRole enum 변환
        UserRole userRole = null;
        if (user.getRole() != null) {
            try {
                userRole = UserRole.fromString(user.getRole());
            } catch (Exception e) {
                log.warn("사용자 권한 변환 중 오류 발생: {}", e.getMessage());
                userRole = UserRole.USER;
            }
        }
        
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(profileImageUrl)
                .phone(user.getPhone())
                .gender(user.getGender()) // 이제 Gender enum을 직접 사용
                .age(user.getAge())
                .role(userRole) // 이제 UserRole enum을 사용
                .build();
    }
}
