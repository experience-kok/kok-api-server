package com.example.auth.service;

import com.example.auth.domain.User;
import com.example.auth.dto.KakaoUserInfo;
import com.example.auth.dto.UserLoginResult;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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

}
