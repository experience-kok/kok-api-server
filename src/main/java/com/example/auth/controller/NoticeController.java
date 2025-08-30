package com.example.auth.controller;

import com.example.auth.common.ApiResponse;
import com.example.auth.constant.SortOption;
import com.example.auth.dto.NoticePageResponse;
import com.example.auth.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공지사항 API", description = "공지사항 API (목록조회/검색 전용)")
@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(
            summary = "공지사항 전체 목록 조회 (페이지네이션)",
            description = "모든 공지사항의 목록을 페이지네이션으로 조회합니다.\n\n" +
                    "**정렬 우선순위:**\n" +
                    "1. 필독 공지사항이 항상 먼저 표시됩니다\n" +
                    "2. 그 다음 선택한 정렬 옵션이 적용됩니다\n\n" +
                    "정렬 옵션:\n" +
                    "- latest: 최신순 (기본값)\n"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/NoticeListSuccessResponse")
                    )
            )
    })
    @GetMapping
    public ApiResponse<NoticePageResponse> getAllNotices(
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,
            @Parameter(description = "페이지당 항목 수 (최대 100)")
            @RequestParam(required = false, defaultValue = "10") int size,
            @Parameter(description = "정렬 옵션 (latest: 최신순)")
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        log.info("공지사항 전체 목록 조회 API 호출 - page: {}, size: {}, 정렬: {}", page, size, sort);

        // 프론트엔드 페이지 번호 (1부터 시작)를 서버 페이지 번호 (1부터 시작)로 변환
        int serverPage = Math.max(0, page - 1);
        
        SortOption sortOption = SortOption.fromValue(sort);
        NoticePageResponse response = noticeService.getAllNotices(serverPage, size, sortOption);

        return ApiResponse.success("목록 조회 성공 .", response);
    }

    @Operation(
            summary = "공지사항 제목 검색 (페이지네이션)",
            description = "제목으로 공지사항을 검색합니다.\n\n" +
                    "**정렬 우선순위:**\n" +
                    "1. 필독 공지사항이 항상 먼저 표시됩니다\n" +
                    "2. 그 다음 선택한 정렬 옵션이 적용됩니다\n\n" +
                    "정렬 옵션:\n" +
                    "- latest: 최신순"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/NoticeListSuccessResponse")
                    )
            )
    })
    @GetMapping("/search")
    public ApiResponse<NoticePageResponse> searchNotices(
            @Parameter(description = "검색할 제목 키워드")
            @RequestParam String title,
            @Parameter(description = "페이지 번호 (1부터 시작)")
            @RequestParam(required = false, defaultValue = "1") int page,
            @Parameter(description = "페이지당 항목 수 (최대 100)")
            @RequestParam(required = false, defaultValue = "10") int size,
            @Parameter(description = "정렬 옵션 (latest: 최신순")
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        log.info("공지사항 제목 검색 API 호출 - 키워드: {}, 정렬: {}", title, sort);

        // 프론트엔드 페이지 번호 (1부터 시작)를 서버 페이지 번호 (0부터 시작)로 변환
        int serverPage = Math.max(0, page - 1);
        
        SortOption sortOption = SortOption.fromValue(sort);
        NoticePageResponse response = noticeService.searchNoticesByTitle(title, serverPage, size, sortOption);

        return ApiResponse.success("공지사항 검색 성공.", response);
    }

}
