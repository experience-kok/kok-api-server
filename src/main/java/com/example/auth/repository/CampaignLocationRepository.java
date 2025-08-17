package com.example.auth.repository;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 캠페인 위치 정보 Repository (간소화)
 */
@Repository
public interface CampaignLocationRepository extends JpaRepository<CampaignLocation, Long> {

    /**
     * 특정 캠페인의 위치 정보 조회
     * @param campaign 캠페인
     * @return 위치 정보 (Optional)
     */
    Optional<CampaignLocation> findByCampaign(Campaign campaign);

    /**
     * 특정 캠페인 ID의 위치 정보 조회
     * @param campaignId 캠페인 ID
     * @return 위치 정보 (Optional)
     */
    Optional<CampaignLocation> findByCampaignId(Long campaignId);


    /**
     * 좌표가 있는 위치 정보 조회
     * @return 위치 목록
     */
    @Query("SELECT cl FROM CampaignLocation cl WHERE cl.latitude IS NOT NULL AND cl.longitude IS NOT NULL")
    List<CampaignLocation> findByCoordinatesNotNull();

    /**
     * 특정 캠페인의 위치 정보 존재 여부 확인
     * @param campaignId 캠페인 ID
     * @return 존재 여부
     */
    boolean existsByCampaignId(Long campaignId);

    /**
     * 특정 캠페인의 위치 정보 삭제
     * @param campaignId 캠페인 ID
     */
    void deleteByCampaignId(Long campaignId);
}
