package com.example.auth.service;

import com.example.auth.dto.banner.BannerImageResponse;
import com.example.auth.repository.BannerImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 배너 이미지 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerImageService {

    private final BannerImageRepository bannerImageRepository;

    /**
     * 모든 배너 이미지 목록을 조회합니다.
     * @return 배너 이미지 목록 (최신순)
     */
    public List<BannerImageResponse> getAllBanners() {
        log.info("모든 배너 이미지 목록 조회");
        
        return bannerImageRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(BannerImageResponse::from)
                .collect(Collectors.toList());
    }
}
