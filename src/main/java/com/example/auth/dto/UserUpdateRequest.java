package com.example.auth.dto;

import com.example.auth.constant.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Schema(description = "사용자 정보 수정 요청 - 프로필 정보를 업데이트할 때 사용하는 데이터 구조", example = """
    {
      "email": "newemail@example.com",
      "nickname": "홍길동",
      "profileImage": "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg",
      "phone": "010-1234-5678",
      "gender": "MALE",
      "age": 30
    }
    """)
public class UserUpdateRequest {

    @Schema(description = "이메일 - 새로운 이메일 주소 (선택사항, 모든 사용자 변경 가능)", example = "newemail@example.com")
    @Email(message = "올바른 이메일 형식을 입력해주세요")
    private String email;

    @Schema(description = "닉네임 - 플랫폼 내에서 표시될 사용자 별명 (2~8자, 한글/영문/숫자 가능)", example = "홍길동")
    @Size(min = 2, max = 8, message = "닉네임은 2자 이상 8자 이하로 입력해주세요")
    private String nickname;

    @Schema(description = "프로필 이미지 URL - S3에 업로드된 이미지의 전체 URL (선택사항)", example = "https://drxgfm74s70w1.cloudfront.net/profile-images/user123.jpg")
    private String profileImage;

    @Schema(description = "전화번호 - 캠페인 선정 시 연락용 번호 (하이픈 포함 가능, 선택사항)", example = "010-1234-5678")
    private String phone;

    @Schema(description = "성별 - 개인화된 캠페인 추천을 위한 성별 정보 (선택사항)", example = "MALE")
    private Gender gender;

    @Schema(description = "나이 - 연령대별 캠페인 매칭을 위한 만 나이 (선택사항)", example = "30", minimum = "14", maximum = "100")
    private Integer age;
}
