package com.example.auth.service;

import com.example.auth.constant.SortOption;
import com.example.auth.domain.Notice;
import com.example.auth.dto.NoticeListResponse;
import com.example.auth.dto.NoticePageResponse;
import com.example.auth.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    /**
     * 공지사항 전체 목록 조회 (페이지네이션) - 필독 공지사항 우선 정렬
     */
    public NoticePageResponse getAllNotices(int page, int size, SortOption sortOption) {
        log.info("공지사항 전체 목록 조회 요청 - page: {}, size: {}, 정렬: {}", page, size, sortOption.getDescription());

        // 페이지 번호와 크기 검증
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // 최대 100개로 제한

        // 필독 공지사항을 먼저 정렬하고, 그 다음 원하는 정렬 기준 적용
        Sort primarySort = Sort.by(Sort.Direction.DESC, "isMustRead"); // 필독 먼저
        Sort secondarySort = sortOption.getSort(); // 사용자가 선택한 정렬
        Sort combinedSort = primarySort.and(secondarySort);
        
        Pageable pageable = PageRequest.of(page, size, combinedSort);
        Page<Notice> noticePage = noticeRepository.findAll(pageable);

        Page<NoticeListResponse> responsePages = noticePage.map(NoticeListResponse::from);

        log.info("공지사항 목록 조회 완료 (필독 우선) - 정렬: {}, 총 {}개, 페이지: {}/{}", 
                sortOption.getDescription(), noticePage.getTotalElements(), page + 1, noticePage.getTotalPages());

        return NoticePageResponse.from(responsePages);
    }

    /**
     * 제목으로 공지사항 검색 (페이지네이션) - 필독 공지사항 우선 정렬
     */
    public NoticePageResponse searchNoticesByTitle(String title, int page, int size, SortOption sortOption) {
        log.info("공지사항 제목 검색 요청 - 키워드: {}, page: {}, size: {}, 정렬: {}", 
                title, page, size, sortOption.getDescription());

        // 페이지 번호와 크기 검증
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // 최대 100개로 제한

        // 필독 공지사항을 먼저 정렬하고, 그 다음 원하는 정렬 기준 적용
        Sort primarySort = Sort.by(Sort.Direction.DESC, "isMustRead"); // 필독 먼저
        Sort secondarySort = sortOption.getSort(); // 사용자가 선택한 정렬
        Sort combinedSort = primarySort.and(secondarySort);
        
        Pageable pageable = PageRequest.of(page, size, combinedSort);
        Page<Notice> noticePage = noticeRepository.findByTitleContainingIgnoreCase(title, pageable);

        Page<NoticeListResponse> responsePages = noticePage.map(NoticeListResponse::from);

        log.info("공지사항 검색 완료 (필독 우선) - 키워드: {}, 정렬: {}, 결과: {}개, 페이지: {}/{}", 
                title, sortOption.getDescription(), noticePage.getTotalElements(), page + 1, noticePage.getTotalPages());

        return NoticePageResponse.from(responsePages);
    }

    /**
     * 필독 공지사항만 조회 (페이지네이션)
     */
    public NoticePageResponse getMustReadNotices(int page, int size, SortOption sortOption) {
        log.info("필독 공지사항 조회 요청 - page: {}, size: {}, 정렬: {}", page, size, sortOption.getDescription());

        // 페이지 번호와 크기 검증
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // 최대 100개로 제한

        Pageable pageable = PageRequest.of(page, size, sortOption.getSort());
        Page<Notice> noticePage = noticeRepository.findByIsMustReadTrue(pageable);

        Page<NoticeListResponse> responsePages = noticePage.map(NoticeListResponse::from);

        log.info("필독 공지사항 조회 완료 - 정렬: {}, 총 {}개, 페이지: {}/{}", 
                sortOption.getDescription(), noticePage.getTotalElements(), page + 1, noticePage.getTotalPages());

        return NoticePageResponse.from(responsePages);
    }
}
