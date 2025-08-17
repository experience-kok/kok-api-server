package com.example.auth.dto.business;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사업자 정보 등록/수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사업자 정보 등록/수정 요청")
public class BusinessInfoRequest {

    @NotBlank(message = "업체명은 필수입니다.")
    @Size(max = 100, message = "업체명은 100자를 초과할 수 없습니다.")
    @Schema(description = "업체명", example = "맛있는 카페")
    private String companyName;

    @NotBlank(message = "사업자등록번호는 필수입니다.")
    @Size(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
    @Schema(description = "사업자등록번호", example = "123-45-67890")
    private String businessRegistrationNumber;
}
