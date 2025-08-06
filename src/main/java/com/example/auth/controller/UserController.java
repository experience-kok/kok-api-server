package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.domain.User;
import com.example.auth.dto.NicknameUpdateRequest;
import com.example.auth.dto.ProfileImageUpdateRequest;
import com.example.auth.dto.UserDTO;
import com.example.auth.dto.UserUpdateRequest;
import com.example.auth.dto.withdrawal.WithdrawalRequest;
import com.example.auth.dto.withdrawal.WithdrawalResponse;
import com.example.auth.common.ErrorResponseDTO;
import com.example.auth.common.UserProfileResponseDTO;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.exception.TokenErrorType;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtUtil;
import com.example.auth.service.S3Service;
import com.example.auth.service.TokenService;
import com.example.auth.service.UserService;
import com.example.auth.service.UserWithdrawalService;
import com.example.auth.util.TokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

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
    private final UserService userService;
    private final S3Service s3Service;
    private final UserWithdrawalService userWithdrawalService;

    @Operation(summary = "내 정보 조회", description = "accessToken으로 로그인한 유저 정보를 가져옵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/UserProfileSuccessResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "프로필 조회 성공",
                                summary = "사용자 프로필 정보 조회 성공",
                                value = "{\n" +
                                       "  \"success\": true,\n" +
                                       "  \"message\": \"사용자 정보를 성공적으로 불러왔습니다.\",\n" +
                                       "  \"status\": 200,\n" +
                                       "  \"data\": {\n" +
                                       "    \"user\": {\n" +
                                       "      \"id\": 123,\n" +
                                       "      \"email\": \"user@example.com\",\n" +
                                       "      \"nickname\": \"체험러123\",\n" +
                                       "      \"role\": \"USER\",\n" +
                                       "      \"profileImageUrl\": \"https://example.com/profiles/user123.jpg\",\n" +
                                       "      \"phone\": \"010-1234-5678\",\n" +
                                       "      \"gender\": \"FEMALE\",\n" +
                                       "      \"age\": 25,\n" +
                                       "      \"createdAt\": \"2025-01-01T10:00:00Z\",\n" +
                                       "      \"updatedAt\": \"2025-07-10T15:30:00Z\"\n" +
                                       "    }\n" +
                                       "  }\n" +
                                       "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "JWT 인증 실패 (만료, 위조 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = {
                                @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "토큰 만료",
                                    summary = "JWT 토큰이 만료된 경우",
                                    value = "{\n" +
                                           "  \"success\": false,\n" +
                                           "  \"message\": \"만료된 토큰입니다.\",\n" +
                                           "  \"errorCode\": \"TOKEN_EXPIRED\",\n" +
                                           "  \"status\": 401\n" +
                                           "}"
                                ),
                                @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "토큰 없음",
                                    summary = "Authorization 헤더가 없는 경우",
                                    value = "{\n" +
                                           "  \"success\": false,\n" +
                                           "  \"message\": \"인증 토큰이 필요합니다.\",\n" +
                                           "  \"errorCode\": \"UNAUTHORIZED\",\n" +
                                           "  \"status\": 401\n" +
                                           "}"
                                )
                            })),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "서버 내부 오류",
                                value = "{\n" +
                                       "  \"success\": false,\n" +
                                       "  \"message\": \"서버 오류가 발생했습니다.\",\n" +
                                       "  \"errorCode\": \"INTERNAL_ERROR\",\n" +
                                       "  \"status\": 500\n" +
                                       "}"
                            )))
    })
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        log.info("프로필 조회 요청 받음 - 토큰: {}", maskToken(bearerToken));

        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

            log.info("사용자 프로필 조회: userId={}", userId);

            UserDTO userDTO = UserDTO.fromEntity(user);
            Map<String, Object> responseData = Map.of("user", userDTO);

            return ResponseEntity.ok(
                    BaseResponse.success(
                            responseData,
                            "사용자 정보를 성공적으로 불러왔습니다."
                    )
            );
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰으로 프로필 조회 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        } catch (JwtValidationException e) {
            log.warn("JWT 검증 오류 - 프로필 조회: {}, 타입: {}", e.getMessage(), e.getErrorType());

            String errorCode = "UNAUTHORIZED";
            if (e.getErrorType() == TokenErrorType.EXPIRED) {
                errorCode = "TOKEN_EXPIRED";
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("프로필 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }



    @Operation(summary = "내 정보 수정", description = "사용자 정보를 수정합니다. 소셜 로그인으로 제공된 이메일은 수정할 수 없습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/UserProfileSuccessResponse"))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 (검증 실패 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "JWT 인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "수정할 사용자 정보", required = true)
            @RequestBody @Valid UserUpdateRequest request
    ) {
        log.info("프로필 수정 요청 받음 - 토큰: {}", maskToken(bearerToken));

        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

            // 수정 가능한 필드만 반영
            if (request.getNickname() != null) {
                user.updateNickname(request.getNickname());
            }

            if (request.getProfileImage() != null) {
                if (!request.getProfileImage().startsWith("http")) {
                    return ResponseEntity.badRequest()
                            .body(BaseResponse.fail("유효한 이미지 URL이 아닙니다.", "INVALID_IMAGE_URL", HttpStatus.BAD_REQUEST.value()));
                }
                user.updateProfileImg(request.getProfileImage());
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

            User updatedUser = userRepository.save(user);
            log.info("사용자 정보 수정 완료: userId={}", userId);

            UserDTO userDTO = UserDTO.fromEntity(updatedUser);
            Map<String, Object> responseData = Map.of("user", userDTO);

            return ResponseEntity.ok(
                    BaseResponse.success(
                            responseData,
                            "사용자 정보가 성공적으로 수정되었습니다."
                    )
            );
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰으로 프로필 수정 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        } catch (JwtValidationException e) {
            log.warn("JWT 검증 오류 - 프로필 수정: {}, 타입: {}", e.getMessage(), e.getErrorType());

            String errorCode = "UNAUTHORIZED";
            if (e.getErrorType() == TokenErrorType.EXPIRED) {
                errorCode = "TOKEN_EXPIRED";
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("프로필 수정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(summary = "프로필 이미지 수정", description = "사용자의 프로필 이미지만 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 이미지 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/UserProfileSuccessResponse"))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "JWT 인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PatchMapping("/profile/image")
    public ResponseEntity<?> updateProfileImage(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody @Valid ProfileImageUpdateRequest request
    ) {
        log.info("프로필 이미지 수정 요청 받음 - 토큰: {}", maskToken(bearerToken));

        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            User updatedUser = userService.updateUserProfileImage(userId, request.getProfileImage());
            log.info("사용자 프로필 이미지 수정 완료: userId={}", userId);

            Map<String, Object> userData = Map.of(
                    "id", updatedUser.getId(),
                    "profileImage", s3Service.getImageUrl(updatedUser.getProfileImg())
            );
            
            Map<String, Object> responseData = Map.of("user", userData);

            return ResponseEntity.ok(
                    BaseResponse.success(
                            responseData,
                            "프로필 이미지가 성공적으로 수정되었습니다."
                    )
            );
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰으로 프로필 이미지 수정 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        } catch (JwtValidationException e) {
            log.warn("JWT 검증 오류 - 프로필 이미지 수정: {}, 타입: {}", e.getMessage(), e.getErrorType());

            String errorCode = "UNAUTHORIZED";
            if (e.getErrorType() == TokenErrorType.EXPIRED) {
                errorCode = "TOKEN_EXPIRED";
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("프로필 이미지 수정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임만 수정합니다. 닉네임 중복 검사가 이루어집니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "닉네임 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/UserProfileSuccessResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "닉네임 수정 성공",
                                summary = "닉네임이 성공적으로 변경된 경우",
                                value = "{\n" +
                                       "  \"success\": true,\n" +
                                       "  \"message\": \"닉네임이 성공적으로 수정되었습니다.\",\n" +
                                       "  \"status\": 200,\n" +
                                       "  \"data\": {\n" +
                                       "    \"user\": {\n" +
                                       "      \"id\": 123,\n" +
                                       "      \"nickname\": \"새로운닉네임123\"\n" +
                                       "    }\n" +
                                       "  }\n" +
                                       "}"
                            ))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 (검증 실패, 닉네임 중복 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = {
                                @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "닉네임 중복",
                                    summary = "이미 사용 중인 닉네임인 경우",
                                    value = "{\n" +
                                           "  \"success\": false,\n" +
                                           "  \"message\": \"이미 사용 중인 닉네임입니다.\",\n" +
                                           "  \"errorCode\": \"NICKNAME_DUPLICATE\",\n" +
                                           "  \"status\": 400\n" +
                                           "}"
                                ),
                                @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "유효성 검증 실패",
                                    summary = "닉네임 형식이 올바르지 않은 경우",
                                    value = "{\n" +
                                           "  \"success\": false,\n" +
                                           "  \"message\": \"닉네임은 2-20자 사이로 입력해주세요.\",\n" +
                                           "  \"errorCode\": \"VALIDATION_ERROR\",\n" +
                                           "  \"status\": 400\n" +
                                           "}"
                                )
                            })),
            @ApiResponse(responseCode = "401", description = "JWT 인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "인증 실패",
                                value = "{\n" +
                                       "  \"success\": false,\n" +
                                       "  \"message\": \"유효하지 않은 토큰입니다.\",\n" +
                                       "  \"errorCode\": \"TOKEN_INVALID\",\n" +
                                       "  \"status\": 401\n" +
                                       "}"
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "서버 내부 오류",
                                value = "{\n" +
                                       "  \"success\": false,\n" +
                                       "  \"message\": \"서버 오류가 발생했습니다.\",\n" +
                                       "  \"errorCode\": \"INTERNAL_ERROR\",\n" +
                                       "  \"status\": 500\n" +
                                       "}"
                            )))
    })
    @PatchMapping("/profile/nickname")
    public ResponseEntity<?> updateNickname(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody @Valid NicknameUpdateRequest request
    ) {
        log.info("닉네임 수정 요청 받음 - 토큰: {}", maskToken(bearerToken));

        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
            
            // 닉네임 중복 검사
            String newNickname = request.getNickname();
            if (!newNickname.equals(user.getNickname())) {
                Optional<User> existingUser = userRepository.findByNickname(newNickname);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                    log.warn("닉네임 중복 발생: {}", newNickname);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(BaseResponse.fail("이미 사용 중인 닉네임입니다.", "NICKNAME_DUPLICATE", HttpStatus.BAD_REQUEST.value()));
                }
            }

            User updatedUser = userService.updateUserNickname(userId, newNickname);
            log.info("사용자 닉네임 수정 완료: userId={}, nickname={}", userId, newNickname);

            Map<String, Object> userData = Map.of(
                    "id", updatedUser.getId(),
                    "nickname", updatedUser.getNickname()
            );
            
            Map<String, Object> responseData = Map.of("user", userData);

            return ResponseEntity.ok(
                    BaseResponse.success(
                            responseData,
                            "닉네임이 성공적으로 수정되었습니다."
                    )
            );
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰으로 닉네임 수정 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        } catch (JwtValidationException e) {
            log.warn("JWT 검증 오류 - 닉네임 수정: {}, 타입: {}", e.getMessage(), e.getErrorType());

            String errorCode = "UNAUTHORIZED";
            if (e.getErrorType() == TokenErrorType.EXPIRED) {
                errorCode = "TOKEN_EXPIRED";
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("닉네임 수정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 탈퇴 처리합니다. 탈퇴 후 24시간 동안 재가입이 제한됩니다.\n\n" +
                                                        "### 탈퇴 제한 조건:\n" +
                                                        "- **CLIENT 사용자**: 승인되고 진행 중인 캠페인이 있는 경우 탈퇴 불가 (만료된 캠페인은 탈퇴 가능)\n" +
                                                        "- **USER 사용자**: 선정된 캠페인 신청이 있는 경우 탈퇴 불가\n" +
                                                        "- **대기 상태**: APPLIED, PENDING 상태의 신청은 탈퇴 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiSuccessResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "탈퇴 성공",
                                summary = "회원 탈퇴가 성공적으로 처리된 경우",
                                value = "{\n" +
                                       "  \"success\": true,\n" +
                                       "  \"message\": \"회원 탈퇴가 완료되었습니다.\",\n" +
                                       "  \"status\": 200,\n" +
                                       "  \"data\": {\n" +
                                       "    \"withdrawnAt\": \"2025-08-06T15:30:00+09:00\",\n" +
                                       "    \"canRejoinAt\": \"2025-08-07T15:30:00+09:00\",\n" +
                                       "    \"withdrawalReason\": \"서비스를 더 이상 이용하지 않아서\",\n" +
                                       "    \"message\": \"탈퇴가 완료되었습니다. 24시간 후 재가입이 가능합니다.\"\n" +
                                       "  }\n" +
                                       "}"
                            ))),
            @ApiResponse(responseCode = "400", description = "탈퇴 불가 (승인된 캠페인, 선정된 신청 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"),
                            examples = {
                                @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "CLIENT - 진행 중인 승인된 캠페인 있음",
                                    summary = "CLIENT 사용자가 승인되고 진행 중인 캠페인이 있어 탈퇴할 수 없는 경우",
                                    value = "{\n" +
                                           "  \"success\": false,\n" +
                                           "  \"message\": \"진행 중인 승인된 캠페인이 있어 탈퇴할 수 없습니다. 캠페인 완료 후 다시 시도해주세요.\",\n" +
                                           "  \"errorCode\": \"WITHDRAWAL_NOT_ALLOWED\",\n" +
                                           "  \"status\": 400\n" +
                                           "}"
                                ),
                                @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "USER - 선정된 캠페인 있음",
                                    summary = "USER 사용자가 선정된 캠페인 신청이 있어 탈퇴할 수 없는 경우",
                                    value = "{\n" +
                                           "  \"success\": false,\n" +
                                           "  \"message\": \"선정된 캠페인이 있어 탈퇴할 수 없습니다. 캠페인 완료 후 다시 시도해주세요.\",\n" +
                                           "  \"errorCode\": \"WITHDRAWAL_NOT_ALLOWED\",\n" +
                                           "  \"status\": 400\n" +
                                           "}"
                                )
                            })),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @DeleteMapping("/exit")
    public ResponseEntity<?> withdrawUser(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "탈퇴 요청 정보")
            @RequestBody(required = false) WithdrawalRequest request
    ) {
        log.info("회원 탈퇴 요청 받음 - 토큰: {}", maskToken(bearerToken));

        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            String withdrawalReason = request != null ? request.getWithdrawalReason() : null;

            // 탈퇴 처리
            WithdrawalResponse withdrawalResponse = userWithdrawalService.withdrawUser(userId, withdrawalReason);

            // 현재 사용자의 토큰 무효화 처리
            tokenService.deleteRefreshToken(userId);
            
            String token = bearerToken.replace("Bearer ", "");
            try {
                long remainTime = jwtUtil.getClaimsIgnoreExpiration(token).getExpiration().getTime() - System.currentTimeMillis();
                if (remainTime > 0) {
                    tokenService.blacklistAccessToken(token, remainTime);
                }
            } catch (Exception e) {
                log.warn("토큰 블랙리스트 처리 실패: {}", e.getMessage());
            }

            log.info("회원 탈퇴 완료: userId={}", userId);

            return ResponseEntity.ok(
                    BaseResponse.success(withdrawalResponse, "회원 탈퇴가 완료되었습니다.")
            );
            
        } catch (com.example.auth.exception.WithdrawalException.WithdrawalNotAllowedException e) {
            log.warn("탈퇴 불가 - userId: {}, 사유: {}", 
                    tokenUtils.getUserIdFromToken(bearerToken), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "WITHDRAWAL_NOT_ALLOWED", HttpStatus.BAD_REQUEST.value()));
                    
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰으로 회원 탈퇴 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
                    
        } catch (JwtValidationException e) {
            log.warn("JWT 검증 오류 - 회원 탈퇴: {}, 타입: {}", e.getMessage(), e.getErrorType());

            String errorCode = "UNAUTHORIZED";
            if (e.getErrorType() == TokenErrorType.EXPIRED) {
                errorCode = "TOKEN_EXPIRED";
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
                    
        } catch (Exception e) {
            log.error("회원 탈퇴 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * 토큰을 마스킹하여 로그에 안전하게 출력
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 20) {
            return "invalid-token";
        }
        token = token.replace("Bearer ", "");
        return token.substring(0, 10) + "..." + token.substring(token.length() - 5);
    }

}
