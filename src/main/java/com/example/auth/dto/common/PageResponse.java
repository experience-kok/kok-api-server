package com.example.auth.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * 페이징 처리된 데이터 응답을 위한 공통 DTO
 * @param <T> 페이징 데이터 항목의 타입
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이징 응답")
public class PageResponse<T> {

    @Schema(description = "조회된 데이터 목록")
    private List<T> content;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber;

    @Schema(description = "페이지 크기", example = "10")
    private int pageSize;

    @Schema(description = "전체 페이지 수", example = "5")
    private int totalPages;

    @Schema(description = "전체 항목 수", example = "42")
    private long totalElements;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;
    
    /**
     * Spring Data의 Page 객체로부터 PageResponse 객체를 생성합니다.
     * @param page Spring Data Page 객체
     * @param <T> 데이터 타입
     * @return 생성된 PageResponse 객체
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
    
    /**
     * Spring Data의 Page 객체로부터 변환 함수를 적용하여 PageResponse 객체를 생성합니다.
     * @param page Spring Data Page 객체
     * @param mapper 원본 데이터를 대상 데이터로 변환하는 함수
     * @param <E> 원본 데이터 타입
     * @param <T> 대상 데이터 타입
     * @return 생성된 PageResponse 객체
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        List<T> content = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());
                
        return PageResponse.<T>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}