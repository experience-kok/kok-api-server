package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NaverUserInfo {
    
    private String resultcode;
    private String message;
    private NaverUserResponse response;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class NaverUserResponse {
        
        private String id;
        private String nickname;
        private String email;
        private String name;
        
        @JsonProperty("profile_image")
        private String profileImage;
        
        private String gender;
        private String birthday;
    }
}
