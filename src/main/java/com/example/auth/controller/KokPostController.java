package com.example.auth.controller;

import com.example.auth.common.ApiResponse;
import com.example.auth.constant.SortOption;
import com.example.auth.dto.KokPostListResponse;
import com.example.auth.service.KokPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "체험콕 아티클 API", description = "체험콕 아티클 API (목록조회/검색 전용)")
@Slf4j
@RestController
@RequestMapping("/api/kokposts")
@RequiredArgsConstructor
public class KokPostController {

    private final KokPostService kokPostService;

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
