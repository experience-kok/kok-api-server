package com.example.auth.repository;

import com.example.auth.domain.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 캠페인 좋아요 Repository
 */
public interface LikeRepository extends JpaRepository<Like, Long> {

    /**
     * 특정 캠페인의 좋아요 수 조회
     */
    long countByCampaignId(Long campaignId);

    /**
     * 사용자가 특정 캠페인에 좋아요 했는지 확인
     */
    boolean existsByUserIdAndCampaignId(Long userId, Long campaignId);

    /**
     * 사용자의 특정 캠페인 좋아요 조회
     */
    Optional<Like> findByUserIdAndCampaignId(Long userId, Long campaignId);

    /**
     * 사용자가 좋아요한 캠페인 목록 (페이징)
     */
    @Query("SELECT l FROM Like l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    Page<Like> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 캠페인을 좋아요한 사용자 목록 (페이징)
     */
    @Query("SELECT l FROM Like l WHERE l.campaignId = :campaignId ORDER BY l.createdAt DESC")
    Page<Like> findByCampaignIdOrderByCreatedAtDesc(@Param("campaignId") Long campaignId, Pageable pageable);

    /**
     * 여러 캠페인의 좋아요 수를 한번에 조회 (성능 최적화)
     */
    @Query("SELECT l.campaignId, COUNT(l) FROM Like l WHERE l.campaignId IN :campaignIds GROUP BY l.campaignId")
    List<Object[]> countLikesByCampaignIds(@Param("campaignIds") List<Long> campaignIds);

    /**
     * 특정 사용자가 여러 캠페인에 좋아요 했는지 확인 (성능 최적화)
     */
    @Query("SELECT l.campaignId FROM Like l WHERE l.userId = :userId AND l.campaignId IN :campaignIds")
    List<Long> findLikedCampaignIdsByUserId(@Param("userId") Long userId, @Param("campaignIds") List<Long> campaignIds);
}
