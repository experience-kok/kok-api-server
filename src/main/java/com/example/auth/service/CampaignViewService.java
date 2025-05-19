package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.dto.campaign.CampaignListResponse;
import com.example.auth.dto.campaign.CampaignListSimpleResponse;
import com.example.auth.dto.campaign.view.*;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignViewService {

    private final CampaignRepository campaignRepository;
    private static final String APPROVED_STATUS = "APPROVED";

    /**
     * 캠페인 목록 조회 (페이징 처리) - 간소화된 응답
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignList(int page, int size, String sort, boolean onlyActive, 
                                                            String categoryType, String campaignType) {
        // 페이지 정보 생성 (첫 페이지는 0부터 시작)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort != null ? sort : "createdAt"));
        
        Page<Campaign> campaignPage;
        
        if (categoryType != null && !categoryType.isEmpty()) {
            // 카테고리별 조회
            campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryType(
                    APPROVED_STATUS, categoryType, pageable);
        } else if (campaignType != null && !campaignType.isEmpty()) {
            // 캠페인 타입별 조회
            campaignPage = campaignRepository.findByApprovalStatusAndCampaignType(
                    APPROVED_STATUS, campaignType, pageable);
        } else if (onlyActive) {
            // 마감되지 않은 캠페인만 조회
            campaignPage = campaignRepository.findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, LocalDate.now(), pageable);
        } else {
            // 모든 승인된 캠페인 조회
            campaignPage = campaignRepository.findByApprovalStatus(APPROVED_STATUS, pageable);
        }
        
        // 엔티티를 간소화된 DTO로 변환
        Page<CampaignListSimpleResponse> responsePage = campaignPage.map(campaign -> {
            CampaignListSimpleResponse response = CampaignListSimpleResponse.fromEntity(campaign);
            
            // 현재 신청 인원수 설정 (현재는 더미 데이터)
            // 실제로는 신청 테이블에서 조회해야 함
            int dummyCurrentApplicants = (int)(Math.random() * response.getMaxApplicants());
            response.setCurrentApplicants(dummyCurrentApplicants);
            
            return response;
        });
        
        return PageResponse.from(responsePage);
    }

    /**
     * 캠페인 썸네일 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public ThumbnailResponse getThumbnail(Long campaignId) {
        Campaign campaign = findApprovedCampaignById(campaignId);
        return ThumbnailResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 기본 정보 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public BasicInfoResponse getBasicInfo(Long campaignId) {
        Campaign campaign = findApprovedCampaignById(campaignId);
        return BasicInfoResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 제품 및 일정 정보 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public ProductAndScheduleResponse getProductAndSchedule(Long campaignId) {
        Campaign campaign = findApprovedCampaignById(campaignId);
        return ProductAndScheduleResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 업체 정보 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public CompanyInfoResponse getCompanyInfo(Long campaignId) {
        Campaign campaign = findApprovedCampaignById(campaignId);
        return CompanyInfoResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 미션 가이드 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public MissionGuideResponse getMissionGuide(Long campaignId) {
        Campaign campaign = findApprovedCampaignById(campaignId);
        return MissionGuideResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 미션 키워드 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public MissionKeywordsResponse getMissionKeywords(Long campaignId) {
        Campaign campaign = findApprovedCampaignById(campaignId);
        return MissionKeywordsResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 위치 정보 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public LocationInfoResponse getLocationInfo(Long campaignId) {
        Campaign campaign = findApprovedCampaignById(campaignId);
        return LocationInfoResponse.fromEntity(campaign);
    }

    /**
     * ID로 승인된 캠페인 조회
     */
    private Campaign findApprovedCampaignById(Long campaignId) {
        return campaignRepository.findByIdAndApprovalStatus(campaignId, APPROVED_STATUS)
                .orElseThrow(() -> new ResourceNotFoundException("승인된 캠페인을 찾을 수 없습니다."));
    }
}