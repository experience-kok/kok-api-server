package com.example.auth.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 개별 캠페인 신청 정보 조회 응답을 위한 Wrapper DTO
 * 일관된 응답 구조를 위해 모든 종류의 신청 정보를 application 객체 안에 포함시킵니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "개별 캠페인 신청 정보 응답 Wrapper")
public class ApplicationSingleResponseWrapper<T> {
    
    @Schema(description = "신청 정보")
    private T application;
    
    /**
     * 정적 팩토리 메서드: 어떤 타입의 신청 정보든 application 필드에 포함시키는 Wrapper 생성
     */
    public static <T> ApplicationSingleResponseWrapper<T> of(T applicationData) {
        return ApplicationSingleResponseWrapper.<T>builder()
                .application(applicationData)
                .build();
    }
}
