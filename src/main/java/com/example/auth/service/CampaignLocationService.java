package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignLocation;
import com.example.auth.dto.location.CampaignLocationRequest;
import com.example.auth.dto.location.CampaignLocationResponse;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignLocationRepository;
import com.example.auth.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캠페인 위치 정보 서비스 (간소화)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignLocationService {

    private final CampaignLocationRepository locationRepository;
    private final CampaignRepository campaignRepository;

    /**
     * 캠페인 위치 정보 조회
     * @param campaignId 캠페인 ID
     * @return 위치 정보 (없으면 null)
     */
    @Transactional(readOnly = true)
    public CampaignLocationResponse getLocation(Long campaignId) {
        // 캠페인 존재 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        CampaignLocation location = locationRepository.findByCampaignId(campaignId).orElse(null);
        return location != null ? CampaignLocationResponse.fromEntity(location) : null;
    }

    /**
     * 캠페인 위치 정보 설정 (추가 또는 수정)
     * @param campaignId 캠페인 ID
     * @param request 위치 정보 요청
     * @return 설정된 위치 정보
     */
    @Transactional
    public CampaignLocationResponse setLocation(Long campaignId, CampaignLocationRequest request) {
        // 캠페인 존재 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        // 좌표 유효성 검증
        if (!request.hasValidCoordinates()) {
            throw new IllegalArgumentException("위도와 경도는 함께 입력하거나 모두 비워야 합니다.");
        }

        // 기존 위치 정보 조회
        CampaignLocation location = locationRepository.findByCampaignId(campaignId)
                .orElse(null);

        if (location == null) {
            // 새로 생성
            location = CampaignLocation.builder()
                    .campaign(campaign)
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .homepage(request.getHomepage())
                    .contactPhone(request.getContactPhone())
                    .visitAndReservationInfo(request.getVisitAndReservationInfo())
                    .build();
            log.info("캠페인 위치 정보 생성: campaignId={}, 좌표=({},{})", campaignId, request.getLatitude(), request.getLongitude());
        } else {
            // 기존 정보 수정
            location.setLatitude(request.getLatitude());
            location.setLongitude(request.getLongitude());
            location.setHomepage(request.getHomepage());
            location.setContactPhone(request.getContactPhone());
            location.setVisitAndReservationInfo(request.getVisitAndReservationInfo());
            log.info("캠페인 위치 정보 수정: campaignId={}, 좌표=({},{})", campaignId, request.getLatitude(), request.getLongitude());
        }

        CampaignLocation savedLocation = locationRepository.save(location);
        return CampaignLocationResponse.fromEntity(savedLocation);
    }

    /**
     * 캠페인 위치 정보 삭제
     * @param campaignId 캠페인 ID
     */
    @Transactional
    public void deleteLocation(Long campaignId) {
        // 캠페인 존재 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        CampaignLocation location = locationRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("위치 정보를 찾을 수 없습니다. 캠페인 ID: " + campaignId));

        locationRepository.delete(location);
        log.info("캠페인 위치 정보 삭제: campaignId={}", campaignId);
    }

    /**
     * 위치 정보 존재 여부 확인
     * @param campaignId 캠페인 ID
     * @return 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean hasLocation(Long campaignId) {
        return locationRepository.existsByCampaignId(campaignId);
    }

    /**
     * 방문 정보와 함께 캠페인 위치 정보 생성
     * @param campaign 캠페인
     * @param visitInfo 방문 정보
     * @return 생성된 위치 정보
     */
    @Transactional
    public CampaignLocationResponse createCampaignLocationWithVisitInfo(Campaign campaign, 
                                                                       com.example.auth.dto.campaign.CreateCampaignRequest.VisitInfo visitInfo) {
        CampaignLocation location = CampaignLocation.builder()
                .campaign(campaign)
                .latitude(visitInfo.getLat())
                .longitude(visitInfo.getLng())
                .homepage(visitInfo.getHomepage())
                .contactPhone(visitInfo.getContactPhone())
                .visitAndReservationInfo(visitInfo.getVisitAndReservationInfo())
                .businessAddress(visitInfo.getBusinessAddress())
                .businessDetailAddress(visitInfo.getBusinessDetailAddress())
                .build();

        CampaignLocation savedLocation = locationRepository.save(location);
        log.info("방문 정보와 함께 캠페인 위치 정보 생성: campaignId={}", campaign.getId());
        
        return CampaignLocationResponse.fromEntity(savedLocation);
    }

    /**
     * 방문 정보와 함께 캠페인 위치 정보 업데이트
     * @param campaign 캠페인
     * @param visitInfo 방문 정보
     * @return 업데이트된 위치 정보
     */
    @Transactional
    public CampaignLocationResponse updateCampaignLocationWithVisitInfo(Campaign campaign, 
                                                                       com.example.auth.dto.campaign.CreateCampaignRequest.VisitInfo visitInfo) {
        // 기존 위치 정보 조회
        CampaignLocation location = locationRepository.findByCampaignId(campaign.getId())
                .orElse(null);

        if (location == null) {
            // 새로 생성
            return createCampaignLocationWithVisitInfo(campaign, visitInfo);
        } else {
            // 기존 정보 업데이트
            location.setLatitude(visitInfo.getLat());
            location.setLongitude(visitInfo.getLng());
            location.setHomepage(visitInfo.getHomepage());
            location.setContactPhone(visitInfo.getContactPhone());
            location.setVisitAndReservationInfo(visitInfo.getVisitAndReservationInfo());
            location.setBusinessAddress(visitInfo.getBusinessAddress());
            location.setBusinessDetailAddress(visitInfo.getBusinessDetailAddress());
            
            CampaignLocation savedLocation = locationRepository.save(location);
            log.info("방문 정보와 함께 캠페인 위치 정보 업데이트: campaignId={}", campaign.getId());
            
            return CampaignLocationResponse.fromEntity(savedLocation);
        }
    }

    /**
     * 방문 정보만 업데이트 (상시 수정 가능한 필드들만)
     * @param campaign 캠페인
     * @param visitInfo 방문 정보 (UpdateCampaignRequest용)
     */
    @Transactional
    public void updateCampaignLocationVisitInfo(Campaign campaign, 
                                               com.example.auth.dto.campaign.UpdateCampaignRequest.VisitInfo visitInfo) {
        // 기존 위치 정보 조회
        CampaignLocation location = locationRepository.findByCampaignId(campaign.getId())
                .orElse(null);

        if (location != null) {
            // 상시 수정 가능한 필드들만 업데이트
            if (visitInfo.getOfficialWebsite() != null) {
                location.setHomepage(visitInfo.getOfficialWebsite());
            }
            if (visitInfo.getContactNumber() != null) {
                location.setContactPhone(visitInfo.getContactNumber());
            }
            if (visitInfo.getVisitReservationInfo() != null) {
                location.setVisitAndReservationInfo(visitInfo.getVisitReservationInfo());
            }
            
            locationRepository.save(location);
            log.info("방문 정보 상시 수정 가능한 필드들 업데이트: campaignId={}", campaign.getId());
        } else {
            log.warn("방문 정보를 업데이트하려 했으나 위치 정보가 존재하지 않습니다. 캠페인 ID: {}", campaign.getId());
        }
    }
}
