package com.example.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class UserUpdateRequest {

    @Size(min = 2, max = 100, message = "닉네임은 2자 이상 100자 이하로 입력해주세요")
    private String nickname;

    private String profileImg;
    private String phone;
    private String gender;
    private Integer age;
}