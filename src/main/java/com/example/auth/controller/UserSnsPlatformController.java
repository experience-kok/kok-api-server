package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.constant.PlatformType;
import com.example.auth.domain.UserSnsPlatform;
import com.example.auth.dto.FollowerCountUpdateRequest;
import com.example.auth.dto.PlatformConnectRequest;
import com.example.auth.repository.UserSnsPlatformRepository;
import com.example.auth.service.InstagramConnectService;
import com.example.auth.service.NaverBlogConnectService;
import com.example.auth.service.SnsCrawlService;
import com.example.auth.service.YouTubeConnectService;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Hidden;
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

    // 전체 플랫폼 목록 조회 API (연동된 것 + 연동 안된 것 모두)
    @Operation(
            summary = "전체 SNS 플랫폼 목록 조회",
            description = "사용자가 연동할 수 있는 모든 SNS 플랫폼 목록을 조회합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- 연동 가능한 모든 플랫폼 목록을 반환합니다\n" +
                    "- 각 플랫폼의 연동 상태(`isConnected`)를 표시합니다\n" +
                    "- 연동된 플랫폼의 경우 추가 정보(계정 URL, 팔로워 수 등)를 제공합니다\n" +
                    "- 연동되지 않은 플랫폼의 경우 기본 정보만 제공합니다\n\n" +
                    "### 응답 구조\n" +
                    "- **연동된 플랫폼**: `isConnected: true`, 실제 데이터 포함\n" +
                    "- **미연동 플랫폼**: `isConnected: false`, 기본값으로 표시\n\n" +
                    "### 활용 예시\n" +
                    "프론트엔드에서 SNS 연동 화면을 구성할 때, 어떤 플랫폼이 연동되어 있고 어떤 플랫폼이 연동 가능한지 한 번에 파악할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "전체 플랫폼 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/AllPlatformListSuccessResponse"),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "전체 플랫폼 목록 조회 성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"전체 SNS 플랫폼 목록 조회 성공\",\n" +
                                            "  \"status\": 200,\n" +
                                            "  \"data\": {\n" +
                                            "    \"platforms\": [\n" +
                                            "      {\n" +
                                            "        \"platformType\": \"INSTAGRAM\",\n" +
                                            "        \"isConnected\": true,\n" +
                                            "        \"id\": 15,\n" +
                                            "        \"accountUrl\": \"https://instagram.com/myaccount\"\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"platformType\": \"YOUTUBE\",\n" +
                                            "        \"isConnected\": false,\n" +
                                            "        \"id\": null,\n" +
                                            "        \"accountUrl\": null\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"platformType\": \"BLOG\",\n" +
                                            "        \"isConnected\": false,\n" +
                                            "        \"id\": null,\n" +
                                            "        \"accountUrl\": null\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"platformType\": \"TIKTOK\",\n" +
                                            "        \"isConnected\": false,\n" +
                                            "        \"id\": null,\n" +
                                            "        \"accountUrl\": null\n" +
                                            "      }\n" +
                                            "    ]\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping
    public ResponseEntity<?> getAllPlatforms(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            // 사용자가 연동한 플랫폼들 조회
            List<UserSnsPlatform> connectedPlatforms = userSnsPlatformRepository.findByUserId(userId);
            Map<String, UserSnsPlatform> connectedPlatformMap = connectedPlatforms.stream()
                    .collect(Collectors.toMap(
                            platform -> platform.getPlatformType().toUpperCase(),
                            platform -> platform,
                            (existing, replacement) -> existing  // 중복 시 기존 값 유지
                    ));

            // 모든 가능한 플랫폼 목록 생성
            List<Map<String, Object>> allPlatformList = List.of(
                    createPlatformInfo(PlatformType.INSTAGRAM, connectedPlatformMap),
                    createPlatformInfo(PlatformType.YOUTUBE, connectedPlatformMap),
                    createPlatformInfo(PlatformType.BLOG, connectedPlatformMap),
                    createPlatformInfo(PlatformType.TIKTOK, connectedPlatformMap)
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("platforms", allPlatformList);

            return ResponseEntity.ok(BaseResponse.success(responseData, "전체 SNS 플랫폼 목록 조회 성공"));
        } catch (Exception e) {
            log.error("전체 SNS 플랫폼 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "ALL_PLATFORM_LIST_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * 플랫폼 정보 생성 헬퍼 메서드
     */
    private Map<String, Object> createPlatformInfo(PlatformType platformType, Map<String, UserSnsPlatform> connectedPlatformMap) {
        Map<String, Object> platformInfo = new HashMap<>();
        UserSnsPlatform connectedPlatform = connectedPlatformMap.get(platformType.name());

        platformInfo.put("platformType", platformType.name());

        if (connectedPlatform != null) {
            // 연동된 플랫폼
            platformInfo.put("isConnected", true);
            platformInfo.put("id", connectedPlatform.getId());
            platformInfo.put("accountUrl", connectedPlatform.getAccountUrl());
        } else {
            // 연동되지 않은 플랫폼
            platformInfo.put("isConnected", false);
            platformInfo.put("id", null);
            platformInfo.put("accountUrl", null);
        }

        return platformInfo;
    }

    // 팔로워 수 수동 업데이트 API
    @Hidden
    @Operation(summary = "SNS 플랫폼 팔로워 수 수동 업데이트", description = "연동된 SNS 플랫폼의 팔로워 수를 수동으로 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팔로워 수 업데이트 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/PlatformListSuccessResponse"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "플랫폼을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
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
    @Operation(summary = "SNS 플랫폼 연동", description = """
            플랫폼 타입(BLOG, INSTAGRAM, YOUTUBE, TIKTOK)과 URL을 입력받아 연동합니다.
            
            ### 연동 동작
            - **신규 연동**: 해당 플랫폼이 연동되지 않은 경우 새로 연동합니다.
            - **기존 연동 업데이트**: 이미 해당 플랫폼이 연동된 경우, 새 URL로 덮어씁니다.
            - **동일 URL 재연동**: 같은 URL로 재연동 시도 시 기존 정보를 반환합니다.
            
            ### 예시
            - 사용자가 Instagram A를 연동한 상태에서 Instagram B를 연동하면, A가 B로 교체됩니다.
            - 팔로워 수와 크롤링 상태는 초기화됩니다.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SNS 플랫폼 연동 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/PlatformConnectSuccessResponse"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잘못된 URL 형식 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "다른 사용자가 이미 연동한 URL",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "success": false,
                                              "message": "이미 다른 사용자가 연동한 틱톡입니다.",
                                              "status": 409,
                                              "errorCode": "PLATFORM_ALREADY_CONNECTED",
                                              "data": null
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
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
            PlatformType platformType;
            try {
                platformType = PlatformType.fromString(request.getType());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("지원하지 않는 플랫폼 타입입니다.", "UNSUPPORTED_PLATFORM", HttpStatus.BAD_REQUEST.value()));
            }

            switch (platformType) {
                case BLOG:
                    platformId = naverBlogConnectService.connect(userId, request.getUrl());
                    break;
                case INSTAGRAM:
                    platformId = instagramConnectService.connect(userId, request.getUrl());
                    break;
                case YOUTUBE:
                    platformId = youtubeConnectService.connect(userId, request.getUrl());
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(BaseResponse.fail("지원하지 않는 플랫폼 타입입니다.", "UNSUPPORTED_PLATFORM", HttpStatus.BAD_REQUEST.value()));
            }

            Map<String, Object> responseData = Map.of(
                    "platformId", platformId,
                    "platformType", platformType.name(),
                    "message", platformType.getKoreanName() + " 연동이 완료되었습니다."
            );

            return ResponseEntity.ok(BaseResponse.success(responseData, platformType.getKoreanName() + " 연동 성공"));
        } catch (IllegalArgumentException e) {
            // 잘못된 URL 형식 등
            log.warn("잘못된 플랫폼 연동 요청: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "INVALID_PLATFORM_REQUEST", HttpStatus.BAD_REQUEST.value()));
        } catch (IllegalStateException e) {
            // 다른 사용자가 이미 연동한 URL
            log.warn("플랫폼 연동 충돌: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(BaseResponse.fail(e.getMessage(), "PLATFORM_ALREADY_CONNECTED", HttpStatus.CONFLICT.value()));
        } catch (Exception e) {
            log.error("SNS 플랫폼 연동 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "PLATFORM_CONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // 개별 플랫폼 연동 해제 API
    @Operation(summary = "SNS 플랫폼 연동 해제", description = """
            특정 SNS 플랫폼의 연동을 해제합니다.
            
            ### 동작
            - 지정된 플랫폼 ID의 연동을 완전히 삭제합니다
            - 해당 플랫폼의 팔로워 수 및 모든 관련 데이터가 삭제됩니다
            - 삭제 후에는 같은 플랫폼 타입으로 새로운 계정을 연동할 수 있습니다
            
            ### 주의사항
            - 연동 해제 후에는 복구할 수 없습니다
            - 본인이 연동한 플랫폼만 해제 가능합니다
            - 에러코드
                - `PLATFORM_NOT_FOUND`: 해당 플랫폼 연동 정보를 찾을 수 없음
                - `INVALID_DISCONNECT_REQUEST`: 잘못된 요청 (예: 지원하지 않는 플랫폼 타입)
                - `PLATFORM_DISCONNECT_ERROR`: 서버 오류 등 기타 문제
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SNS 플랫폼 연동 해제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "연동 해제 성공",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "플랫폼 연동 해제 성공",
                                              "status": 200,
                                              "data": {
                                                "platformId": 15,
                                                "platformType": "INSTAGRAM",
                                                "accountUrl": "https://instagram.com/myaccount",
                                                "message": "인스타그램 연동이 해제되었습니다."
                                              }
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "연동된 플랫폼을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @DeleteMapping("/{platformId}")
    public ResponseEntity<?> disconnectPlatform(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "연동 해제할 플랫폼 ID", required = true, example = "15")
            @PathVariable Long platformId
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);

            // 플랫폼 소유권 확인 및 정보 조회
            UserSnsPlatform platform = userSnsPlatformRepository.findByUserIdAndId(userId, platformId)
                    .orElseThrow(() -> new RuntimeException("연동된 플랫폼을 찾을 수 없습니다."));

            String platformType = platform.getPlatformType();
            String accountUrl = platform.getAccountUrl();

            // 개별 서비스의 disconnect 메소드 호출
            switch (PlatformType.fromString(platformType)) {
                case BLOG:
                    naverBlogConnectService.disconnect(userId, platformId);
                    break;
                case INSTAGRAM:
                    instagramConnectService.disconnect(userId, platformId);
                    break;
                case YOUTUBE:
                    youtubeConnectService.disconnect(userId, platformId);
                    break;
                default:
                    throw new IllegalArgumentException("지원하지 않는 플랫폼 타입입니다: " + platformType);
            }

            log.info("SNS 플랫폼 연동 해제 완료: userId={}, platformId={}, type={}",
                    userId, platformId, platformType);

            Map<String, Object> responseData = Map.of(
                    "platformId", platformId,
                    "platformType", platformType,
                    "accountUrl", accountUrl,
                    "message", PlatformType.fromString(platformType).getKoreanName() + " 연동이 해제되었습니다."
            );

            return ResponseEntity.ok(BaseResponse.success(responseData, "플랫폼 연동 해제 성공"));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 플랫폼 연동 해제 요청: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "INVALID_DISCONNECT_REQUEST", HttpStatus.BAD_REQUEST.value()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                log.warn("존재하지 않는 플랫폼 연동 해제 시도: userId={}, platformId={}",
                        tokenUtils.getUserIdFromToken(bearerToken), platformId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.fail(e.getMessage(), "PLATFORM_NOT_FOUND", HttpStatus.NOT_FOUND.value()));
            }
            throw e;
        } catch (Exception e) {
            log.error("플랫폼 연동 해제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail(e.getMessage(), "PLATFORM_DISCONNECT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}