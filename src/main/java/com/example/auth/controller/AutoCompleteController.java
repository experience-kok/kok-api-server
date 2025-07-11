package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.autocomplete.AutoCompleteResponse;
import com.example.auth.service.AutoCompleteService;
import com.example.auth.service.SearchAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/campaigns/search")
@RequiredArgsConstructor
@Tag(name = "자동완성 API", description = "캠페인 검색 자동완성 및 실시간 인기 검색어 API")
public class AutoCompleteController {

    private final AutoCompleteService autoCompleteService;
    private final SearchAnalyticsService searchAnalyticsService;

    @Operation(
            summary = "검색 자동완성 제안",
            description = "입력된 키워드를 기반으로 캠페인 제목에서 자동완성 제안을 제공."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "자동완성 제안 조회 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "맛집 검색 예시",
                                            summary = "맛집 키워드 검색 결과",
                                            value = """
                                            {
                                              "success": true,
                                              "message": "자동완성 제안 조회 성공",
                                              "status": 200,
                                              "data": {
                                                "suggestions": [
                                                  "맛집 체험단 모집 - 강남구 한식당",
                                                  "맛집 블로거 선정 이벤트",
                                                  "맛집 리뷰어 모집합니다"
                                                ]
                                              }
                                            }
                                            """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "카페 검색 예시",
                                            summary = "카페 키워드 검색 결과",
                                            value = """
                                            {
                                              "success": true,
                                              "message": "자동완성 제안 조회 성공",
                                              "status": 200,
                                              "data": {
                                                "suggestions": [
                                                  "카페 인플루언서 모집",
                                                  "홍대 카페 체험단",
                                                  "감성 카페 리뷰어 선정"
                                                ]
                                              }
                                            }
                                            """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "결과 없음 예시",
                                            summary = "검색 결과가 없는 경우",
                                            value = """
                                            {
                                              "success": true,
                                              "message": "자동완성 제안 조회 성공",
                                              "status": 200,
                                              "data": {
                                                "suggestions": []
                                              }
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400", 
                    description = "잘못된 요청 (키워드 누락 또는 limit 범위 초과)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "에러 응답 예시",
                                    summary = "키워드 누락 시",
                                    value = """
                                    {
                                      "success": false,
                                      "message": "검색 키워드는 필수입니다.",
                                      "errorCode": "INVALID_PARAMETER",
                                      "status": 400
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/suggestions")
    public ResponseEntity<?> getSearchSuggestions(
            @Parameter(
                    description = "검색 키워드 (2글자 이상 권장)", 
                    required = true, 
                    example = "맛집",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "string",
                            minLength = 1,
                            maxLength = 50,
                            example = "맛집"
                    )
            )
            @RequestParam String q,

            @Parameter(
                    description = "최대 제안 개수 (1-20)", 
                    example = "10",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            type = "integer",
                            minimum = "1",
                            maximum = "20",
                            example = "10",
                            defaultValue = "10"
                    )
            )
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        try {
            // 파라미터 검증
            if (q == null || q.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("검색 키워드는 필수입니다.", "INVALID_PARAMETER", HttpStatus.BAD_REQUEST.value()));
            }

            if (limit < 1 || limit > 20) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("limit은 1-20 사이의 값이어야 합니다.", "INVALID_PARAMETER", HttpStatus.BAD_REQUEST.value()));
            }

            log.debug("자동완성 요청 - keyword: {}, limit: {}", q, limit);

            List<String> suggestions = autoCompleteService.getSuggestions(q.trim(), limit);

            AutoCompleteResponse response = AutoCompleteResponse.of(suggestions);
            
            log.debug("자동완성 응답 - {}개 제안", suggestions.size());
            return ResponseEntity.ok(BaseResponse.success(response, "자동완성 제안 조회 성공"));

        } catch (Exception e) {
            log.error("자동완성 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("자동완성 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
            summary = "실시간 인기 검색어 조회",
            description = "사용자들이 실제로 많이 검색한 캠페인 키워드를 실시간으로 제공합니다."
                    + "\n\n### 주요 기능:"
                    + "\n- 사용자들이 실제로 검색한 키워드 기반의 실시간 통계"
                    + "\n- Redis 기반의 실시간 데이터 집계"
                    + "\n- 검색창 초기 상태에서 검색 힌트로 활용 가능"
                    + "\n- 최대 20개의 인기 키워드 제공"
                    + "\n\n### 데이터 특징:"
                    + "\n- **실시간성**: 사용자 검색 시마다 실시간 업데이트"
                    + "\n- **인기순 정렬**: 검색 빈도가 높은 순으로 정렬"
                    + "\n- **자동 갱신**: 7일 주기로 오래된 데이터 자동 삭제"
                    + "\n- **Fallback**: 데이터 없으면 빈 배열 반환"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/realtime")
    public ResponseEntity<?> getRealtimeTrendingKeywords(
            @Parameter(description = "조회할 키워드 수 (최대 20개)")
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        try {
            // limit 범위 제한
            limit = Math.max(1, Math.min(limit, 20));
            
            log.info("실시간 인기 검색어 조회 요청 - limit: {}", limit);
            
            List<String> trendingKeywords = searchAnalyticsService.getTrendingKeywords(limit);
            
            // 요청된 형식으로 응답 데이터 구성
            Map<String, Object> responseData = Map.of(
                    "suggestions", trendingKeywords
            );
            
            log.info("실시간 인기 검색어 조회 성공 - 키워드 수: {}", trendingKeywords.size());
            
            return ResponseEntity.ok(BaseResponse.success(responseData, "인기 검색어 조회 성공"));
            
        } catch (Exception e) {
            log.error("실시간 인기 검색어 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("실시간 인기 검색어 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
