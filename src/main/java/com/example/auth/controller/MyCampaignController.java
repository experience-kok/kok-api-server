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
                    examples = {
                        @ExampleObject(
                            name = "USER 역할 응답",
                            value = "{\n" +
                                   "  \"success\": true,\n" +
                                   "  \"message\": \"내 캠페인 요약 조회 성공\",\n" +
                                   "  \"status\": 200,\n" +
                                   "  \"data\": {\n" +
                                   "    \"role\": \"USER\",\n" +
                                   "    \"summary\": {\n" +
                                   "      \"applied\": {\n" +
                                   "        \"count\": 8,\n" +
                                   "        \"label\": \"지원\"\n" +
                                   "      },\n" +
                                   "      \"pending\": {\n" +
                                   "        \"count\": 5,\n" +
                                   "        \"label\": \"대기중\"\n" +
                                   "      },\n" +
                                   "      \"selected\": {\n" +
                                   "        \"count\": 2,\n" +
                                   "        \"label\": \"선정\"\n" +
                                   "      },\n" +
                                   "      \"completed\": {\n" +
                                   "        \"count\": 1,\n" +
                                   "        \"label\": \"완료\"\n" +
                                   "      }\n" +
                                   "    }\n" +
                                   "  }\n" +
                                   "}"
                        ),
                        @ExampleObject(
                            name = "CLIENT 역할 응답",
                            value = "{\n" +
                                   "  \"success\": true,\n" +
                                   "  \"message\": \"내 캠페인 요약 조회 성공\",\n" +
                                   "  \"status\": 200,\n" +
                                   "  \"data\": {\n" +
                                   "    \"role\": \"CLIENT\",\n" +
                                   "    \"summary\": {\n" +
                                   "      \"pending\": {\n" +
                                   "        \"count\": 3,\n" +
                                   "        \"label\": \"대기중\"\n" +
                                   "      },\n" +
                                   "      \"approved\": {\n" +
                                   "        \"count\": 5,\n" +
                                   "        \"label\": \"승인됨\"\n" +
                                   "      },\n" +
                                   "      \"rejected\": {\n" +
                                   "        \"count\": 2,\n" +
                                   "        \"label\": \"거절됨\"\n" +
                                   "      },\n" +
                                   "      \"expired\": {\n" +
                                   "        \"count\": 7,\n" +
                                   "        \"label\": \"만료됨\"\n" +
                                   "      }\n" +
                                   "    }\n" +
                                   "  }\n" +
                                   "}"
                        )
                    }
                )
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 실패",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"success\": false,\n" +
                               "  \"message\": \"인증이 필요합니다.\",\n" +
                               "  \"errorCode\": \"UNAUTHORIZED\",\n" +
                               "  \"status\": 401\n" +
                               "}"
                    )
                )
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "서버 오류",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"success\": false,\n" +
                               "  \"message\": \"내 캠페인 요약 조회 중 오류가 발생했습니다.\",\n" +
                               "  \"errorCode\": \"INTERNAL_ERROR\",\n" +
                               "  \"status\": 500\n" +
                               "}"
                    )
                )
            )
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
            
            MyCampaignSummaryResponse summary = myCampaignService.getMyCampaignSummary(userId, userRole);
            
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
