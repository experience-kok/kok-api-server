package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.banner.BannerImageResponse;
import com.example.auth.service.BannerImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
@Tag(name = "배너 이미지 API", description = "배너 이미지 관련 API")
public class BannerImageController {

    private final BannerImageService bannerImageService;

    @Operation(
            summary = "배너 이미지 목록 조회",
            description = "모든 배너 이미지 목록을 최신순으로 조회합니다."
                    + "\n\n### 응답 정보:"
                    + "\n- **id**: 배너 고유 식별자"
                    + "\n- **bannerUrl**: 배너 이미지 URL"
                    + "\n- **redirectUrl**: 클릭 시 이동할 URL"
                    + "\n\n### 사용 예시:"
                    + "\n- 메인 페이지 배너 표시"
                    + "\n- 프로모션 배너 캐러셀"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<?> getAllBanners() {
        try {
            log.info("배너 이미지 목록 조회 요청");

            List<BannerImageResponse> banners = bannerImageService.getAllBanners();
            
            return ResponseEntity.ok(BaseResponse.success(banners, "배너 이미지 목록 조회 성공"));
        } catch (Exception e) {
            log.error("배너 이미지 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("배너 이미지 목록 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
