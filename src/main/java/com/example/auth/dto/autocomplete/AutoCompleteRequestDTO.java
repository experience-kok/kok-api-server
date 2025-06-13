package com.example.auth.dto.autocomplete;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "자동완성 요청 DTO")
public class AutoCompleteRequestDTO {

    @NotBlank(message = "검색 키워드는 필수입니다.")
    @Size(min = 1, max = 50, message = "검색 키워드는 1-50자 사이여야 합니다.")
    @Schema(
            description = "검색 키워드", 
            example = "맛집",
            minLength = 1,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String keyword;

    @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
    @Max(value = 20, message = "limit은 20 이하여야 합니다.")
    @Schema(
            description = "최대 제안 개수", 
            example = "10",
            minimum = "1",
            maximum = "20",
            defaultValue = "10"
    )
    private Integer limit = 10;
}
