package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.campaign.MyCampaignListResponse;
import com.example.auth.dto.campaign.MyCampaignSummaryResponse;
import com.example.auth.exception.JwtValidationException;
import com.example.auth.exception.TokenErrorType;
import com.example.auth.exception.UnauthorizedException;
import com.example.auth.service.MyCampaignService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 내 캠페인 목록 조회 컨트롤러
 * 사용자 역할(USER/CLIENT)에 따라 다른 형태의 캠페인 목록을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/my-campaigns")
@RequiredArgsConstructor
@Tag(name = "내 캠페인 API", description = "사용자별 캠페인 목록 조회 API")
public class MyCampaignController {

    private final MyCampaignService myCampaignService;
    private final TokenUtils tokenUtils;

    @Operation(
        summary = "내 캠페인 요약 조회", 
        description = "사용자 역할에 따른 캠페인 카테고리별 요약 정보를 조회합니다.\n\n" +
                      "### USER 역할\n" +
                      "- **지원**: 내가 신청한 모든 캠페인 수\n" +
                      "- **대기중**: 심사 대기중인 신청 수\n" +
                      "- **선정**: 선정된 신청 수\n" +
                      "- **완료**: 완료한 신청 수\n\n" +
                      "### CLIENT 역할\n" +
                      "- **대기중**: 관리자 승인 대기중인 캠페인 수\n" +
                      "- **승인됨**: 승인되어 활성화된 캠페인 수\n" +
                      "- **거절됨**: 관리자가 거절한 캠페인 수\n" +
                      "- **만료됨**: 승인됐지만 신청기간이 종료된 캠페인 수"
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "요약 조회 성공",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(ref = "#/components/schemas/MyCampaignSuccessResponse"),
                        examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "CLIENT 역할 - 내 캠페인 요약",
                                summary = "기업 회원의 캠페인 요약 정보",
                                value = """
                                    {
                                      "success": true,
                                      "message": "내 캠페인 요약 조회 성공",
                                      "status": 200,
                                      "data": {
                                        "role": "CLIENT",
                                        "summary": {
                                          "pending": {
                                            "count": 0,
                                            "label": "대기중"
                                          },
                                          "approved": {
                                            "count": 14,
                                            "label": "승인됨"
                                          },
                                          "rejected": {
                                            "count": 0,
                                            "label": "거절됨"
                                          },
                                          "expired": {
                                            "count": 0,
                                            "label": "만료됨"
                                          }
                                        }
                                      }
                                    }
                                    """
                            ),
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "USER 역할 - 내 신청 요약",
                                summary = "인플루언서의 신청 요약 정보",
                                value = """
                                    {
                                      "success": true,
                                      "message": "내 캠페인 요약 조회 성공",
                                      "status": 200,
                                      "data": {
                                        "role": "USER",
                                        "summary": {
                                          "applied": {
                                            "count": 3,
                                            "label": "대기중"
                                          },
                                          "pending": {
                                            "count": 2,
                                            "label": "선정됨"
                                          },
                                          "selected": {
                                            "count": 1,
                                            "label": "거절됨"
                                          },
                                          "completed": {
                                            "count": 5,
                                            "label": "완료됨"
                                          }
                                        }
                                      }
                                    }
                                    """
                            )
                        })),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 실패",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(
                responseCode = "500", 
                description = "서버 오류",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/summary")
    public ResponseEntity<?> getMyCampaignSummary(
            @Parameter(description = "Bearer 토큰", required = true)
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            String userRole = tokenUtils.getRoleFromToken(bearerToken);
            
            log.info("내 캠페인 요약 조회 요청: userId={}, role={}", userId, userRole);
            
            Object summary = myCampaignService.getMyCampaignSummary(userId, userRole);
            
            return ResponseEntity.ok(BaseResponse.success(summary, "내 캠페인 요약 조회 성공"));
            
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
            log.error("내 캠페인 요약 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("내 캠페인 요약 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }


}
