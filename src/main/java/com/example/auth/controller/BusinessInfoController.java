package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.domain.Company;
import com.example.auth.domain.User;
import com.example.auth.dto.business.BusinessInfoRequest;
import com.example.auth.dto.business.BusinessInfoResponse;
import com.example.auth.service.CompanyService;
import com.example.auth.service.UserService;
import com.example.auth.util.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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

import java.util.Optional;

/**
 * 사업자 정보 관리 컨트롤러
 * CLIENT 권한 심사용 사업자 정보를 관리합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/business-info")
@RequiredArgsConstructor
@Tag(name = "사업자 정보 API", description = "CLIENT 권한 심사용 사업자 정보 관리")
public class BusinessInfoController {

    private final UserService userService;
    private final CompanyService companyService;
    private final TokenUtils tokenUtils;

    @Operation(
        summary = "사업자 정보 조회",
        description = "현재 사용자의 사업자 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "사업자 정보 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BusinessInfoResponse.class)
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
    @GetMapping
    public ResponseEntity<?> getBusinessInfo(@RequestHeader("Authorization") String bearerToken) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            User user = userService.findUserById(userId);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.fail("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND", HttpStatus.NOT_FOUND.value()));
            }
            
            // Company 엔티티에서 사업자 정보 조회
            Optional<Company> companyOpt = companyService.findByUserId(userId);
            BusinessInfoResponse response = BusinessInfoResponse.fromCompany(companyOpt.orElse(null));
            
            log.info("사업자 정보 조회 성공: userId={}", userId);
            
            return ResponseEntity.ok(BaseResponse.success(response, "사업자 정보 조회 성공"));
            
        } catch (Exception e) {
            log.error("사업자 정보 조회 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("사업자 정보 조회 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "사업자 정보 등록",
        description = """
            CLIENT 권한 심사를 위한 사업자 정보를 등록합니다.
            
            ### 필수 정보
            - companyName: 업체명
            - businessRegistrationNumber: 사업자등록번호
            - termsAgreed: 약관 동의 여부 (true 필수)
            
            ### 주의사항
            - 이미 등록된 사업자 정보가 있는 경우 등록할 수 없습니다.
            - 약관에 동의하지 않으면 등록할 수 없습니다.
            - 약관 동의 시점이 자동으로 기록됩니다.
            
            ### 오류 코드
            - ALREADY_REGISTERED: 이미 사업자 정보가 등록된 경우
            - BUSINESS_INFO_REGISTRATION_FAILED: 약관 미동의 또는 기타 검증 실패
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "사업자 정보 등록 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BusinessInfoResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = """
                    잘못된 요청
                    - 약관 미동의
                    - 필수 필드 누락
                    - 이미 등록된 사업자 정보 존재
                    """,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")
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
    @PostMapping
    public ResponseEntity<?> updateBusinessInfo(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody @Valid BusinessInfoRequest request) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            User user = userService.findUserById(userId);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.fail("사용자를 찾을 수 없어요.", "USER_NOT_FOUND", HttpStatus.NOT_FOUND.value()));
            }
            
            // 이미 사업자 정보가 등록되어 있는지 확인
            Optional<Company> existingCompany = companyService.findByUserId(userId);
            if (existingCompany.isPresent()) {
                log.warn("사업자 정보 등록/수정 차단: userId={}, 이미 등록된 사업자 정보가 존재함", userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(BaseResponse.fail("이미 사업자 정보가 등록되어 있어요. 등록된 사업자 정보는 수정할 수 없어요.", "ALREADY_REGISTERED", HttpStatus.BAD_REQUEST.value()));
            }
            
            // Company 엔티티에 사업자 정보 저장 (신규 등록만 허용)
            Company company = companyService.createBusinessInfoOnly(user, request);
            BusinessInfoResponse response = BusinessInfoResponse.fromCompany(company);
            
            log.info("사업자 정보 등록 성공: userId={}, companyName={}", userId, request.getCompanyName());
            
            return ResponseEntity.ok(BaseResponse.success(response, "사업자 정보가 성공적으로 등록되었어요."));
            
        } catch (IllegalArgumentException e) {
            log.warn("사업자 정보 등록 실패: userId={}, 사유: {}", tokenUtils.getUserIdFromToken(bearerToken), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(e.getMessage(), "BUSINESS_INFO_REGISTRATION_FAILED", HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            log.error("사업자 정보 등록 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("사업자 정보 등록 중 오류가 발생했어요.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @Operation(
        summary = "사업자 정보 삭제",
        description = "등록된 사업자 정보를 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200", 
                description = "사업자 정보 삭제 성공"
            ),
            @ApiResponse(
                responseCode = "401", 
                description = "인증 실패"
            )
    })
    @DeleteMapping
    public ResponseEntity<?> deleteBusinessInfo(@RequestHeader("Authorization") String bearerToken) {
        try {
            Long userId = tokenUtils.getUserIdFromToken(bearerToken);
            User user = userService.findUserById(userId);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.fail("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND", HttpStatus.NOT_FOUND.value()));
            }
            
            // Company 엔티티 삭제
            companyService.deleteByUserId(userId);
            
            log.info("사업자 정보 삭제 성공: userId={}", userId);
            
            return ResponseEntity.ok(BaseResponse.success(null, "사업자 정보가 성공적으로 삭제되었습니다."));
            
        } catch (Exception e) {
            log.error("사업자 정보 삭제 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.fail("사업자 정보 삭제 중 오류가 발생했습니다.", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
