package com.example.auth.service;

import com.example.auth.domain.User;
import com.example.auth.dto.KakaoUserInfo;
import com.example.auth.dto.UserLoginResult;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;

    // UserService.java
    public UserLoginResult findOrCreateUser(String provider, KakaoUserInfo info) {
        String socialId = String.valueOf(info.id());

        Optional<User> existingUser = userRepository.findByProviderAndSocialId(provider, socialId);

        if (existingUser.isPresent()) {
            return new UserLoginResult(existingUser.get(), false); // 기존 회원
        }

        String nickname = (String) info.properties().get("nickname");
        String profile = (String) info.properties().get("profile_image");
        String email = info.kakao_account() != null ? (String) info.kakao_account().get("email") : null;

        User newUser = userRepository.save(User.builder()
                .provider(provider)
                .socialId(socialId)
                .email(email)
                .nickname(nickname)
                .profileImg(profile)
                .role("USER")
                .build());

        return new UserLoginResult(newUser, true); // 신규 회원
    }
    
    /**
     * 사용자 닉네임 업데이트
     * @param userId 사용자 ID
     * @param nickname 새 닉네임
     * @return 업데이트된 사용자 객체
     * @throws RuntimeException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public User updateUserNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        
        user.updateNickname(nickname);
        return userRepository.save(user);
    }
    
    /**
     * 닉네임 중복 확인
     * @param nickname 확인할 닉네임
     * @return 중복 여부 (true: 중복, false: 중복 아님)
     */
    public boolean isNicknameExists(String nickname) {
        return userRepository.findByNickname(nickname).isPresent();
    }
    
    /**
     * 사용자 프로필 이미지 업데이트
     * @param userId 사용자 ID
     * @param imageUrl S3에 업로드된 이미지 URL
     * @return 업데이트된 사용자 객체
     * @throws RuntimeException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public User updateUserProfileImage(Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        
        // CloudFront URL로 변환 (또는 S3 URL 그대로 사용)
        String cloudFrontUrl = s3Service.getImageUrl(imageUrl);
        
        user.updateProfileImg(cloudFrontUrl);
        return userRepository.save(user);
    }
    
    // 사용자 역할 관리 메서드는 다른 프로젝트에서 관리합니다.
    
    /**
     * 사용자 ID로 사용자 조회
     * @param userId 사용자 ID
     * @return 사용자 객체 또는 null
     */
    public User findUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
    
    /**
     * 사용자 권한 업데이트
     * @param userId 사용자 ID
     * @param role 새로운 권한 ('USER', 'CLIENT', 'ADMIN')
     * @return 업데이트된 사용자 객체
     * @throws RuntimeException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public User updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        
        user.updateRole(role);
        return userRepository.save(user);
    }
}