package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.constant.PlatformType;
import com.example.auth.domain.UserSnsPlatform;
import com.example.auth.dto.ApiResponseSchema;
import com.example.auth.dto.FollowerCountUpdateRequest;
import com.example.auth.dto.PlatformConnectRequest;
import com.example.auth.repository.UserSnsPlatformRepository;
import com.example.auth.service.InstagramConnectService;
import com.example.auth.service.NaverBlogConnectService;
import com.example.auth.service.SnsCrawlService;
import com.example.auth.service.YouTubeConnectService;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "SNS 플랫폼 연동 API", description = "SNS 계정 연동 관련 API")
@RestController
@RequestMapping("/api/platforms")
@RequiredArgsConstructor
public class UserSnsPlatformController {

    private final TokenUtils tokenUtils;
    private final UserSnsPlatformRepository userSnsPlatformRepository;
    private final SnsCrawlService snsCrawlService;
    
    // 서비스 주입
    private final NaverBlogConnectService naverBlogConnectService;
    private final InstagramConnectService instagramConnectService;
    private final YouTubeConnectService youtubeConnectService;
    
    // 모든 연동된 플랫폼 목록 조회 API
    @Operation(summary = "연동된 SNS 플랫폼 목록 조회", description = "사용자가 연동한 모든 SNS 플랫폼 목록을 조회합니다. 팔로워 수는 수동으로 업데이트해야 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "플랫폼 목록 조회 성공",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/BaseResponseSuccess"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/BaseResponseError"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/BaseResponseError")))
    })
    @GetMapping
    public ResponseEntity<?> getUserPlatforms(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            List<UserSnsPlatform> platforms = userSnsPlatformRepository.findByUserId(userId);

            // DTO로 변환하여 반환
            List<Map<String, Object>> platformList = platforms.stream()
                    .map(platform -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", platform.getId());
                        // platformType을 대문자로 변환하여 반환
                        String platformType = platform.getPlatformType();
                        try {
                            // PlatformType enum으로 변환 후 name()을 사용하여 대문자 이름 가져오기
                            map.put("platformType", PlatformType.fromString(platformType).name());
                        } catch (Exception e) {
                            // 변환 중 오류 발생 시 원본 값 대문자로 변환하여 반환
                            map.put("platformType", platformType != null ? platformType.toUpperCase() : null);
                        }
                        map.put("accountUrl", platform.getAccountUrl());
                        map.put("followerCount", platform.getFollowerCount() != null ? platform.getFollowerCount() : 0);
                        map.put("lastCrawledAt", platform.getLastCrawledAt() != null ?
                                platform.getLastCrawledAt().toString() : null);
                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("platforms", platformList);

            return ResponseEntity.ok(BaseResponse.success(responseData, "SNS 플랫폼 목록 조회 성공"));
        } catch (Exception e) {
            log.error("SNS 플랫폼 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "PLATFORM_LIST_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // 팔로워 수 수동 업데이트 API
    @Operation(summary = "SNS 플랫폼 팔로워 수 수동 업데이트", description = "연동된 SNS 플랫폼의 팔로워 수를 수동으로 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팔로워 수 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "404", description = "플랫폼을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @PutMapping("/{platformId}/follower-count")
    public ResponseEntity<?> updateFollowerCount(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "플랫폼 ID", required = true, example = "1")
            @PathVariable Long platformId,
            @Parameter(description = "팔로워 수 업데이트 요청", required = true)
            @RequestBody @Valid FollowerCountUpdateRequest request
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            // 플랫폼 소유권 확인
            UserSnsPlatform platform = userSnsPlatformRepository.findByUserIdAndId(userId, platformId)
                    .orElseThrow(() -> new RuntimeException("연동된 플랫폼을 찾을 수 없습니다."));
            
            // 팔로워 수 업데이트
            snsCrawlService.updateFollowerCount(platformId, request.getFollowerCount());
            
            Map<String, Object> responseData = Map.of(
                    "platformId", platformId,
                    "followerCount", request.getFollowerCount(),
                    "message", "팔로워 수가 성공적으로 업데이트되었습니다."
            );
            
            return ResponseEntity.ok(BaseResponse.success(responseData, "팔로워 수 업데이트 성공"));
        } catch (Exception e) {
            log.error("팔로워 수 업데이트 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "FOLLOWER_COUNT_UPDATE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // 통합 플랫폼 연동 API
    @Operation(summary = "SNS 플랫폼 연동", description = "플랫폼 타입(BLOG, INSTAGRAM, YOUTUBE)과 URL을 입력받아 연동합니다. (팔로워 수는 자동으로 수집되지 않으며 수동 업데이트가 필요합니다.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SNS 플랫폼 연동 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잘못된 URL 형식 등)",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @PostMapping("/connect")
    public ResponseEntity<?> connectPlatform(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "SNS 플랫폼 연동 요청", required = true)
            @RequestBody @Valid PlatformConnectRequest request
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            Long platformId;
            String platformName;
            
            switch (request.getType()) {
                case "BLOG":
                    platformId = naverBlogConnectService.connect(userId, request.getUrl());
                    platformName = "네이버 블로그";
                    break;
                case "INSTAGRAM":
                    platformId = instagramConnectService.connect(userId, request.getUrl());
                    platformName = "인스타그램";
                    break;
                case "YOUTUBE":
                    platformId = youtubeConnectService.connect(userId, request.getUrl());
                    platformName = "유튜브 채널";
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(BaseResponse.fail("지원하지 않는 플랫폼 타입입니다.", "UNSUPPORTED_PLATFORM", HttpStatus.BAD_REQUEST.value()));
            }
            
            Map<String, Object> responseData = Map.of(
                    "platformId", platformId,
                    "platformType", request.getType(),
                    "message", platformName + " 연동이 완료되었습니다. (팔로워 수는 수동 업데이트가 필요합니다.)"
            );
            
            return ResponseEntity.ok(BaseResponse.success(responseData, platformName + " 연동 성공"));
        } catch (Exception e) {
            log.error("SNS 플랫폼 연동 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "PLATFORM_CONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    // 네이버 블로그 연동 API (Deprecated)
    @Deprecated
    @Hidden
    @Operation(summary = "네이버 블로그 연동 (Deprecated)", description = "네이버 블로그 URL을 입력받아 연동합니다. 대신 /api/platforms/connect API를 사용하세요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "네이버 블로그 연동 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잘못된 URL 형식 등)",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @PostMapping("/blog/connect")
    public ResponseEntity<?> connectNaverBlog(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "네이버 블로그 연동 요청", required = true)
            @RequestBody @Valid PlatformConnectRequest request
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            Long platformId = naverBlogConnectService.connect(userId, request.getUrl());
            
            Map<String, Object> responseData = Map.of(
                    "platformId", platformId,
                    "message", "네이버 블로그 연동이 완료되었습니다. (팔로워 수는 수동 업데이트가 필요합니다.)"
            );
            
            return ResponseEntity.ok(BaseResponse.success(responseData, "네이버 블로그 연동 성공"));
        } catch (Exception e) {
            log.error("네이버 블로그 연동 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "BLOG_CONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    // 통합 플랫폼 연동 해제 API
    @Operation(summary = "SNS 플랫폼 연동 해제", description = "연동된 SNS 플랫폼을 해제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SNS 플랫폼 연동 해제 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "404", description = "연동된 플랫폼을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @DeleteMapping("/{platformId}")
    public ResponseEntity<?> disconnectPlatform(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "플랫폼 ID", required = true, example = "1")
            @PathVariable Long platformId
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            // 플랫폼 정보 조회
            UserSnsPlatform platform = userSnsPlatformRepository.findByUserIdAndId(userId, platformId)
                    .orElseThrow(() -> new RuntimeException("연동된 플랫폼을 찾을 수 없습니다."));
            
            String platformType = platform.getPlatformType();
            String platformName;
            
            // 플랫폼 타입에 따라 적절한 서비스로 연동 해제 처리
            switch (platformType) {
                case "BLOG":
                    naverBlogConnectService.disconnect(userId, platformId);
                    platformName = "네이버 블로그";
                    break;
                case "INSTAGRAM":
                    instagramConnectService.disconnect(userId, platformId);
                    platformName = "인스타그램";
                    break;
                case "YOUTUBE":
                    youtubeConnectService.disconnect(userId, platformId);
                    platformName = "유튜브 채널";
                    break;
                default:
                    // 알 수 없는 플랫폼 타입인 경우 기본 삭제 처리
                    userSnsPlatformRepository.deleteById(platformId);
                    platformName = "SNS 플랫폼";
            }
            
            return ResponseEntity.ok(BaseResponse.success(null, platformName + " 연동이 해제되었습니다."));
        } catch (Exception e) {
            log.error("SNS 플랫폼 연동 해제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "PLATFORM_DISCONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    // 네이버 블로그 연동 해제 API (Deprecated)
    @Deprecated
    @Hidden
    @Operation(summary = "네이버 블로그 연동 해제 (Deprecated)", description = "연동된 네이버 블로그를 해제합니다. 대신 /api/platforms/{platformId} API를 사용하세요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "네이버 블로그 연동 해제 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "404", description = "연동된 블로그를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @DeleteMapping("/blog/{platformId}")
    public ResponseEntity<?> disconnectNaverBlog(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "플랫폼 ID", required = true, example = "1")
            @PathVariable Long platformId
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            naverBlogConnectService.disconnect(userId, platformId);
            
            return ResponseEntity.ok(BaseResponse.success(null, "네이버 블로그 연동이 해제되었습니다."));
        } catch (Exception e) {
            log.error("네이버 블로그 연동 해제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "BLOG_DISCONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    // 인스타그램 연동 API (Deprecated)
    @Deprecated
    @Hidden
    @Operation(summary = "인스타그램 연동 (Deprecated)", description = "인스타그램 프로필 URL을 입력받아 연동합니다. 대신 /api/platforms/connect API를 사용하세요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인스타그램 연동 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잘못된 URL 형식 등)",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @PostMapping("/instagram/connect")
    public ResponseEntity<?> connectInstagram(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "인스타그램 연동 요청", required = true)
            @RequestBody @Valid PlatformConnectRequest request
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            Long platformId = instagramConnectService.connect(userId, request.getUrl());
            
            Map<String, Object> responseData = Map.of(
                    "platformId", platformId,
                    "message", "인스타그램 연동이 완료되었습니다. (팔로워 수는 수동 업데이트가 필요합니다.)"
            );
            
            return ResponseEntity.ok(BaseResponse.success(responseData, "인스타그램 연동 성공"));
        } catch (Exception e) {
            log.error("인스타그램 연동 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "INSTAGRAM_CONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    // 인스타그램 연동 해제 API (Deprecated)
    @Deprecated
    @Hidden
    @Operation(summary = "인스타그램 연동 해제 (Deprecated)", description = "연동된 인스타그램을 해제합니다. 대신 /api/platforms/{platformId} API를 사용하세요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인스타그램 연동 해제 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "404", description = "연동된 인스타그램을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @DeleteMapping("/instagram/{platformId}")
    public ResponseEntity<?> disconnectInstagram(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "플랫폼 ID", required = true, example = "1")
            @PathVariable Long platformId
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            instagramConnectService.disconnect(userId, platformId);
            
            return ResponseEntity.ok(BaseResponse.success(null, "인스타그램 연동이 해제되었습니다."));
        } catch (Exception e) {
            log.error("인스타그램 연동 해제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "INSTAGRAM_DISCONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    // 유튜브 연동 API (Deprecated)
    @Deprecated
    @Hidden
    @Operation(summary = "유튜브 채널 연동 (Deprecated)", description = "유튜브 채널 URL을 입력받아 연동합니다. 대신 /api/platforms/connect API를 사용하세요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유튜브 채널 연동 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잘못된 URL 형식 등)",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @PostMapping("/youtube/connect")
    public ResponseEntity<?> connectYoutube(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "유튜브 채널 연동 요청", required = true)
            @RequestBody @Valid PlatformConnectRequest request
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            Long platformId = youtubeConnectService.connect(userId, request.getUrl());
            
            Map<String, Object> responseData = Map.of(
                    "platformId", platformId,
                    "message", "유튜브 채널 연동이 완료되었습니다. (구독자 수는 수동 업데이트가 필요합니다.)"
            );
            
            return ResponseEntity.ok(BaseResponse.success(responseData, "유튜브 채널 연동 성공"));
        } catch (Exception e) {
            log.error("유튜브 채널 연동 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "YOUTUBE_CONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    // 유튜브 연동 해제 API (Deprecated)
    @Deprecated
    @Hidden
    @Operation(summary = "유튜브 채널 연동 해제 (Deprecated)", description = "연동된 유튜브 채널을 해제합니다. 대신 /api/platforms/{platformId} API를 사용하세요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유튜브 채널 연동 해제 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "404", description = "연동된 유튜브 채널을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @DeleteMapping("/youtube/{platformId}")
    public ResponseEntity<?> disconnectYoutube(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "플랫폼 ID", required = true, example = "1")
            @PathVariable Long platformId
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            youtubeConnectService.disconnect(userId, platformId);
            
            return ResponseEntity.ok(BaseResponse.success(null, "유튜브 채널 연동이 해제되었습니다."));
        } catch (Exception e) {
            log.error("유튜브 채널 연동 해제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "YOUTUBE_DISCONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    // 모든 플랫폼 연동 해제 API (한 번에 모든 연동을 해제)
    @Operation(summary = "모든 SNS 플랫폼 연동 해제", description = "사용자의 모든 SNS 연동을 해제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "모든 SNS 플랫폼 연동 해제 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Success.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = BaseResponse.Error.class)))
    })
    @DeleteMapping("/all")
    public ResponseEntity<?> disconnectAllPlatforms(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            
            // 사용자의 모든 플랫폼 연동 삭제
            List<UserSnsPlatform> platforms = userSnsPlatformRepository.findByUserId(userId);
            userSnsPlatformRepository.deleteAll(platforms);
            
            int count = platforms.size();
            log.info("사용자의 모든 SNS 연동 해제 완료: userId={}, count={}", userId, count);
            
            Map<String, Object> responseData = Map.of(
                    "count", count,
                    "message", count + "개의 SNS 연동이 모두 해제되었습니다."
            );
            
            return ResponseEntity.ok(BaseResponse.success(responseData, "모든 SNS 플랫폼 연동 해제 성공"));
        } catch (Exception e) {
            log.error("모든 SNS 플랫폼 연동 해제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "ALL_DISCONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}