package com.example.auth.repository;

import com.example.auth.domain.BannerImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 배너 이미지 엔티티에 대한 데이터 접근 레포지토리
 */
@Repository
public interface BannerImageRepository extends JpaRepository<BannerImage, Long> {

    /**
     * 모든 배너를 생성일 기준 내림차순으로 조회합니다.
     * @return 배너 목록 (최신순)
     */
    @Query("SELECT b FROM BannerImage b ORDER BY b.displayOrder ASC")
    List<BannerImage> findAllOrderByCreatedAtDesc();
}
