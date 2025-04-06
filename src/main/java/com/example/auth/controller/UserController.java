package com.example.auth.controller;

import com.example.auth.common.ApiResponse;
import com.example.auth.domain.User;
import com.example.auth.dto.UserUpdateRequest;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtUtil;
import com.example.auth.service.TokenService;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "유저 API", description = "회원 정보 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final TokenUtils tokenUtils;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "내 정보 조회", description = "accessToken으로 로그인한 유저 정보를 가져옵니다.")
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String bearerToken) {
        Long userId = tokenUtils.getUserIdFromToken(bearerToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        log.info("사용자 프로필 조회: userId={}", userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        new ProfileResponse(
                                user.getId(),
                                user.getEmail(),             // 이메일은 읽기 전용
                                user.getNickname(),
                                user.getProfileImg(),
                                user.getPhone(),
                                user.getGender(),
                                user.getAge()
                        ),
                        "사용자 정보를 성공적으로 불러왔습니다."
                )
        );
    }

    @Operation(summary = "내 정보 수정", description = "사용자 정보를 수정합니다. 소셜 로그인으로 제공된 이메일은 수정할 수 없습니다.")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody @Valid UserUpdateRequest request
    ) {
        Long userId = tokenUtils.getUserIdFromToken(bearerToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // 수정 가능한 필드만 반영
        if (request.getNickname() != null) {
            user.updateNickname(request.getNickname());
        }

        if (request.getProfileImg() != null) {
            user.updateProfileImg(request.getProfileImg());
        }

        if (request.getPhone() != null) {
            user.updatePhone(request.getPhone());
        }

        if (request.getGender() != null) {
            user.updateGender(request.getGender());
        }

        if (request.getAge() != null) {
            user.updateAge(request.getAge());
        }

        // 이메일은 수정하지 않음 (소셜 로그인에서 받은 정보)
        // role도 수정 불가하며 응답에서도 제외

        User updatedUser = userRepository.save(user);
        log.info("사용자 정보 수정 완료: userId={}", userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        new ProfileResponse(
                                updatedUser.getId(),
                                updatedUser.getEmail(),             // 읽기 전용 필드
                                updatedUser.getNickname(),
                                updatedUser.getProfileImg(),
                                updatedUser.getPhone(),
                                updatedUser.getGender(),
                                updatedUser.getAge()
                        ),
                        "사용자 정보가 성공적으로 수정되었습니다."
                )
        );
    }

    // 응답용 DTO - role은 제외, email은 읽기 전용
    public record ProfileResponse(
            Long id,
            String email,
            String nickname,
            String profileImage,
            String phone,
            String gender,
            Integer age
    ) {}
}
