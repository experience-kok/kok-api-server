package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.dto.like.LikeResponse;
import com.example.auth.dto.like.LikeStatusResponse;
import com.example.auth.dto.like.LikeUserResponse;
import com.example.auth.dto.like.MyLikedCampaignResponse;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.exception.TokenErrorType;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.service.LikeService;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 캠페인 좋아요 컨트롤러
 * 캠페인에 대한 좋아요 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Tag(name = "좋아요 API", description = "캠페인 좋아요 관련 API")
public class LikeController {

    private final LikeService likeService;
    private final TokenUtils tokenUtils;

    @Operation(
        summary = "캠페인 좋아요 토글",
        description = "캠페인에 좋아요를 추가하거나 취소합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 좋아요가 없으면 추가, 있으면 취소\n" +
                      "- 실시간 좋아요 수 반환\n" +
                      "- 로그인한 사용자만 사용 가능\n\n" +
                      "### 응답 정보\n" +
                      "- **liked**: 현재 좋아요 상태 (true: 좋아요함, false: 좋아요 안함)\n" +
                      "- **totalCount**: 해당 캠페인의 총 좋아요 수\n" +
                      "- **campaignId**: 캠페인 ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "좋아요 토글 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/LikeToggleSuccessResponse"),
                examples = {
                    @ExampleObject(
                        name = "좋아요 추가",
                        summary = "좋아요를 추가한 경우",
                        value = """
                            {
                              "success": true,
                              "message": "좋아요가 추가되었습니다",
                              "status": 200,
                              "data": {
                                "liked": true,
                                "totalCount": 43,
                                "campaignId": 123
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "좋아요 취소",
                        summary = "좋아요를 취소한 경우",
                        value = """
                            {
                              "success": true,
                              "message": "좋아요가 취소되었습니다",
                              "status": 200,
                              "data": {
                                "liked": false,
                                "totalCount": 42,
                                "campaignId": 123
                              }
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "캠페인을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
            )
        )
    })
    @PostMapping("/campaigns/{campaignId}")
    public ResponseEntity<?> toggleCampaignLike(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "캠페인 ID", required = true, example = "123")
            @PathVariable Long campaignId
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("캠페인 좋아요 토글 요청: userId={}, campaignId={}", userId, campaignId);

            LikeResponse response = likeService.toggleCampaignLike(userId, campaignId);

            String message = response.isLiked() ? "좋아요가 추가되었습니다" : "좋아요가 취소되었습니다";

            return ResponseEntity.ok(BaseResponse.success(response, message));

        } catch (JwtValidationException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            String errorCode = e.getErrorType() == TokenErrorType.EXPIRED ? "TOKEN_EXPIRED" : "TOKEN_INVALID";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (UnauthorizedException e) {
            log.warn("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인 좋아요 토글 실패 (리소스 없음): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("캠페인 좋아요 토글 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("좋아요 처리 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "내가 좋아요한 캠페인 목록 조회",
        description = "로그인한 사용자가 좋아요한 캠페인 목록을 최신순으로 조회합니다.\n\n" +
                      "### 주요 기능\n" +
                      "- 내가 좋아요한 캠페인만 필터링\n" +
                      "- 최신 좋아요순 정렬\n" +
                      "- 페이징 지원\n" +
                      "- 각 캠페인의 현재 좋아요 수 포함\n\n" +
                      "### 페이징 특징\n" +
                      "- 기본 페이지 크기: 10개\n" +
                      "- 최신 좋아요순으로 정렬"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "내가 좋아요한 캠페인 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/MyLikedCampaignSuccessResponse"),
                examples = @ExampleObject(
                    name = "내가 좋아요한 캠페인 목록",
                    summary = "좋아요한 캠페인 목록 조회 성공",
                    value = """
                        {
                          "success": true,
                          "message": "내가 좋아요한 캠페인 목록 조회 성공",
                          "status": 200,
                          "data": {
                            "content": [
                              {
                                "campaignId": 123,
                                "title": "신상 음료 체험단 모집",
                                "campaignType": "인스타그램",
                                "thumbnailUrl": "https://example.com/thumbnail.jpg",
                                "currentApplicants": 8,
                                "maxApplicants": 15,
                                "applicationDeadlineDate": "2027-12-12",
                                "likeCount": 42,
                                "likedAt": "2025-07-29T10:30:00",
                                "category": {
                                  "type": "방문",
                                  "name": "맛집"
                                }
                              }
                            ],
                            "pageNumber": 1,
                            "pageSize": 10,
                            "totalPages": 3,
                            "totalElements": 25,
                            "first": true,
                            "last": false
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
            )
        )
    })
    @GetMapping("/campaigns/my")
    public ResponseEntity<?> getMyLikedCampaigns(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (1-100)", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            log.info("내가 좋아요한 캠페인 목록 조회 요청: userId={}, page={}, size={}", userId, page, size);

            // 페이지 번호 검증 (1부터 시작하므로 0으로 변환)
            if (page < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 번호는 1 이상이어야 합니다.", "INVALID_PAGE", HttpStatus.BAD_REQUEST.value()));
            }

            // 페이지 크기 검증
            if (size < 1 || size > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("페이지 크기는 1-100 사이여야 합니다.", "INVALID_PAGE_SIZE", HttpStatus.BAD_REQUEST.value()));
            }

            PageResponse<MyLikedCampaignResponse> pageResponse = 
                    likeService.getMyLikedCampaigns(userId, page - 1, size);

            return ResponseEntity.ok(BaseResponse.success(pageResponse, "내가 좋아요한 캠페인 목록 조회 성공"));

        } catch (JwtValidationException e) {
            log.warn("토큰 검증 실패: {}", e.getMessage());
            String errorCode = e.getErrorType() == TokenErrorType.EXPIRED ? "TOKEN_EXPIRED" : "TOKEN_INVALID";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), errorCode, HttpStatus.UNAUTHORIZED.value()));
        } catch (UnauthorizedException e) {
            log.warn("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(e.getMessage(), "UNAUTHORIZED", HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            log.error("내가 좋아요한 캠페인 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("좋아요한 캠페인 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
