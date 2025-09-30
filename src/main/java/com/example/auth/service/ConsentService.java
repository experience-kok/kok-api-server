package com.example.auth.service;

import com.example.auth.constant.ConsentType;
import com.example.auth.domain.User;
import com.example.auth.domain.UserConsent;
import com.example.auth.dto.TempUserData;
import com.example.auth.repository.UserConsentRepository;
import com.example.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentService {

    private final UserConsentRepository userConsentRepository;
    private final TempUserService tempUserService;
    private final UserRepository userRepository;

    /**
     * Redis에서 임시 사용자 데이터를 가져와 정식 사용자로 생성하고 동의 정보 저장
     * @param tempUserId Redis에 저장된 임시 사용자 ID
     * @param agreements 동의 항목
     * @param request HTTP 요청 (IP 추출용)
     * @return 생성된 정식 User 객체
     */
    @Transactional
    public User createUserFromTempData(String tempUserId, Map<String, Boolean> agreements, HttpServletRequest request) {
        // 1. Redis에서 임시 사용자 데이터 조회
        TempUserData tempUserData = tempUserService.getTempUser(tempUserId);
        
        if (tempUserData == null) {
            log.error("임시 사용자 데이터 없음 (만료됨): tempUserId={}", tempUserId);
            throw new RuntimeException("세션이 만료되었습니다. 다시 로그인해주세요.");
        }
        
        // 2. 정식 User 생성 (DB에 저장)
        User newUser = User.builder()
                .provider(tempUserData.getProvider())
                .socialId(tempUserData.getSocialId())
                .email(tempUserData.getEmail())
                .nickname(tempUserData.getNickname())
                .profileImg(tempUserData.getProfileImg())
                .role("USER")
                .build();
        
        User savedUser = userRepository.save(newUser);
        log.info("Redis 임시 데이터 → DB 정식 사용자 생성: userId={}, socialId={}", 
                savedUser.getId(), tempUserData.getSocialId());
        
        // 3. 동의 정보 저장
        saveConsents(savedUser, agreements, request);
        
        // 4. Redis에서 임시 데이터 삭제
        tempUserService.deleteTempUser(tempUserId);
        log.info("Redis 임시 데이터 삭제 완료: tempUserId={}", tempUserId);
        
        return savedUser;
    }

    @Transactional
    public void saveConsents(User user, Map<String, Boolean> agreements, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String consentVersion = "1.0"; // 약관 버전 (설정으로 관리 가능)

        // termsAgreed -> TERMS_OF_SERVICE
        if (agreements.containsKey("termsAgreed")) {
            saveConsent(user, ConsentType.TERMS_OF_SERVICE, 
                       agreements.get("termsAgreed"), consentVersion, ipAddress);
        }

        // privacyPolicyAgreed -> PRIVACY_POLICY
        if (agreements.containsKey("privacyPolicyAgreed")) {
            saveConsent(user, ConsentType.PRIVACY_POLICY, 
                       agreements.get("privacyPolicyAgreed"), consentVersion, ipAddress);
        }

        log.info("사용자 동의 항목 저장 완료: userId={}, ip={}", user.getId(), ipAddress);
    }

    private void saveConsent(User user, ConsentType consentType, Boolean agreed, 
                            String version, String ipAddress) {
        UserConsent consent = UserConsent.builder()
                .user(user)
                .consentType(consentType)
                .agreed(agreed)
                .consentVersion(version)
                .ipAddress(ipAddress)
                .build();

        userConsentRepository.save(consent);
        
        log.debug("동의 항목 저장: userId={}, type={}, agreed={}", 
                 user.getId(), consentType, agreed);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // X-Forwarded-For는 여러 IP가 올 수 있으므로 첫 번째만 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
