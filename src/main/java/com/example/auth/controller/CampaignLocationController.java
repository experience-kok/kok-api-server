package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.constant.UserRole;
import com.example.auth.dto.location.CampaignLocationRequest;
import com.example.auth.dto.location.CampaignLocationResponse;
import com.example.auth.exception.*;
import com.example.auth.service.CampaignLocationService;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

/**
 * 캠페인 위치 정보 관리 컨트롤러 (간소화)
 */
@Slf4j
@RestController
@RequestMapping("/api/campaigns/{campaignId}/location")
@RequiredArgsConstructor
@Tag(name = "캠페인 위치 API", description = "캠페인의 위치 정보 관리 API (간소화)")
public class CampaignLocationController {

    private final CampaignLocationService locationService;
    private final TokenUtils tokenUtils;

    @Operation(
            summary = "캠페인 위치 정보 조회",
            description = "특정 캠페인의 위치 정보를 조회합니다.\n\n" +
                    "### 주요 기능\n" +
                    "- 캠페인의 위치 정보를 조회합니다.\n" +
                    "- 인증 없이 누구나 조회 가능합니다.\n" +
                    "- 위치 정보가 없으면 null을 반환합니다.\n\n" +
                    "### 응답 정보\n" +
                    "- **lat, lng**: 지도 표시용 좌표 (선택사항)\n" +
                    "- **homepage**: 공식 홈페이지 주소\n" +
                    "- **contactPhone**: 연락처\n" +
                    "- **visitAndReservationInfo**: 방문/예약 안내\n" +
                    "- **businessAddress**: 사업장 주소\n" +
                    "- **businessDetailAddress**: 사업장 상세 주소\n"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "위치 정보 있음",
                                            value = """
                                            {
                                              "success": true,
                                              "message": "캠페인 위치 정보를 조회했습니다",
                                              "status": 200,
                                              "data": {
                                                "id": 1,
                                                "campaignId": 42,
                                                "lat": 37.5665,
                                                "lng": 126.9780,
                                                "homepage": "https://delicious-cafe.com",
                                                "contactPhone": "02-123-4567",
                                                "visitAndReservationInfo": "평일 10시-22시 방문 가능, 사전 예약 필수",
                                                "businessAddress": "서울특별시 강남구 테헤란로 123",
                                                "businessDetailAddress": "123빌딩 5층"
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "위치 정보 없음",
                                            value = """
                                            {
                                              "success": true,
                                              "message": "캠페인 위치 정보를 조회했습니다",
                                              "status": 200,
                                              "data": null
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음",
                    content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping
    public ResponseEntity<?> getLocation(
            @Parameter(description = "캠페인 ID", required = true, example = "42")
            @PathVariable Long campaignId
    ) {
        try {
            CampaignLocationResponse location = locationService.getLocation(campaignId);
            return ResponseEntity.ok(BaseResponse.success(location, "캠페인 위치 정보를 조회했습니다"));
        } catch (ResourceNotFoundException e) {
            log.warn("캠페인 위치 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(e.getMessage(), "NOT_FOUND", HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            log.error("캠페인 위치 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("위치 정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
