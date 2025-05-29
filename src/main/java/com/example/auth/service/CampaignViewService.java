package com.example.auth.service;

import com.example.auth.domain.Campaign;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        // 정렬 기준에 따라 페이지 정보 생성
        Pageable pageable;
        boolean sortByCurrentApplicants = "currentApplicants".equals(sort);
        
        if (sortByCurrentApplicants) {
            // currentApplicants로 정렬하는 경우 쿼리에서 ORDER BY를 처리하므로 Pageable에는 정렬 없이 생성
            pageable = PageRequest.of(page, size);
        } else {
            // 기본 정렬 처리
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort != null ? sort : "createdAt"));
        }
        
        Page<Campaign> campaignPage;
        
        if (sortByCurrentApplicants) {
            // 신청 인원수 기준 정렬 (신청 많은 순)
            campaignPage = getCampaignPageSortedByCurrentApplicants(categoryType, campaignType, onlyActive, pageable);
        } else {
            // 기존 정렬 방식
            campaignPage = getCampaignPageWithStandardSort(categoryType, campaignType, onlyActive, pageable);
        }
        
        // 엔티티를 간소화된 DTO로 변환
        Page<CampaignListSimpleResponse> responsePage = campaignPage.map(campaign -> {
            CampaignListSimpleResponse response = CampaignListSimpleResponse.fromEntity(campaign);
            return response;
        });
        
        // 신청 인원수를 배치로 조회하여 설정 (성능 최적화)
        List<CampaignListSimpleResponse> campaigns = responsePage.getContent();
        if (!campaigns.isEmpty()) {
            // 임시로 0으로 설정 (실제로는 CampaignApplication 테이블에서 조회)
            campaigns.forEach(campaign -> campaign.setCurrentApplicants(0));
        }
        
        return PageResponse.from(responsePage);
    }

    /**
     * 마감 임박순 캠페인 목록 조회 (페이징 처리) - 활성 캠페인만
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignListByDeadlineSoon(int page, int size, 
                                                                          String categoryType, String campaignType) {
        // 마감일 오름차순 정렬 (마감 가까운 순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "applicationDeadlineDate"));
        
        // 모든 활성 캠페인 조회 (마감되지 않은 캠페인만)
        Page<Campaign> campaignPage;
        
        if (categoryType != null && !categoryType.isEmpty()) {
            campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, categoryType, LocalDate.now(), pageable);
        } else if (campaignType != null && !campaignType.isEmpty()) {
            campaignPage = campaignRepository.findByApprovalStatusAndCampaignTypeAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, campaignType, LocalDate.now(), pageable);
        } else {
            campaignPage = campaignRepository.findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, LocalDate.now(), pageable);
        }
        
        // 엔티티를 간소화된 DTO로 변환
        Page<CampaignListSimpleResponse> responsePage = campaignPage.map(campaign -> {
            CampaignListSimpleResponse response = CampaignListSimpleResponse.fromEntity(campaign);
            return response;
        });
        
        // 신청 인원수를 배치로 조회하여 설정 (성능 최적화)
        List<CampaignListSimpleResponse> campaigns = responsePage.getContent();
        if (!campaigns.isEmpty()) {
            // 임시로 0으로 설정 (실제로는 CampaignApplication 테이블에서 조회)
            campaigns.forEach(campaign -> campaign.setCurrentApplicants(0));
        }
        
        return PageResponse.from(responsePage);
    }
    
    /**
     * 세분화된 필터 조건으로 캠페인 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getFilteredCampaignList(
            int page, int size, String categoryType, String categoryName, 
            String campaignTypes, String sort) {
        
        // 정렬 기준 변환
        String actualSort = convertSortParameter(sort);
        boolean sortByCurrentApplicants = "currentApplicants".equals(actualSort);
        boolean sortByDeadline = "applicationDeadlineDate".equals(actualSort);
        
        // 캠페인 타입 파싱 (쉼표로 구분된 문자열을 List로 변환)
        List<String> campaignTypeList = null;
        if (campaignTypes != null && !campaignTypes.trim().isEmpty()) {
            campaignTypeList = Arrays.stream(campaignTypes.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        
        // 페이지 정보 생성
        Pageable pageable;
        if (sortByCurrentApplicants) {
            pageable = PageRequest.of(page, size);
        } else if (sortByDeadline) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "applicationDeadlineDate"));
        } else {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        
        Page<Campaign> campaignPage;
        
        if (sortByCurrentApplicants) {
            // 인기순 정렬
            campaignPage = getFilteredCampaignPageByPopularity(categoryType, categoryName, campaignTypeList, pageable);
        } else {
            // 일반 정렬
            campaignPage = getFilteredCampaignPageByStandardSort(categoryType, categoryName, campaignTypeList, pageable);
        }
        
        // DTO 변환
        Page<CampaignListSimpleResponse> responsePage = campaignPage.map(CampaignListSimpleResponse::fromEntity);
        
        // 신청 인원수를 배치로 조회하여 설정
        List<CampaignListSimpleResponse> campaigns = responsePage.getContent();
        if (!campaigns.isEmpty()) {
            // 임시로 0으로 설정 (실제로는 CampaignApplication 테이블에서 조회)
            campaigns.forEach(campaign -> campaign.setCurrentApplicants(0));
        }
        
        return PageResponse.from(responsePage);
    }
    
    /**
     * 정렬 파라미터 변환
     */
    private String convertSortParameter(String sort) {
        return switch (sort) {
            case "popular" -> "currentApplicants";
            case "deadline" -> "applicationDeadlineDate"; 
            case "latest" -> "createdAt";
            default -> "createdAt";
        };
    }
    
    /**
     * 인기순 정렬을 위한 필터링된 캠페인 조회
     */
    private Page<Campaign> getFilteredCampaignPageByPopularity(String categoryType, String categoryName, 
                                                              List<String> campaignTypes, Pageable pageable) {
        // 실제로는 JOIN을 통한 복잡한 쿼리가 필요하지만, 임시로 기본 조회로 처리
        return getFilteredCampaignPageByStandardSort(categoryType, categoryName, campaignTypes, pageable);
    }
    
    /**
     * 일반 정렬을 위한 필터링된 캠페인 조회
     */
    private Page<Campaign> getFilteredCampaignPageByStandardSort(String categoryType, String categoryName, 
                                                                List<String> campaignTypes, Pageable pageable) {
        if (categoryName != null && campaignTypes != null && !campaignTypes.isEmpty()) {
            // 카테고리명과 플랫폼 타입 모두 필터링
            return campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndCampaignTypeInAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, categoryType, categoryName, campaignTypes, LocalDate.now(), pageable);
        } else if (categoryName != null) {
            // 카테고리명만 필터링
            return campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, categoryType, categoryName, LocalDate.now(), pageable);
        } else if (campaignTypes != null && !campaignTypes.isEmpty()) {
            // 플랫폼 타입만 필터링
            return campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndCampaignTypeInAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, categoryType, campaignTypes, LocalDate.now(), pageable);
        } else {
            // 카테고리 타입만 필터링
            return campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, categoryType, LocalDate.now(), pageable);
        }
    }

    /**
     * 신청자 수 기준으로 정렬된 캠페인 페이지 조회
     */
    private Page<Campaign> getCampaignPageSortedByCurrentApplicants(String categoryType, String campaignType, 
                                                                   boolean onlyActive, Pageable pageable) {
        if (categoryType != null && !categoryType.isEmpty()) {
            // 카테고리별 조회 (신청 많은 순)
            return campaignRepository.findByApprovalStatusAndCategoryCategoryTypeOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, categoryType, pageable);
        } else if (campaignType != null && !campaignType.isEmpty()) {
            // 캠페인 타입별 조회 (신청 많은 순)
            return campaignRepository.findByApprovalStatusAndCampaignTypeOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, campaignType, pageable);
        } else if (onlyActive) {
            // 마감되지 않은 캠페인만 조회 (신청 많은 순)
            return campaignRepository.findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqualOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, LocalDate.now(), pageable);
        } else {
            // 모든 승인된 캠페인 조회 (신청 많은 순)
            return campaignRepository.findByApprovalStatusOrderByCurrentApplicantsDesc(APPROVED_STATUS, pageable);
        }
    }
    
    /**
     * 기본 정렬 방식으로 캠페인 페이지 조회
     */
    private Page<Campaign> getCampaignPageWithStandardSort(String categoryType, String campaignType, 
                                                          boolean onlyActive, Pageable pageable) {
        if (categoryType != null && !categoryType.isEmpty()) {
            // 카테고리별 조회
            return campaignRepository.findByApprovalStatusAndCategoryCategoryType(
                    APPROVED_STATUS, categoryType, pageable);
        } else if (campaignType != null && !campaignType.isEmpty()) {
            // 캠페인 타입별 조회
            return campaignRepository.findByApprovalStatusAndCampaignType(
                    APPROVED_STATUS, campaignType, pageable);
        } else if (onlyActive) {
            // 마감되지 않은 캠페인만 조회
            return campaignRepository.findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, LocalDate.now(), pageable);
        } else {
            // 모든 승인된 캠페인 조회
            return campaignRepository.findByApprovalStatus(APPROVED_STATUS, pageable);
        }
    }

    // ===== 캠페인 상세 조회 메서드들 =====

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