package com.example.auth.repository;

import com.example.auth.domain.KokPost;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KokPostRepository extends JpaRepository<KokPost, Long> {

    /**
     * 캠페인 ID로 체험콕 포스트 조회
     */
    Optional<KokPost> findByCampaignId(Long campaignId);

    /**
     * 캠페인 ID로 체험콕 포스트 목록 조회 (생성일 기준 내림차순)
     */
    List<KokPost> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);

    /**
     * 전체 포스트 목록 조회 (생성일 기준 내림차순)
     */
    List<KokPost> findAllByOrderByCreatedAtDesc();

    /**
     * 제목으로 검색 (대소문자 구분 없음)
     */
    @Query("SELECT k FROM KokPost k WHERE LOWER(k.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<KokPost> findByTitleContainingIgnoreCase(@Param("title") String title, Sort sort);

    /**
     * 조회수 기준 인기 포스트 조회 (개선된 버전)
     */
    @Query(value = """
        SELECT k FROM KokPost k 
        ORDER BY k.viewCount DESC, k.createdAt DESC
        """)
    List<KokPost> findTopByOrderByViewCountDesc(@Param("limit") int limit);

    /**
     * 조회수를 특정 양만큼 증가 (원자적 연산)
     */
    @Modifying
    @Query("UPDATE KokPost k SET k.viewCount = k.viewCount + :amount WHERE k.id = :postId")
    int incrementViewCountByAmount(@Param("postId") Long postId, @Param("amount") Long amount);

    /**
     * 조회수 1 증가 (단일 증가용)
     */
    @Modifying
    @Query("UPDATE KokPost k SET k.viewCount = k.viewCount + 1 WHERE k.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * 여러 포스트의 조회수를 배치로 업데이트
     */
    @Modifying
    @Query(value = """
        UPDATE kokposts 
        SET view_count = view_count + :amount 
        WHERE id IN :postIds
        """, nativeQuery = true)
    int batchIncrementViewCount(@Param("postIds") List<Long> postIds, @Param("amount") Long amount);

    /**
     * 조회수가 특정 값 이상인 포스트 조회
     */
    @Query("SELECT k FROM KokPost k WHERE k.viewCount >= :minViewCount ORDER BY k.viewCount DESC")
    List<KokPost> findByViewCountGreaterThanEqual(@Param("minViewCount") Long minViewCount);

    /**
     * 캠페인 ID 목록으로 포스트 존재 여부 확인
     */
    @Query("SELECT k.campaignId FROM KokPost k WHERE k.campaignId IN :campaignIds")
    List<Long> findExistingCampaignIds(@Param("campaignIds") List<Long> campaignIds);

    /**
     * 조회수 통계 조회
     */
    @Query("""
        SELECT 
            COUNT(k) as totalPosts,
            SUM(k.viewCount) as totalViews,
            AVG(k.viewCount) as avgViews,
            MAX(k.viewCount) as maxViews,
            MIN(k.viewCount) as minViews
        FROM KokPost k
        """)
    ViewCountStatistics getViewCountStatistics();

    /**
     * 최근 생성된 포스트 중 조회수 상위 N개 조회
     */
    @Query(value = """
        SELECT * FROM kokposts 
        WHERE created_at >= NOW() - INTERVAL '7 days'
        ORDER BY view_count DESC, created_at DESC 
        LIMIT :limit
        """, nativeQuery = true)
    List<KokPost> findRecentPopularPosts(@Param("limit") int limit);

    /**
     * 조회수가 0인 포스트 개수 조회
     */
    @Query("SELECT COUNT(k) FROM KokPost k WHERE k.viewCount = 0")
    long countPostsWithZeroViews();

    /**
     * 특정 기간 동안 생성된 포스트의 총 조회수
     */
    @Query(value = """
        SELECT COALESCE(SUM(view_count), 0) 
        FROM kokposts 
        WHERE created_at BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    long getTotalViewsInPeriod(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 조회수 순위별 포스트 조회 (페이지네이션 지원)
     */
    @Query(value = """
        SELECT *, 
               ROW_NUMBER() OVER (ORDER BY view_count DESC, created_at DESC) as rank
        FROM kokposts 
        ORDER BY view_count DESC, created_at DESC
        OFFSET :offset LIMIT :limit
        """, nativeQuery = true)
    List<KokPost> findPostsWithRanking(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * ViewCount 통계를 위한 인터페이스
     */
    interface ViewCountStatistics {
        Long getTotalPosts();
        Long getTotalViews();
        Double getAvgViews();
        Long getMaxViews();
        Long getMinViews();
    }
}