package com.example.auth.controller;

import com.example.auth.service.*;
import com.example.auth.dto.consent.ConsentRequest;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.example.auth.common.BaseResponse;
import com.example.auth.dto.auth.EmailLoginRequest;
import com.example.auth.dto.auth.EmailSignupRequest;
import com.example.auth.dto.KakaoAuthRequest;
import com.example.auth.dto.KakaoTokenResponse;
import com.example.auth.dto.KakaoUserInfo;
import com.example.auth.dto.RefreshTokenRequest;
import com.example.auth.dto.UserDTO;
import com.example.auth.dto.UserLoginResult;
import com.example.auth.dto.response.ApiResponseSchemas;
import com.example.auth.domain.User;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.exception.TokenErrorType;
import com.example.auth.exception.TokenRefreshException;
import com.example.auth.security.JwtUtil;
import com.example.auth.util.TokenUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "로그인 / 로그아웃 / 재발급 관련 API")
public class AuthController {

    private final KakaoService kakaoService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final TokenUtils tokenUtils;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final UserWithdrawalService userWithdrawalService;
    private final AuthCodeCacheService authCodeCacheService;
    private final ConsentService consentService;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Operation(summary = "카카오 로그인 리다이렉트", description = "프론트에서 받은 redirectUri를 기반으로 카카오 로그인 페이지로 직접 리다이렉트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "카카오 로그인 페이지로 리다이렉트"),
            @ApiResponse(responseCode = "400", description = "허용되지 않은 redirectUri",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/login-redirect")
    public void redirectToKakaoLogin(
            @RequestParam("redirectUri") String redirectUri,
            HttpServletResponse response
    ) throws IOException {
        List<String> allowedUris = List.of(
                "http://localhost:3000/login/oauth2/code/kakao",
                "https://chkok.kr/login/oauth2/code/kakao"
        );

        if (!allowedUris.contains(redirectUri)) {
            log.warn("허용되지 않은 redirectUri: {}", redirectUri);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "허용되지 않은 redirectUri입니다.");
            return;
        }

        String kakaoUrl = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .build().toUriString();

        log.info("카카오 로그인 페이지로 리다이렉트: redirectUri={}", redirectUri);
        response.sendRedirect(kakaoUrl);
    }
    @Operation(
            summary = "카카오 로그인",
            description = "카카오 OAuth 인가 코드를 통해 로그인하고 JWT 토큰을 발급받습니다.\n\n" +
                    "### 사용법:\n" +
                    "1. 카카오 로그인 페이지에서 사용자 인증\n" +
                    "2. 리다이렉트 URI로 인가 코드 수신\n" +
                    "3. 인가 코드와 리다이렉트 URI를 이 API에 전송\n" +
                    "4. 응답으로 JWT 토큰과 사용자 정보 수신"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "카카오 로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseSchemas.LoginSuccessResponse.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "신규 회원 (동의 필요)",
                                            summary = "신규 사용자가 처음 로그인하는 경우",
                                            value = """
                            {
                              "success": true,
                              "message": "동의가 필요합니다",
                              "status": 200,
                              "data": {
                                "loginType": "consentRequired",
                                "tempToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMiLCJpYXQiOjE2NzAyNjUyMDAsImV4cCI6MTY3MDI2ODgwMH0.abc123",
                                "user": {
                                  "id": 123,
                                  "email": "newuser@example.com",
                                  "nickname": "신규사용자123",
                                  "role": "USER",
                                  "profileImage": "https://k.kakaocdn.net/dn/profile/image.jpg"
                                }
                              }
                            }
                            """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "기존 사용자 로그인",
                                            summary = "기존 사용자가 로그인하는 경우",
                                            value = """
                            {
                              "success": true,
                              "message": "카카오 로그인 성공",
                              "status": 200,
                              "data": {
                                "loginType": "login",
                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0NTYiLCJpYXQiOjE2NzAyNjUyMDAsImV4cCI6MTY3MDI2ODgwMH0.xyz789",
                                "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0NTYiLCJpYXQiOjE2NzAyNjUyMDAsImV4cCI6MTY3MTEyOTIwMH0.abc987",
                                "user": {
                                  "id": 456,
                                  "email": "existinguser@example.com",
                                  "nickname": "기존사용자456",
                                  "role": "CLIENT",
                                  "profileImage": "https://example.com/custom-profile.jpg"
                                }
                              }
                            }
                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 요청 (허용되지 않은 redirectUri 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (잘못된 인가 코드 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            )
    })
    @PostMapping("/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestBody @Valid KakaoAuthRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("카카오 로그인 요청 시작: redirectUri={}, timestamp={}", request.getRedirectUri(), startTime);

        // 허용된 리다이렉트 URI인지 검증
        List<String> allowedUris = List.of(
                "http://localhost:3000/login/oauth2/code/kakao",
                "https://chkok.kr/login/oauth2/code/kakao"
        );

        if (!allowedUris.contains(request.getRedirectUri())) {
            log.warn("허용되지 않은 redirectUri: {}", request.getRedirectUri());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail("허용되지 않은 redirectUri입니다.", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST.value()));
        }

        // 인가 코드 기본 검증
        String authCode = request.getAuthorizationCode();
        if (authCode == null || authCode.trim().isEmpty()) {
            log.warn("빈 인가 코드로 카카오 로그인 시도");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail("유효하지 않은 인가 코드입니다.", "INVALID_AUTH_CODE", HttpStatus.BAD_REQUEST.value()));
        }

        // 인가 코드 길이 검증 (카카오 인가 코드는 일반적으로 30자 이상)
        if (authCode.length() < 20) {
            log.warn("너무 짧은 인가 코드: length={}", authCode.length());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail("유효하지 않은 인가 코드 형식입니다.", "INVALID_AUTH_CODE_FORMAT", HttpStatus.BAD_REQUEST.value()));
        }

        // 인가 코드 중복 사용 검증
        if (authCodeCacheService.isAuthCodeUsed(authCode)) {
            log.warn("이미 사용된 인가 코드로 로그인 시도: code={}", authCode);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail("이미 사용된 인가 코드입니다. 다시 로그인해주세요.", "AUTH_CODE_ALREADY_USED", HttpStatus.BAD_REQUEST.value()));
        }

        try {
            log.debug("카카오 토큰 요청 시작: timestamp={}", System.currentTimeMillis());
            KakaoTokenResponse kakaoToken = kakaoService.requestToken(request.getAuthorizationCode(), request.getRedirectUri());
            log.debug("카카오 토큰 요청 완료: timestamp={}", System.currentTimeMillis());

            // 인가 코드 사용 처리 (성공 시에만)
            authCodeCacheService.markAuthCodeAsUsed(authCode);

            log.debug("카카오 사용자 정보 요청 시작: timestamp={}", System.currentTimeMillis());
            KakaoUserInfo userInfo = kakaoService.requestUserInfo(kakaoToken.accessToken());
            log.debug("카카오 사용자 정보 요청 완료: timestamp={}", System.currentTimeMillis());

            // 재가입 제한 체크
            String email = null;
            if (userInfo.kakao_account() != null) {
                email = (String) userInfo.kakao_account().get("email");
            }

            userWithdrawalService.checkWithdrawalRestriction(
                    email,
                    String.valueOf(userInfo.id()),
                    "kakao"
            );

            // 사용자 조회 또는 생성
            UserLoginResult result = userService.findOrCreateUser("kakao", userInfo);
            User user = result.user();


            // 신규 회원인 경우 동의가 필요함
            if (result.isNew()) {
                log.info("신규 회원 - 동의 필요: tempUserId={}", result.tempUserId());

                // 임시 토큰 생성 (tempUserId 사용, 10분 유효)
                String tempToken = jwtUtil.createTempToken(result.tempUserId());

                UserDTO userDTO = UserDTO.fromEntity(user);

                Map<String, Object> responseData = Map.of(
                        "loginType", "consentRequired",
                        "tempToken", tempToken,
                        "user", userDTO
                );

                log.info("신규 회원 임시 토큰 발급 완료: tempUserId={}, 총 소요시간={}ms",
                        result.tempUserId(), System.currentTimeMillis() - startTime);

                return ResponseEntity.ok(BaseResponse.success(responseData, "동의가 필요합니다"));
            }

            // 기존 회원인 경우 바로 로그인
            log.info("기존 회원 로그인: userId={}", user.getId());

            String accessToken = jwtUtil.createAccessToken(user.getId());
            String refreshToken = jwtUtil.createRefreshToken(user.getId());
            tokenService.saveRefreshToken(user.getId(), refreshToken);

            log.info("카카오 로그인 성공: userId={}, loginType=login, 총 소요시간={}ms",
                    user.getId(), System.currentTimeMillis() - startTime);

            // UserDTO를 사용하여 응답 데이터 생성
            UserDTO userDTO = UserDTO.fromEntity(user);

            Map<String, Object> responseData = Map.of(
                    "loginType", "login",
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "user", userDTO
            );

            return ResponseEntity.ok(BaseResponse.success(responseData, "카카오 로그인 성공"));
        } catch (com.example.auth.exception.WithdrawalException.RejoinRestrictionException e) {
            log.warn("재가입 제한으로 카카오 로그인 거부: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "REJOIN_RESTRICTED", HttpStatus.FORBIDDEN.value()));
        } catch (RuntimeException e) {
            // KakaoService에서 발생하는 타임아웃 및 연결 오류 처리
            if (e.getMessage().contains("응답 시간이 초과") || e.getMessage().contains("timeout") || e.getMessage().contains("408")) {
                log.error("카카오 로그인 타임아웃 오류: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                        .body(BaseResponse.fail("카카오 로그인 처리 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.", "REQUEST_TIMEOUT", HttpStatus.REQUEST_TIMEOUT.value()));
            } else if (e.getMessage().contains("인가 코드가 만료") || e.getMessage().contains("이미 사용된 코드")) {
                log.error("카카오 인가 코드 만료/중복 사용: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("카카오 로그인 세션이 만료되었습니다. 다시 로그인해주세요.", "AUTHORIZATION_CODE_EXPIRED", HttpStatus.BAD_REQUEST.value()));
            } else if (e.getMessage().contains("앱 설정에 오류") || e.getMessage().contains("관리자에게 문의")) {
                log.error("카카오 앱 설정 오류: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(BaseResponse.fail("카카오 로그인 설정에 문제가 있습니다. 관리자에게 문의해주세요.", "KAKAO_CONFIG_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
            } else if (e.getMessage().contains("요청 형식이 잘못")) {
                log.error("카카오 요청 형식 오류: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("로그인 요청 형식에 오류가 있습니다. 다시 시도해주세요.", "INVALID_REQUEST_FORMAT", HttpStatus.BAD_REQUEST.value()));
            } else if (e.getMessage().contains("카카오 인증 서버")) {
                log.error("카카오 서버 연결 오류: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(BaseResponse.fail("카카오 로그인 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.", "SERVICE_UNAVAILABLE", HttpStatus.BAD_GATEWAY.value()));
            }
            log.error("카카오 로그인 런타임 오류: {}", e.getMessage(), e);
            throw e; // 다른 RuntimeException은 일반 Exception 핸들러로 전달
        } catch (Exception e) {
            log.error("카카오 로그인 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("카카오 로그인 처리 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "로그아웃",
        description = "현재 사용자의 액세스 토큰을 블랙리스트에 추가하고 리프레시 토큰을 제거합니다.\n\n" +
                      "### 참고:\n" +
                      "- 로그아웃 후 해당 토큰으로는 더 이상 API를 사용할 수 없습니다\n" +
                      "- 새로운 로그인이 필요합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "로그아웃 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(ref = "#/components/schemas/ApiSuccessResponse")
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "토큰 인증 실패 또는 만료됨",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String bearerToken) {
        // 토큰 형식 확인
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            log.warn("유효하지 않은 토큰 형식으로 로그아웃 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("유효하지 않은 토큰 형식입니다.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        }

        String token = bearerToken.replace("Bearer ", "");

        // 이미 블랙리스트된 토큰인지 확인
        if (tokenService.isBlacklisted(token)) {
            log.warn("이미 로그아웃된 토큰으로 로그아웃 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("이미 로그아웃된 토큰입니다.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        }

        try {
            // 토큰 유효성 검증 및 사용자 ID 추출
            Claims claims = jwtUtil.getClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            long remainTime = claims.getExpiration().getTime() - System.currentTimeMillis();

            // 토큰 블랙리스트 처리 및 리프레시 토큰 제거
            tokenService.blacklistAccessToken(token, remainTime);
            tokenService.deleteRefreshToken(userId);

            log.info("로그아웃 완료: userId={}", userId);

            return ResponseEntity.ok(BaseResponse.success(null, "로그아웃 완료"));
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰으로 로그아웃 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        } catch (JwtValidationException e) {
            log.warn("JWT 검증 오류 - 로그아웃: {}, 타입: {}", e.getMessage(), e.getErrorType());

            String errorCode = "UNAUTHORIZED";
            if (e.getErrorType() == TokenErrorType.EXPIRED) {
                errorCode = "TOKEN_EXPIRED";
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("유효하지 않은 토큰입니다.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        }
    }

    @Operation(
        summary = "토큰 재발급",
        description = "만료된 액세스 토큰을 리프레시 토큰으로 재발급합니다.\n\n" +
                      "### 사용법:\n" +
                      "1. 만료된 액세스 토큰을 Authorization 헤더에 포함\n" +
                      "2. 유효한 리프레시 토큰을 요청 본문에 포함\n" +
                      "3. 새로운 액세스 토큰과 리프레시 토큰 수신"
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "토큰 재발급 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponseSchemas.TokenRefreshResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "리프레시 토큰 유효하지 않음",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                )
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버 오류",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody @Valid RefreshTokenRequest request
    ) {
        log.info("토큰 재발급 요청");

        try {
            String accessToken = bearerToken.replace("Bearer ", "");

            // 토큰 만료 여부 상관없이 클레임 추출
            Claims claims;
            try {
                claims = jwtUtil.getClaimsIgnoreExpiration(accessToken);
            } catch (Exception e) {
                log.warn("액세스 토큰 파싱 실패: {}", e.getMessage());
                throw new TokenRefreshException("유효하지 않은 액세스 토큰입니다.", "UNAUTHORIZED");
            }

            Long userId = Long.valueOf(claims.getSubject());

            // 블랙리스트 체크
            if (tokenService.isBlacklisted(accessToken)) {
                log.warn("블랙리스트에 있는 토큰으로 재발급 시도: userId={}", userId);
                throw new TokenRefreshException("로그아웃된 토큰입니다.", "UNAUTHORIZED");
            }

            // Redis에 저장된 리프레시 토큰 검증
            String savedRefresh = tokenService.getRefreshToken(userId);
            if (savedRefresh == null) {
                log.warn("저장된 리프레시 토큰 없음: userId={}", userId);
                throw new TokenRefreshException("리프레시 토큰이 만료되었습니다.", "INVALID_REFRESH_TOKEN");
            }

            if (!request.getRefreshToken().equals(savedRefresh)) {
                log.warn("유효하지 않은 리프레시 토큰: userId={}", userId);
                throw new TokenRefreshException("리프레시 토큰이 유효하지 않습니다.", "INVALID_REFRESH_TOKEN");
            }

            // 새 토큰 발급
            String newAccess = jwtUtil.createAccessToken(userId);
            String newRefresh = jwtUtil.createRefreshToken(userId);
            tokenService.saveRefreshToken(userId, newRefresh);

            log.info("토큰 재발급 성공: userId={}", userId);

            Map<String, Object> data = Map.of(
                    "accessToken", newAccess,
                    "refreshToken", newRefresh
            );

            return ResponseEntity.ok(BaseResponse.success(data, "토큰이 성공적으로 재발급되었습니다."));
        } catch (TokenRefreshException e) {
            if ("INVALID_REFRESH_TOKEN".equals(e.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail(e.getMessage(), "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED.value()));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail(e.getMessage(), e.getErrorCode(), HttpStatus.UNAUTHORIZED.value()));
            }
        } catch (Exception e) {
            log.error("토큰 재발급 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("토큰 재발급 중 오류가 발생했습니다.", "TOKEN_REFRESH_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(summary = "CLIENT 권한 검사", description = "현재 사용자가 CLIENT 권한을 가지고 있는지 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "권한 검사 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiSuccessResponse"))),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "CLIENT 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/check-client")
    public ResponseEntity<?> checkClientRole(@RequestHeader("Authorization") String bearerToken) {
        // 토큰 형식 확인
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            log.warn("유효하지 않은 토큰 형식으로 CLIENT 권한 검사 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("유효하지 않은 토큰 형식입니다.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        }

        String token = bearerToken.replace("Bearer ", "");

        // 블랙리스트 토큰 확인
        if (tokenService.isBlacklisted(token)) {
            log.warn("블랙리스트된 토큰으로 CLIENT 권한 검사 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        }

        try {
            // 토큰 유효성 검증 및 사용자 ID 추출
            Claims claims = jwtUtil.getClaims(token);
            Long userId = Long.valueOf(claims.getSubject());

            // 사용자 정보 조회
            User user = userService.findUserById(userId);
            if (user == null) {
                log.warn("존재하지 않는 사용자로 CLIENT 권한 검사 시도: userId={}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail("존재하지 않는 사용자입니다.", "USER_NOT_FOUND", HttpStatus.UNAUTHORIZED.value()));
            }

            // CLIENT 권한 확인
            boolean isClient = "CLIENT".equals(user.getRole());
            
            log.info("CLIENT 권한 검사 완료: userId={}, isClient={}, userRole={}", userId, isClient, user.getRole());

            if (!isClient) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(BaseResponse.fail("CLIENT 권한이 필요합니다.", "INSUFFICIENT_PERMISSION", HttpStatus.FORBIDDEN.value()));
            }

            return ResponseEntity.ok(BaseResponse.success(null, "CLIENT 권한 확인 완료"));

        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰으로 CLIENT 권한 검사 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        } catch (JwtValidationException e) {
            log.warn("JWT 검증 오류 - CLIENT 권한 검사: {}, 타입: {}", e.getMessage(), e.getErrorType());

            String errorCode = "UNAUTHORIZED";
            if (e.getErrorType() == TokenErrorType.EXPIRED) {
                errorCode = "TOKEN_EXPIRED";
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("CLIENT 권한 검사 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("권한 검사 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "이메일 회원가입",
        description = "이메일과 비밀번호로 새 계정을 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "201", 
                description = "회원가입 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponseSchemas.LoginSuccessResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "409", 
                description = "이미 존재하는 이메일",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                )
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<?> emailSignup(@RequestBody @Valid EmailSignupRequest request) {
        log.info("이메일 회원가입 요청: email={}", request.getEmail());

        try {
            // 재가입 제한 체크
            userWithdrawalService.checkWithdrawalRestriction(
                request.getEmail(), 
                request.getEmail(), 
                "LOCAL"
            );
            
            // 이미 존재하는 이메일인지 확인
            User existingUser = userService.findByEmail(request.getEmail());
            if (existingUser != null) {
                log.warn("이미 존재하는 이메일로 회원가입 시도: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(BaseResponse.fail("이미 존재하는 이메일입니다.", "EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT.value()));
            }

            // 닉네임 중복 확인
            if (userService.isNicknameExists(request.getNickname())) {
                log.warn("이미 존재하는 닉네임으로 회원가입 시도: {}", request.getNickname());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(BaseResponse.fail("이미 존재하는 닉네임입니다.", "NICKNAME_ALREADY_EXISTS", HttpStatus.CONFLICT.value()));
            }

            // 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(request.getPassword());

            // 새 사용자 생성
            User newUser = User.builder()
                    .provider("LOCAL")
                    .socialId(request.getEmail()) // 이메일을 socialId로 사용
                    .email(request.getEmail())
                    .nickname(request.getNickname())
                    .password(encodedPassword)
                    .accountType("LOCAL")
                    .emailVerified(true)
                    .active(true)
                    .role("USER")
                    .build();

            User savedUser = userService.saveUser(newUser);
            
            // JWT 토큰 생성
            String accessToken = jwtUtil.createAccessToken(savedUser.getId());
            String refreshToken = jwtUtil.createRefreshToken(savedUser.getId());
            tokenService.saveRefreshToken(savedUser.getId(), refreshToken);

            log.info("이메일 회원가입 성공: userId={}", savedUser.getId());

            UserDTO userDTO = UserDTO.fromEntity(savedUser);

            Map<String, Object> responseData = Map.of(
                    "loginType", "registration",
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "user", userDTO
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success(responseData, "회원가입 및 로그인 성공"));

        } catch (com.example.auth.exception.WithdrawalException.RejoinRestrictionException e) {
            log.warn("재가입 제한으로 이메일 회원가입 거부: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "REJOIN_RESTRICTED", HttpStatus.FORBIDDEN.value()));
        } catch (Exception e) {
            log.error("이메일 회원가입 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("회원가입 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "이메일 로그인",
        description = "이메일과 비밀번호로 로그인합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "로그인 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponseSchemas.LoginSuccessResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "이메일 또는 비밀번호가 잘못됨",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<?> emailLogin(@RequestBody @Valid EmailLoginRequest request) {
        log.info("이메일 로그인 요청: email={}", request.getEmail());

        try {
            // 재가입 제한 체크
            userWithdrawalService.checkWithdrawalRestriction(
                request.getEmail(), 
                request.getEmail(), 
                "LOCAL"
            );
            
            // 사용자 조회
            User user = userService.findByEmail(request.getEmail());
            if (user == null) {
                log.warn("존재하지 않는 이메일로 로그인 시도: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail("이메일 또는 비밀번호가 잘못되었습니다.", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED.value()));
            }

            // 이메일 계정인지 확인
            if (!"LOCAL".equals(user.getProvider()) && !"LOCAL".equals(user.getAccountType())) {
                log.warn("소셜 계정으로 이메일 로그인 시도: email={}, provider={}", request.getEmail(), user.getProvider());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail("소셜 로그인 계정입니다. 카카오 로그인을 사용해주세요.", "SOCIAL_ACCOUNT", HttpStatus.UNAUTHORIZED.value()));
            }

            // 비밀번호 확인
            if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("잘못된 비밀번호로 로그인 시도: email={}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail("이메일 또는 비밀번호가 잘못되었습니다.", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED.value()));
            }

            // 계정 상태 확인
            if (!user.getActive()) {
                log.warn("비활성화된 계정으로 로그인 시도: email={}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail("비활성화된 계정입니다.", "ACCOUNT_DISABLED", HttpStatus.UNAUTHORIZED.value()));
            }

            // JWT 토큰 생성
            String accessToken = jwtUtil.createAccessToken(user.getId());
            String refreshToken = jwtUtil.createRefreshToken(user.getId());
            tokenService.saveRefreshToken(user.getId(), refreshToken);

            log.info("이메일 로그인 성공: userId={}", user.getId());

            UserDTO userDTO = UserDTO.fromEntity(user);

            Map<String, Object> responseData = Map.of(
                    "loginType", "login",
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "user", userDTO
            );

            return ResponseEntity.ok(BaseResponse.success(responseData, "로그인 성공"));

        } catch (com.example.auth.exception.WithdrawalException.RejoinRestrictionException e) {
            log.warn("재가입 제한으로 이메일 로그인 거부: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail(e.getMessage(), "REJOIN_RESTRICTED", HttpStatus.FORBIDDEN.value()));
        } catch (Exception e) {
            log.error("이메일 로그인 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("로그인 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(summary = "USER 권한 검사", description = "현재 사용자가 USER 권한을 가지고 있는지 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "권한 검사 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiSuccessResponse"))),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "USER 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/check-user")
    public ResponseEntity<?> checkUserRole(@RequestHeader("Authorization") String bearerToken) {
        // 토큰 형식 확인
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            log.warn("유효하지 않은 토큰 형식으로 CLIENT 권한 검사 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("유효하지 않은 토큰 형식입니다.", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        }

        String token = bearerToken.replace("Bearer ", "");

        // 블랙리스트 토큰 확인
        if (tokenService.isBlacklisted(token)) {
            log.warn("블랙리스트된 토큰으로 USER 권한 검사 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        }

        try {
            // 토큰 유효성 검증 및 사용자 ID 추출
            Claims claims = jwtUtil.getClaims(token);
            Long userId = Long.valueOf(claims.getSubject());

            // 사용자 정보 조회
            User user = userService.findUserById(userId);
            if (user == null) {
                log.warn("존재하지 않는 사용자로 USER 권한 검사 시도: userId={}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail("존재하지 않는 사용자입니다.", "USER_NOT_FOUND", HttpStatus.UNAUTHORIZED.value()));
            }

            // USER 권한 확인
            boolean isUser = "USER".equals(user.getRole());

            log.info("USER 권한 검사 완료: userId={}, isUser={}, userRole={}", userId, isUser, user.getRole());

            if (!isUser) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(BaseResponse.fail("USER 권한이 필요합니다.", "INSUFFICIENT_PERMISSION", HttpStatus.FORBIDDEN.value()));
            }

            return ResponseEntity.ok(BaseResponse.success(null, "USER 권한 확인 완료"));

        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰으로 USER 권한 검사 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("만료된 토큰입니다.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        } catch (JwtValidationException e) {
            log.warn("JWT 검증 오류 - USER 권한 검사: {}, 타입: {}", e.getMessage(), e.getErrorType());

            String errorCode = "UNAUTHORIZED";
            if (e.getErrorType() == TokenErrorType.EXPIRED) {
                errorCode = "TOKEN_EXPIRED";
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("USER 권한 검사 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("권한 검사 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    @Operation(
            summary = "동의 완료",
            description = "신규 회원의 약관 동의를 처리하고 정식 토큰을 발급합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "동의 완료 및 회원가입 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseSchemas.LoginSuccessResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "필수 동의 항목 미체크",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 임시 토큰",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
                    )
            )
    })
    @PostMapping("/consent")
    public ResponseEntity<?> completeConsent(
            @RequestBody @Valid ConsentRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("동의 완료 요청");

        try {
            // 1. 임시 토큰 검증
            String tempToken = request.getTempToken();
            Claims claims;
            try {
                claims = jwtUtil.getClaims(tempToken);
            } catch (Exception e) {
                log.warn("유효하지 않은 임시 토큰: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail("유효하지 않거나 만료된 임시 토큰입니다.", "INVALID_TEMP_TOKEN", HttpStatus.UNAUTHORIZED.value()));
            }

            // 2. tempUserId 추출 (String 타입!)
            String tempUserId = claims.getSubject();

            log.info("임시 토큰 검증 성공: tempUserId={}", tempUserId);

            // 3. 필수 동의 항목 검증
            Map<String, Boolean> agreements = request.getAgreements();
            Boolean termsAgreed = agreements.get("termsAgreed");
            Boolean privacyPolicyAgreed = agreements.get("privacyPolicyAgreed");

            if (termsAgreed == null || !termsAgreed) {
                log.warn("서비스 이용약관 미동의: tempUserId={}", tempUserId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("서비스 이용약관에 동의해주세요.", "TERMS_NOT_AGREED", HttpStatus.BAD_REQUEST.value()));
            }

            if (privacyPolicyAgreed == null || !privacyPolicyAgreed) {
                log.warn("개인정보 처리방침 미동의: tempUserId={}", tempUserId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("개인정보 처리방침에 동의해주세요.", "PRIVACY_NOT_AGREED", HttpStatus.BAD_REQUEST.value()));
            }

            // 4. Redis에서 임시 데이터 가져와서 DB에 정식 사용자 생성
            User savedUser = consentService.createUserFromTempData(tempUserId, agreements, httpRequest);

            // 5. 정식 JWT 토큰 발급
            String accessToken = jwtUtil.createAccessToken(savedUser.getId());
            String refreshToken = jwtUtil.createRefreshToken(savedUser.getId());
            tokenService.saveRefreshToken(savedUser.getId(), refreshToken);

            log.info("동의 완료 및 회원가입 성공: userId={}, tempUserId={}", savedUser.getId(), tempUserId);

            UserDTO userDTO = UserDTO.fromEntity(savedUser);

            Map<String, Object> responseData = Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "user", userDTO
            );

            return ResponseEntity.ok(BaseResponse.success(responseData, "동의 완료"));

        } catch (ExpiredJwtException e) {
            log.warn("만료된 임시 토큰: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail("임시 토큰이 만료되었습니다. 다시 로그인해주세요.", "TEMP_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
        } catch (RuntimeException e) {
            // Redis에 데이터 없음 (만료됨)
            if (e.getMessage().contains("세션이 만료")) {
                log.warn("Redis 임시 데이터 만료: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(BaseResponse.fail("세션이 만료되었습니다. 다시 로그인해주세요.", "TEMP_SESSION_EXPIRED", HttpStatus.UNAUTHORIZED.value()));
            }
            throw e;
        } catch (Exception e) {
            log.error("동의 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("동의 처리 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
