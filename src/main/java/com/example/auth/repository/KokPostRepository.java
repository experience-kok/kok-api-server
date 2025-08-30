package com.example.auth.repository;

import com.example.auth.domain.KokPost;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KokPostRepository extends JpaRepository<KokPost, Long> {

    /**
     * 모든 콕포스트를 최신순으로 조회
     */
    List<KokPost> findAllByOrderByCreatedAtDesc();

    /**
     * 캠페인별 콕포스트 목록 조회 (최신순)
     */
    List<KokPost> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);

    /**
     * 제목으로 콕포스트 검색 (대소문자 구분 없이, Sort 옵션)
     */
    List<KokPost> findByTitleContainingIgnoreCase(String title, Sort sort);

    /**
     * 조회수 상위 N개 콕포스트 조회
     */
    List<KokPost> findTop10ByOrderByViewCountDesc();

    /**
     * 캠페인별 콕포스트 개수 조회
     */
    long countByCampaignId(Long campaignId);

    /**
     * 조회수 증가
     */
    @Modifying
    @Query("UPDATE KokPost k SET k.viewCount = k.viewCount + 1 WHERE k.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
