package com.example.auth.controller;

import com.example.auth.common.ApiResponse;
import com.example.auth.domain.User;
import com.example.auth.dto.UserUpdateRequest;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtUtil;
import com.example.auth.service.TokenService;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
        try {
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
        } catch (JwtValidationException e) {
            log.warn("인증 오류 - 프로필 조회: {}, 타입: {}", e.getMessage(), e.getErrorType());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(e.getMessage(), e.getErrorType().name(), HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("프로필 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(summary = "내 정보 수정", description = "사용자 정보를 수정합니다. 소셜 로그인으로 제공된 이메일은 수정할 수 없습니다.")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody @Valid UserUpdateRequest request
    ) {
        try {
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
        } catch (JwtValidationException e) {
            log.warn("인증 오류 - 프로필 수정: {}, 타입: {}", e.getMessage(), e.getErrorType());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(e.getMessage(), e.getErrorType().name(), HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("프로필 수정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // 추가: 회원 탈퇴 API
    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 삭제합니다.")
    @DeleteMapping("/exit")
    public ResponseEntity<?> deleteAccount(@RequestHeader("Authorization") String bearerToken) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            // 탈퇴 로직 구현
            userRepository.deleteById(userId);

            // 해당 유저의 토큰 모두 제거
            tokenService.deleteRefreshToken(userId);

            // 액세스 토큰 블랙리스트 처리
            String token = bearerToken.replace("Bearer ", "");
            try {
                long remainTime = jwtUtil.getClaimsIgnoreExpiration(token).getExpiration().getTime() - System.currentTimeMillis();
                if (remainTime > 0) {
                    tokenService.blacklistAccessToken(token, remainTime);
                }
            } catch (Exception e) {
                log.warn("토큰 블랙리스트 처리 실패: {}", e.getMessage());
                // 회원 탈퇴는 계속 진행
            }

            log.info("회원 탈퇴 완료: userId={}", userId);

            return ResponseEntity.ok(ApiResponse.success(null, "회원 탈퇴가 완료되었습니다."));
        } catch (JwtValidationException e) {
            log.warn("인증 오류 - 회원 탈퇴: {}, 타입: {}", e.getMessage(), e.getErrorType());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(e.getMessage(), e.getErrorType().name(), HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("회원 탈퇴 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
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