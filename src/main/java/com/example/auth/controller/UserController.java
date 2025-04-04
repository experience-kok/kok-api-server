package com.example.auth.controller;

import com.example.auth.common.ApiResponse;
import com.example.auth.domain.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtUtil;
import com.example.auth.service.TokenService;
import com.example.auth.util.TokenUtils;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    private final TokenService tokenService;  // 추가: TokenService 주입
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
                        new ProfileResponse(user.getId(), user.getEmail(), user.getNickname(), user.getProfileImg(), user.getRole()),
                        "사용자 정보를 성공적으로 불러왔습니다."
                )
        );
    }

    @Operation(summary = "회원 탈퇴", description = "accessToken으로 로그인한 유저를 탈퇴 처리합니다.")
    @DeleteMapping("/exit")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String bearerToken) {
        Long userId = tokenUtils.getUserIdFromToken(bearerToken);
        String token = bearerToken.replace("Bearer ", "");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // Redis에서 토큰 정보 삭제
        tokenService.deleteRefreshToken(userId);

        // 현재 토큰을 블랙리스트에 추가
        try {
            Claims claims = jwtUtil.getClaims(token);
            long remainTime = claims.getExpiration().getTime() - System.currentTimeMillis();
            tokenService.blacklistAccessToken(token, remainTime);
        } catch (Exception e) {
            log.warn("토큰 블랙리스트 등록 중 오류: {}", e.getMessage());
            // 토큰 처리 실패해도 회원 탈퇴는 진행
        }

        // DB에서 사용자 정보 삭제
        userRepository.delete(user);
        log.info("회원 탈퇴 완료: userId={}", userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "회원 탈퇴가 완료되었습니다."
                )
        );
    }

    public record ProfileResponse(
            Long id,
            String email,
            String nickname,
            String profileImage,
            String role
    ) {}
}