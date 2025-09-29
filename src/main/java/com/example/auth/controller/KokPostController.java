package com.example.auth.controller;

import com.example.auth.common.ApiResponse;
import com.example.auth.constant.SortOption;
import com.example.auth.dto.KokPostDetailResponse;
import com.example.auth.dto.KokPostDetailWrapper;
import com.example.auth.dto.KokPostListResponse;
import com.example.auth.service.KokPostService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@Tag(name = "체험콕 아티클 API (개선된 버전)", description = "실시간 조회수 처리가 개선된 체험콕 아티클 API")
@Slf4j
@RestController
@RequestMapping("/api/kok-article")
@RequiredArgsConstructor
public class KokPostController {

    private final KokPostService kokPostService;

    @Operation(
            summary = "캠페인별 체험콕 아티클 상세 조회",
            description = "특정 캠페인의 체험콕 아티클 상세 정보를 조회합니다.\n\n" +
                    "개선 사항:\n" +
                    "- 실시간 조회수 반영\n" +
                    "- 원자적 조회수 증가 처리\n" +
                    "- 향상된 중복 방지 로직\n" +
                    "- 캐시 기반 성능 최적화"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "체험콕 아티클 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KokPostDetailWrapper.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 캠페인의 체험콕 글을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "조회수 처리 제한 (동일 IP 24시간 내 중복 요청)"
            )
    })
    @GetMapping("/{campaignId}")
    public ApiResponse<KokPostDetailWrapper> getKokPostByCampaign(
            @Parameter(description = "캠페인 ID", example = "1")
            @PathVariable Long campaignId,
            HttpServletRequest request
    ) {
        log.info("개선된 캠페인별 체험콕 글 상세 조회 API 호출 - campaignId: {}", campaignId);

        // 클라이언트 정보 추출 (개선된 버전)
        String clientIP = extractClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        // 개선된 서비스 호출
        KokPostDetailResponse response = kokPostService.getKokPostDetailByCampaignId(
                campaignId, clientIP, userAgent);
        KokPostDetailWrapper wrapper = KokPostDetailWrapper.of(response);

        log.info("체험콕 글 조회 완료 - campaignId: {}, 실시간 조회수: {}",
                campaignId, response.getViewCount());

        return ApiResponse.success(
                String.format("캠페인 ID %d의 체험콕 글을 성공적으로 조회했어요. (실시간 조회수: %d)",
                        campaignId, response.getViewCount()),
                wrapper
        );
    }

    @Hidden
    @Operation(
            summary = "체험콕 글 전체 목록 조회 (개선된 버전)",
            description = "모든 체험콕 홍보글의 목록을 조회합니다. (실시간 조회수 적용)\n\n" +
                    "정렬 옵션:\n" +
                    "- latest: 최신순 (기본값)\n" +
                    "- popular: 인기순 (조회수 기준)"
    )
    @GetMapping
    public ApiResponse<List<KokPostListResponse>> getAllKokPosts(
            @Parameter(description = "정렬 옵션 (latest: 최신순, popular: 인기순)")
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        log.info("개선된 체험콕 글 전체 목록 조회 API 호출 - 정렬: {}", sort);

        List<KokPostListResponse> response;

        if ("popular".equals(sort)) {
            // 인기순 정렬 시 캐시된 인기 포스트 활용
            response = kokPostService.getPopularKokPosts(100);
        } else {
            // 기본 정렬
            SortOption sortOption = SortOption.fromValue(sort);
            response = kokPostService.getAllKokPosts(sortOption);
        }

        return ApiResponse.success(
                String.format("체험콕 글 목록을 성공적으로 조회했습니다. (총 %d개, 정렬: %s)",
                        response.size(), sort),
                response
        );
    }

    @Hidden
    @Operation(
            summary = "체험콕 글 제목 검색 (개선된 버전)",
            description = "제목으로 체험콕 글을 검색합니다. (실시간 조회수 적용)\n\n" +
                    "정렬 옵션:\n" +
                    "- latest: 최신순 (기본값)\n" +
                    "- popular: 인기순 (조회수 기준)"
    )
    @GetMapping("/search")
    public ApiResponse<List<KokPostListResponse>> searchKokPosts(
            @Parameter(description = "검색할 제목 키워드")
            @RequestParam String title,
            @Parameter(description = "정렬 옵션 (latest: 최신순, popular: 인기순)")
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        log.info("개선된 체험콕 글 제목 검색 API 호출 - 키워드: {}, 정렬: {}", title, sort);

        SortOption sortOption = SortOption.fromValue(sort);
        List<KokPostListResponse> response = kokPostService.searchKokPostsByTitle(title, sortOption);

        return ApiResponse.success(
                String.format("체험콕 글 검색을 성공적으로 완료했습니다. (키워드: %s, 결과: %d개)",
                        title, response.size()),
                response
        );
    }

    /**
     * 클라이언트 실제 IP 주소 추출 (개선된 버전)
     */
    private String extractClientIP(HttpServletRequest request) {
        // 프록시/로드밸런서 환경을 고려한 헤더 우선순위
        String[] ipHeaders = {
                "CF-Connecting-IP",      // Cloudflare
                "X-Forwarded-For",       // 일반적인 프록시
                "X-Real-IP",             // Nginx
                "X-Original-Forwarded-For", // AWS ALB
                "Proxy-Client-IP",       // Apache
                "WL-Proxy-Client-IP",    // WebLogic
                "HTTP_X_FORWARDED_FOR",  // 기타
                "HTTP_X_FORWARDED",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_CLIENT_IP",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (isValidIP(ip)) {
                // X-Forwarded-For의 경우 첫 번째 IP 사용
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                log.debug("클라이언트 IP 추출 성공 - 헤더: {}, IP: {}", header, ip);
                return ip;
            }
        }

        // 모든 헤더에서 IP를 찾지 못한 경우 기본 IP 사용
        String defaultIP = request.getRemoteAddr();
        log.debug("기본 IP 사용 - IP: {}", defaultIP);
        return defaultIP;
    }

    /**
     * 유효한 IP 주소인지 검증
     */
    private boolean isValidIP(String ip) {
        return ip != null
                && !ip.isEmpty()
                && !ip.isBlank()
                && !"unknown".equalsIgnoreCase(ip)
                && !"127.0.0.1".equals(ip)
                && !"0:0:0:0:0:0:0:1".equals(ip)
                && !ip.startsWith("192.168.")
                && !ip.startsWith("10.")
                && !(ip.startsWith("172.") &&
                ip.split("\\.").length >= 2 &&
                Integer.parseInt(ip.split("\\.")[1]) >= 16 &&
                Integer.parseInt(ip.split("\\.")[1]) <= 31);
    }
}