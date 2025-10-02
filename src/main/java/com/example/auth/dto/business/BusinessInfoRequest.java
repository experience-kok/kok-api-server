package com.example.auth.dto.business;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "사업자 정보 등록/수정 요청", example = """
    {
      "companyName": "맛있는 카페",
      "businessRegistrationNumber": "123-45-67890",
      "termsAgreed": true
    }
    """)
public class BusinessInfoRequest {

    @NotBlank(message = "업체명은 필수입니다.")
    @Size(max = 100, message = "업체명은 100자를 초과할 수 없습니다.")
    @Schema(description = "업체명", example = "맛있는 카페", required = true)
    private String companyName;

    @NotBlank(message = "사업자등록번호는 필수입니다.")
    @Size(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
    @Schema(description = "사업자등록번호 (하이픈 포함 가능)", example = "123-45-67890", required = true)
    private String businessRegistrationNumber;

    @NotNull(message = "약관 동의 여부는 필수입니다.")
    @Schema(
        description = "약관 동의 여부 - 사업자 정보 등록을 위해서는 반드시 true여야 합니다.", 
        example = "true", 
        required = true,
        allowableValues = {"true", "false"}
    )
    private Boolean termsAgreed;
}
