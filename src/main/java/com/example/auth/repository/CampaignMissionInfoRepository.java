package com.example.auth.repository;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignMissionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 캠페인 미션 정보 Repository
 */
@Repository
public interface CampaignMissionInfoRepository extends JpaRepository<CampaignMissionInfo, Long> {

    /**
     * 캠페인으로 미션 정보 조회
     * @param campaign 캠페인
     * @return 미션 정보
     */
    Optional<CampaignMissionInfo> findByCampaign(Campaign campaign);

    /**
     * 캠페인 ID로 미션 정보 조회
     * @param campaignId 캠페인 ID
     * @return 미션 정보
     */
    Optional<CampaignMissionInfo> findByCampaignId(Long campaignId);

    /**
     * 미션 시작일이 특정 날짜 이후인 미션 정보 목록 조회
     * @param date 기준 날짜
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByMissionStartDateAfter(LocalDate date);

    /**
     * 미션 마감일이 특정 날짜 이전인 미션 정보 목록 조회
     * @param date 기준 날짜
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByMissionDeadlineDateBefore(LocalDate date);

    /**
     * 특정 기간 내의 미션 정보 목록 조회
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByMissionStartDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 진행 중인 미션 정보 목록 조회 (현재 날짜가 미션 기간 내)
     * @param currentDate 현재 날짜
     * @return 진행 중인 미션 정보 목록
     */
    @Query("SELECT cmi FROM CampaignMissionInfo cmi " +
           "WHERE cmi.missionStartDate <= :currentDate " +
           "AND cmi.missionDeadlineDate >= :currentDate")
    List<CampaignMissionInfo> findActiveMissions(@Param("currentDate") LocalDate currentDate);

    /**
     * 진행 중인 미션 정보 목록 조회 (네이티브 쿼리 버전)
     * @param currentDate 현재 날짜
     * @return 진행 중인 미션 정보 목록
     */
    @Query(value = "SELECT * FROM campaign_mission_info " +
                   "WHERE mission_start_date <= :currentDate " +
                   "AND mission_deadline_date >= :currentDate", 
           nativeQuery = true)
    List<CampaignMissionInfo> findActiveMissionsNative(@Param("currentDate") LocalDate currentDate);

    /**
     * 지도 포함 여부로 미션 정보 조회
     * @param isMap 지도 포함 여부
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByIsMap(Boolean isMap);

    /**
     * 영상 개수가 특정 수 이상인 미션 정보 조회
     * @param minVideoCount 최소 영상 개수
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByNumberOfVideoGreaterThanEqual(Integer minVideoCount);

    /**
     * 이미지 개수가 특정 수 이상인 미션 정보 조회
     * @param minImageCount 최소 이미지 개수
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByNumberOfImageGreaterThanEqual(Integer minImageCount);

    /**
     * 글자 수가 특정 수 이상인 미션 정보 조회
     * @param minTextCount 최소 글자 수
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByNumberOfTextGreaterThanEqual(Integer minTextCount);

    /**
     * 특정 본문 키워드를 포함하는 미션 정보 조회 (네이티브 쿼리 사용)
     * @param keyword 검색할 키워드
     * @return 미션 정보 목록
     */
    @Query(value = "SELECT * FROM campaign_mission_info cmi " +
                   "WHERE :keyword = ANY(cmi.body_keywords)", 
           nativeQuery = true)
    List<CampaignMissionInfo> findByBodyKeywordsContaining(@Param("keyword") String keyword);

    /**
     * 특정 본문 키워드를 포함하는 미션 정보 조회 (대안 메소드)
     * @param keyword 검색할 키워드
     * @return 미션 정보 목록
     */
    @Query(value = "SELECT * FROM campaign_mission_info cmi " +
                   "WHERE array_position(cmi.body_keywords, :keyword) IS NOT NULL", 
           nativeQuery = true)
    List<CampaignMissionInfo> findMissionsByBodyKeyword(@Param("keyword") String keyword);

    /**
     * 본문 키워드 배열에서 키워드를 포함하는 미션 정보 조회 (LIKE 검색)
     * @param keyword 검색할 키워드 (부분 일치)
     * @return 미션 정보 목록
     */
    @Query(value = "SELECT * FROM campaign_mission_info cmi " +
                   "WHERE EXISTS (SELECT 1 FROM unnest(cmi.body_keywords) AS kw WHERE kw ILIKE %:keyword%)", 
           nativeQuery = true)
    List<CampaignMissionInfo> findByBodyKeywordLike(@Param("keyword") String keyword);

    /**
     * 특정 제목 키워드를 포함하는 미션 정보 조회
     * @param keyword 검색할 키워드
     * @return 미션 정보 목록
     */
    @Query(value = "SELECT * FROM campaign_mission_info cmi " +
                   "WHERE :keyword = ANY(cmi.title_keywords)", 
           nativeQuery = true)
    List<CampaignMissionInfo> findByTitleKeywordsContaining(@Param("keyword") String keyword);

    /**
     * 특정 지역 키워드를 포함하는 미션 정보 조회
     * @param keyword 검색할 지역 키워드
     * @return 미션 정보 목록
     */
    @Query(value = "SELECT * FROM campaign_mission_info cmi " +
                   "WHERE :keyword = ANY(cmi.location_keywords)", 
           nativeQuery = true)
    List<CampaignMissionInfo> findByLocationKeywordsContaining(@Param("keyword") String keyword);

    /**
     * 미션 가이드에 특정 텍스트를 포함하는 미션 정보 조회
     * @param searchText 검색할 텍스트
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByMissionGuideContainingIgnoreCase(String searchText);

    /**
     * 캠페인 ID 목록으로 미션 정보 조회
     * @param campaignIds 캠페인 ID 목록
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByCampaignIdIn(List<Long> campaignIds);

    /**
     * 미션 정보가 없는 캠페인 ID 조회
     * @return 미션 정보가 없는 캠페인 ID 목록
     */
    @Query("SELECT c.id FROM Campaign c " +
           "WHERE c.id NOT IN (SELECT cmi.campaign.id FROM CampaignMissionInfo cmi)")
    List<Long> findCampaignIdsWithoutMissionInfo();

    /**
     * 콘텐츠 요구사항이 있는 미션 정보 조회
     * @return 콘텐츠 요구사항이 있는 미션 정보 목록
     */
    @Query("SELECT cmi FROM CampaignMissionInfo cmi " +
           "WHERE (cmi.numberOfVideo > 0 OR cmi.numberOfImage > 0 OR cmi.numberOfText > 0)")
    List<CampaignMissionInfo> findMissionsWithContentRequirements();

    /**
     * 완전한 미션 정보를 가진 항목 조회 (모든 필수 필드가 설정된 경우)
     * @return 완전한 미션 정보 목록
     */
    @Query("SELECT cmi FROM CampaignMissionInfo cmi " +
           "WHERE cmi.missionStartDate IS NOT NULL " +
           "AND cmi.missionDeadlineDate IS NOT NULL " +
           "AND cmi.missionGuide IS NOT NULL " +
           "AND LENGTH(TRIM(cmi.missionGuide)) > 0")
    List<CampaignMissionInfo> findCompleteMissionInfo();

    /**
     * 특정 날짜에 시작하는 미션 정보 조회
     * @param startDate 시작 날짜
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByMissionStartDate(LocalDate startDate);

    /**
     * 특정 날짜에 마감되는 미션 정보 조회
     * @param deadlineDate 마감 날짜
     * @return 미션 정보 목록
     */
    List<CampaignMissionInfo> findByMissionDeadlineDate(LocalDate deadlineDate);

    /**
     * 캠페인 존재 여부 확인
     * @param campaignId 캠페인 ID
     * @return 존재 여부
     */
    boolean existsByCampaignId(Long campaignId);

    /**
     * 캠페인으로 미션 정보 삭제
     * @param campaign 캠페인
     * @return 삭제된 행 수
     */
    int deleteByCampaign(Campaign campaign);

    /**
     * 캠페인 ID로 미션 정보 삭제
     * @param campaignId 캠페인 ID
     * @return 삭제된 행 수
     */
    int deleteByCampaignId(Long campaignId);

    /**
     * 키워드 통계 조회 - 가장 많이 사용된 본문 키워드
     * @param limit 조회할 개수
     * @return 키워드별 사용 횟수
     */
    @Query(value = "SELECT unnest(body_keywords) as keyword, COUNT(*) as usage_count " +
                   "FROM campaign_mission_info " +
                   "WHERE body_keywords IS NOT NULL " +
                   "GROUP BY keyword " +
                   "ORDER BY usage_count DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopBodyKeywords(@Param("limit") int limit);

    /**
     * 키워드 통계 조회 - 가장 많이 사용된 제목 키워드
     * @param limit 조회할 개수
     * @return 키워드별 사용 횟수
     */
    @Query(value = "SELECT unnest(title_keywords) as keyword, COUNT(*) as usage_count " +
                   "FROM campaign_mission_info " +
                   "WHERE title_keywords IS NOT NULL " +
                   "GROUP BY keyword " +
                   "ORDER BY usage_count DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopTitleKeywords(@Param("limit") int limit);

    /**
     * 지역별 미션 통계 조회
     * @param limit 조회할 개수
     * @return 지역별 미션 수
     */
    @Query(value = "SELECT unnest(location_keywords) as location, COUNT(*) as mission_count " +
                   "FROM campaign_mission_info " +
                   "WHERE location_keywords IS NOT NULL " +
                   "GROUP BY location " +
                   "ORDER BY mission_count DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopLocationKeywords(@Param("limit") int limit);

    /**
     * 미션 기간별 통계 조회 (네이티브 쿼리 사용)
     * @return 평균 미션 기간 (일)
     */
    @Query(value = "SELECT AVG(mission_deadline_date - mission_start_date + 1) " +
                   "FROM campaign_mission_info " +
                   "WHERE mission_start_date IS NOT NULL AND mission_deadline_date IS NOT NULL", 
           nativeQuery = true)
    Double findAverageMissionDuration();

    /**
     * 콘텐츠 요구사항별 미션 수 조회
     * @return 콘텐츠 타입별 미션 수
     */
    @Query("SELECT " +
           "SUM(CASE WHEN cmi.numberOfVideo > 0 THEN 1 ELSE 0 END) as videoMissions, " +
           "SUM(CASE WHEN cmi.numberOfImage > 0 THEN 1 ELSE 0 END) as imageMissions, " +
           "SUM(CASE WHEN cmi.numberOfText > 0 THEN 1 ELSE 0 END) as textMissions, " +
           "SUM(CASE WHEN cmi.isMap = true THEN 1 ELSE 0 END) as mapMissions " +
           "FROM CampaignMissionInfo cmi")
    Object[] findContentRequirementStatistics();

    /**
     * 특정 기간 동안 생성된 미션 정보 조회
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 미션 정보 목록
     */
    @Query("SELECT cmi FROM CampaignMissionInfo cmi " +
           "WHERE cmi.createdAt >= :startDate AND cmi.createdAt <= :endDate")
    List<CampaignMissionInfo> findByCreatedAtBetween(@Param("startDate") java.time.ZonedDateTime startDate, 
                                                     @Param("endDate") java.time.ZonedDateTime endDate);

    /**
     * 최근 생성된 미션 정보 조회
     * @param sinceDate 기준 날짜
     * @return 최근 미션 정보 목록
     */
    @Query("SELECT cmi FROM CampaignMissionInfo cmi " +
           "WHERE cmi.createdAt >= :sinceDate " +
           "ORDER BY cmi.createdAt DESC")
    List<CampaignMissionInfo> findRecentMissions(@Param("sinceDate") java.time.ZonedDateTime sinceDate);
}
