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
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Tag(name = "체험콕 아티클 API", description = "체험콕 아티클 API")
@Slf4j
@RestController
@RequestMapping("/api/kok-article")
@RequiredArgsConstructor
public class KokPostController {

    private final KokPostService kokPostService;

    @Operation(
            summary = "캠페인별 체험콕 아티클 상세 조회",
            description = "특정 캠페인의 체험콕 아티클 상세 정보를 조회합니다."
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
            )
    })
    @GetMapping("/{campaignId}")
    public ApiResponse<KokPostDetailWrapper> getKokPostByCampaign(
            @Parameter(description = "캠페인 ID", example = "1")
            @PathVariable Long campaignId,
            HttpServletRequest request
    ) {
        log.info("캠페인별 체험콕 글 상세 조회 API 호출 - campaignId: {}", campaignId);

        // 클라이언트 IP 추출
        String clientIP = getClientIP(request);

        // 서비스 호출 시 IP 전달
        KokPostDetailResponse response = kokPostService.getKokPostDetailByCampaignId(campaignId, clientIP);
        KokPostDetailWrapper wrapper = KokPostDetailWrapper.of(response);

        return ApiResponse.success(
                String.format("캠페인 ID %d의 체험콕 글을 성공적으로 조회했어요.", campaignId),
                wrapper
        );
    }

    /**
     * 클라이언트 실제 IP 주소 추출
     */
    private String getClientIP(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }


    @Hidden
    @Operation(
            summary = "체험콕 글 전체 목록 조회",
            description = "모든 체험콕 홍보글의 목록을 조회합니다.\n\n" +
                    "정렬 옵션:\n" +
                    "- latest: 최신순 (기본값)\n"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/KokPostListSuccessResponse")
                    )
            )
    })
    @GetMapping
    public ApiResponse<List<KokPostListResponse>> getAllKokPosts(
            @Parameter(description = "정렬 옵션 (latest: 최신순)")
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        log.info("체험콕 글 전체 목록 조회 API 호출 - 정렬: {}", sort);

        SortOption sortOption = SortOption.fromValue(sort);
        List<KokPostListResponse> response = kokPostService.getAllKokPosts(sortOption);

        return ApiResponse.success("체험콕 글 목록을 성공적으로 조회했습니다.", response);
    }

    @Hidden
    @Operation(
            summary = "체험콕 글 제목 검색",
            description = "제목으로 체험콕 글을 검색합니다.\n\n" +
                    "정렬 옵션:\n" +
                    "- latest: 최신순 (기본값)\n"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/KokPostListSuccessResponse")
                    )
            )
    })
    @GetMapping("/search")
    public ApiResponse<List<KokPostListResponse>> searchKokPosts(
            @Parameter(description = "검색할 제목 키워드")
            @RequestParam String title,
            @Parameter(description = "정렬 옵션 (latest: 최신순")
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        log.info("체험콕 글 제목 검색 API 호출 - 키워드: {}, 정렬: {}", title, sort);

        SortOption sortOption = SortOption.fromValue(sort);
        List<KokPostListResponse> response = kokPostService.searchKokPostsByTitle(title, sortOption);

        return ApiResponse.success("체험콕 글 검색을 성공적으로 완료했습니다.", response);
    }

}
