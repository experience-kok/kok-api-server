package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitLocationRequest {

    @NotBlank(message = "주소를 입력해주세요")
    private String address;
    
    private BigDecimal latitude;
    
    private BigDecimal longitude;
    
    private String additionalInfo;
}
