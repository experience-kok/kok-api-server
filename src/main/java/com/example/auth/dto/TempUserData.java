package com.example.auth.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TempUserData {
    
    private String tempUserId;      // UUID
    private String provider;        // "kakao"
    private String socialId;        // 카카오 ID
    private String email;
    private String nickname;
    private String profileImg;
    private LocalDateTime createdAt;
    
    public static TempUserData fromKakaoInfo(String tempUserId, String provider, 
                                              String socialId, String email, 
                                              String nickname, String profileImg) {
        return TempUserData.builder()
                .tempUserId(tempUserId)
                .provider(provider)
                .socialId(socialId)
                .email(email)
                .nickname(nickname)
                .profileImg(profileImg)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
