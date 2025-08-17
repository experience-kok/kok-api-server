package com.example.auth.repository;

import com.example.auth.domain.User;
import com.example.auth.domain.UserMissionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface UserMissionHistoryRepository extends JpaRepository<UserMissionHistory, Long> {

    /**
     * 유저의 공개 미션 이력 조회 (다른 클라이언트가 볼 수 있는 포트폴리오)
     */
    @Query("SELECT umh FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.isPublic = true " +
           "ORDER BY umh.completionDate DESC")
    List<UserMissionHistory> findPublicMissionHistoryByUserId(@Param("userId") Long userId);

    /**
     * 유저의 모든 미션 이력 조회 (본인만 볼 수 있음)
     */
    List<UserMissionHistory> findByUserIdOrderByCompletionDateDesc(Long userId);

    /**
     * 유저의 공개 미션 이력 페이징 조회
     */
    @Query("SELECT umh FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.isPublic = :isPublic " +
           "ORDER BY umh.completionDate DESC")
    Page<UserMissionHistory> findByUserIdAndIsPublic(
            @Param("userId") Long userId, 
            @Param("isPublic") Boolean isPublic, 
            Pageable pageable);

    /**
     * 유저의 대표 미션 조회 (Featured)
     */
    @Query("SELECT umh FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.isPublic = true " +
           "AND umh.isFeatured = true " +
           "ORDER BY umh.completionDate DESC")
    List<UserMissionHistory> findFeaturedMissionHistoryByUserId(@Param("userId") Long userId);

    /**
     * 플랫폼별 유저 미션 이력 조회
     */
    @Query("SELECT umh FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.platformType = :platformType " +
           "AND umh.isPublic = true " +
           "ORDER BY umh.completionDate DESC")
    List<UserMissionHistory> findByUserIdAndPlatformType(
            @Param("userId") Long userId, 
            @Param("platformType") String platformType);

    /**
     * 카테고리별 유저 미션 이력 조회
     */
    @Query("SELECT umh FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.campaignCategory = :category " +
           "AND umh.isPublic = true " +
           "ORDER BY umh.completionDate DESC")
    List<UserMissionHistory> findByUserIdAndCampaignCategory(
            @Param("userId") Long userId, 
            @Param("category") String category);

    /**
     * 유저의 미션 완료 통계 조회
     */
    @Query("SELECT " +
           "COUNT(umh) as totalMissions, " +
           "AVG(umh.clientRating) as avgRating, " +
           "COUNT(CASE WHEN umh.clientRating >= 4 THEN 1 END) as highRatedMissions, " +
           "COUNT(DISTINCT umh.campaignCategory) as categoriesWorked, " +
           "COUNT(DISTINCT umh.platformType) as platformsUsed " +
           "FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.isPublic = true")
    Object[] getUserMissionStatistics(@Param("userId") Long userId);

    /**
     * 특정 기간 내 완료된 미션 조회
     */
    @Query("SELECT umh FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.completionDate BETWEEN :startDate AND :endDate " +
           "AND umh.isPublic = true " +
           "ORDER BY umh.completionDate DESC")
    List<UserMissionHistory> findByUserIdAndCompletionDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    /**
     * 평점별 미션 이력 조회
     */
    @Query("SELECT umh FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.clientRating >= :minRating " +
           "AND umh.isPublic = true " +
           "ORDER BY umh.clientRating DESC, umh.completionDate DESC")
    List<UserMissionHistory> findByUserIdAndMinRating(
            @Param("userId") Long userId,
            @Param("minRating") Integer minRating);

    /**
     * 플랫폼별 유저 성과 통계
     */
    @Query("SELECT umh.platformType, " +
           "COUNT(umh) as missionCount, " +
           "AVG(umh.clientRating) as avgRating " +
           "FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.isPublic = true " +
           "GROUP BY umh.platformType " +
           "ORDER BY missionCount DESC")
    List<Object[]> getPlatformStatisticsByUserId(@Param("userId") Long userId);

    /**
     * 카테고리별 유저 성과 통계
     */
    @Query("SELECT umh.campaignCategory, " +
           "COUNT(umh) as missionCount, " +
           "AVG(umh.clientRating) as avgRating " +
           "FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.isPublic = true " +
           "GROUP BY umh.campaignCategory " +
           "ORDER BY missionCount DESC")
    List<Object[]> getCategoryStatisticsByUserId(@Param("userId") Long userId);

    /**
     * 최근 활동한 인플루언서 조회 (추천용)
     */
    @Query("SELECT DISTINCT umh.user FROM UserMissionHistory umh " +
           "WHERE umh.completionDate >= :sinceDate " +
           "AND umh.isPublic = true " +
           "ORDER BY umh.completionDate DESC")
    List<User> findActiveInfluencersSince(@Param("sinceDate") ZonedDateTime sinceDate);

    /**
     * 특정 카테고리에서 경험이 많은 인플루언서 조회
     */
    @Query("SELECT umh.user, COUNT(umh) as missionCount, AVG(umh.clientRating) as avgRating " +
           "FROM UserMissionHistory umh " +
           "WHERE umh.campaignCategory = :category " +
           "AND umh.isPublic = true " +
           "GROUP BY umh.user " +
           "HAVING COUNT(umh) >= :minMissionCount " +
           "ORDER BY missionCount DESC, avgRating DESC")
    List<Object[]> findExperiencedInfluencersInCategory(
            @Param("category") String category,
            @Param("minMissionCount") Long minMissionCount);

    /**
     * 유저별 최근 완료 미션 수 조회
     */
    @Query("SELECT COUNT(umh) FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.completionDate >= :sinceDate " +
           "AND umh.isPublic = true")
    Long countRecentMissionsByUserId(
            @Param("userId") Long userId,
            @Param("sinceDate") ZonedDateTime sinceDate);

    /**
     * 대표 미션 설정 가능 여부 확인 (최대 3개 제한)
     */
    @Query("SELECT COUNT(umh) FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "AND umh.isFeatured = true")
    Long countFeaturedMissionsByUserId(@Param("userId") Long userId);

    /**
     * 캠페인별 참여 이력 확인
     */
    boolean existsByUserIdAndCampaignId(Long userId, Long campaignId);

    /**
     * 유저별 완료 미션 수 조회
     */
    Long countByUserId(Long userId);

    /**
     * 유저별 평균 평점 조회
     */
    @Query("SELECT AVG(umh.clientRating) FROM UserMissionHistory umh WHERE umh.user.id = :userId")
    Double findAverageRatingByUserId(@Param("userId") Long userId);

    /**
     * 캠페인별 평균 평점 조회
     */
    @Query("SELECT AVG(umh.clientRating) FROM UserMissionHistory umh WHERE umh.campaign.id = :campaignId")
    Double findAverageRatingByCampaignId(@Param("campaignId") Long campaignId);

    /**
     * 유저별 카테고리별 미션 수 조회
     */
    @Query("SELECT umh.campaignCategory, COUNT(umh) FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "GROUP BY umh.campaignCategory")
    List<Object[]> findMissionCountByCategory(@Param("userId") Long userId);

    /**
     * 유저별 플랫폼별 미션 수 조회
     */
    @Query("SELECT umh.platformType, COUNT(umh) FROM UserMissionHistory umh " +
           "WHERE umh.user.id = :userId " +
           "GROUP BY umh.platformType")
    List<Object[]> findMissionCountByPlatform(@Param("userId") Long userId);

    /**
     * 유저별 공개 미션 수 조회
     */
    Long countByUserIdAndIsPublic(Long userId, Boolean isPublic);

    /**
     * 전체 공개 미션 이력 수 조회 (포트폴리오 검색용)
     */
    @Query("SELECT COUNT(umh) FROM UserMissionHistory umh " +
           "WHERE umh.isPublic = true")
    Long countPublicMissionHistory();

    /**
     * 검색을 위한 유저 미션 이력 조회
     */
    @Query("SELECT umh FROM UserMissionHistory umh " +
           "WHERE umh.isPublic = true " +
           "AND (LOWER(umh.campaignTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(umh.campaignCategory) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY umh.completionDate DESC")
    Page<UserMissionHistory> searchPublicMissionHistory(
            @Param("keyword") String keyword, 
            Pageable pageable);
}
