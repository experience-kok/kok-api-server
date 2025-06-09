package com.example.auth.service;

import com.example.auth.domain.User;
import com.example.auth.dto.KakaoUserInfo;
import com.example.auth.dto.UserLoginResult;
import com.example.auth.repository.UserRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.CampaignApplicationRepository;
import com.example.auth.repository.UserSnsPlatformRepository;
import com.example.auth.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ImageProcessingService imageProcessingService;
    private final CampaignRepository campaignRepository;
    private final CampaignApplicationRepository campaignApplicationRepository;
    private final UserSnsPlatformRepository userSnsPlatformRepository;
    private final CompanyRepository companyRepository;

    // UserService.java
    public UserLoginResult findOrCreateUser(String provider, KakaoUserInfo info) {
        String socialId = String.valueOf(info.id());
        
        log.debug("카카오 사용자 정보 처리: id={}, properties={}, kakao_account={}", 
                  info.id(), info.properties(), info.kakao_account());

        Optional<User> existingUser = userRepository.findByProviderAndSocialId(provider, socialId);

        if (existingUser.isPresent()) {
            log.info("기존 사용자 로그인: socialId={}", socialId);
            return new UserLoginResult(existingUser.get(), false); // 기존 회원
        }

        // properties가 null인 경우 처리
        String nickname = null;
        String profile = null;
        if (info.properties() != null) {
            nickname = (String) info.properties().get("nickname");
            profile = (String) info.properties().get("profile_image");
        } else {
            log.warn("카카오 사용자 정보에 properties가 없음: id={}", info.id());
        }
        
        // nickname이 null인 경우 기본값 설정
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "사용자" + info.id(); // 기본 닉네임 생성
            log.info("기본 닉네임 생성: {}", nickname);
        }
        
        String email = null;
        if (info.kakao_account() != null) {
            email = (String) info.kakao_account().get("email");
        }

        User newUser = userRepository.save(User.builder()
                .provider(provider)
                .socialId(socialId)
                .email(email)
                .nickname(nickname)
                .profileImg(profile)
                .role("USER")
                .build());

        log.info("신규 사용자 생성 완료: userId={}, socialId={}, nickname={}", 
                 newUser.getId(), socialId, nickname);

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
     * @param imageUrl S3에 업로드된 이미지 URL (Presigned URL 또는 일반 S3 URL)
     * @return 업데이트된 사용자 객체
     * @throws RuntimeException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public User updateUserProfileImage(Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        
        // Presigned URL인 경우 쿼리 파라미터 제거
        String cleanUrl = cleanPresignedUrl(imageUrl);
        
        // 일단 원본 CloudFront URL로 저장
        String originalCloudFrontUrl = s3Service.getImageUrl(cleanUrl);
        user.updateProfileImg(originalCloudFrontUrl);
        User savedUser = userRepository.save(user);
        
        // 비동기로 리사이징 완료 후 URL 업데이트
        imageProcessingService.updateUserProfileImageWhenReady(userId, cleanUrl);
        
        return savedUser;
    }
    
    /**
     * Presigned URL에서 쿼리 파라미터를 제거하여 깨끗한 S3 URL을 반환
     * @param url Presigned URL 또는 일반 URL
     * @return 쿼리 파라미터가 제거된 깨끗한 URL
     */
    private String cleanPresignedUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        // 쿼리 파라미터가 있는 경우 제거
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            return url.substring(0, queryIndex);
        }
        
        return url;
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
    
    /**
     * 회원 탈퇴 처리
     * 연관된 모든 데이터를 안전하게 삭제합니다.
     * @param userId 탈퇴할 사용자 ID
     * @throws RuntimeException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteUserAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        
        log.info("회원 탈퇴 처리 시작: userId={}, role={}", userId, user.getRole());
        
        // 1. 사용자가 신청한 캠페인 신청 내역 삭제
        campaignApplicationRepository.deleteByUserId(userId);
        log.info("캠페인 신청 내역 삭제 완료: userId={}", userId);
        
        // 2. 사용자의 SNS 플랫폼 연동 정보 삭제
        userSnsPlatformRepository.deleteByUserId(userId);
        log.info("SNS 플랫폼 연동 정보 삭제 완료: userId={}", userId);
        
        // 3. 사용자가 생성한 캠페인들 처리
        var userCampaigns = campaignRepository.findByCreatorId(userId);
        if (!userCampaigns.isEmpty()) {
            log.info("사용자가 생성한 캠페인 {}개 삭제 시작", userCampaigns.size());
            
            // 각 캠페인의 신청 내역들을 먼저 삭제
            for (var campaign : userCampaigns) {
                campaignApplicationRepository.deleteByCampaignId(campaign.getId());
            }
            
            // 그 다음 캠페인들 삭제
            campaignRepository.deleteAll(userCampaigns);
            log.info("캠페인 및 관련 신청 내역 삭제 완료: userId={}", userId);
        }
        
        // 4. CLIENT 권한 사용자인 경우 업체 정보 삭제
        if ("CLIENT".equals(user.getRole())) {
            companyRepository.deleteByUserId(userId);
            log.info("업체 정보 삭제 완료: userId={}", userId);
        }
        
        // 5. 마지막으로 사용자 삭제
        userRepository.deleteById(userId);
        log.info("회원 탈퇴 처리 완료: userId={}", userId);
    }
}