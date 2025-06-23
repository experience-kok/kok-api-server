package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignCategory;
import com.example.auth.dto.campaign.CampaignListSimpleResponse;
import com.example.auth.dto.campaign.*;
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
    private static final Campaign.ApprovalStatus APPROVED_STATUS = Campaign.ApprovalStatus.APPROVED;

    /**
     * 현재 날짜를 반환하는 헬퍼 메서드
     */
    private LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    /**
     * String categoryType을 CategoryType enum으로 변환
     */
    private CampaignCategory.CategoryType convertCategoryType(String categoryType) {
        if (categoryType == null || categoryType.isEmpty()) {
            return null;
        }
        try {
            return CampaignCategory.CategoryType.valueOf(categoryType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid categoryType: {}", categoryType);
            return null;
        }
    }

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

        // 신청 인원수를 실제 데이터로 설정
        List<CampaignListSimpleResponse> campaigns = responsePage.getContent();
        if (!campaigns.isEmpty()) {
            campaigns.forEach(campaignResponse -> {
                // 실제 캠페인에서 신청자 수를 가져와 설정
                Campaign actualCampaign = campaignPage.getContent().stream()
                        .filter(c -> c.getId().equals(campaignResponse.getId()))
                        .findFirst()
                        .orElse(null);
                if (actualCampaign != null) {
                    campaignResponse.setCurrentApplicants(actualCampaign.getCurrentApplicantCount());
                }
            });
        }

        return PageResponse.from(responsePage);
    }

    /**
     * 마감 임박순 캠페인 목록 조회 (페이징 처리) - 승인된 활성 캠페인만
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignListByDeadlineSoon(int page, int size,
                                                                                  String categoryType, String campaignType) {
        // 마감일 오름차순 정렬 (마감 가까운 순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "applicationDeadlineDate"));
        LocalDate currentDate = getCurrentDate();

        Page<Campaign> campaignPage;

        if (categoryType != null && !categoryType.isEmpty()) {
            CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);
            if (categoryTypeEnum != null) {
                campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndApplicationDeadlineDateGreaterThanEqual(
                        APPROVED_STATUS, categoryTypeEnum, currentDate, pageable);
            } else {
                campaignPage = campaignRepository.findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqual(
                        APPROVED_STATUS, currentDate, pageable);
            }
        } else if (campaignType != null && !campaignType.isEmpty()) {
            campaignPage = campaignRepository.findByApprovalStatusAndCampaignTypeAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, campaignType, currentDate, pageable);
        } else {
            campaignPage = campaignRepository.findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, currentDate, pageable);
        }

        // 엔티티를 간소화된 DTO로 변환
        Page<CampaignListSimpleResponse> responsePage = campaignPage.map(campaign -> {
            CampaignListSimpleResponse response = CampaignListSimpleResponse.fromEntity(campaign);
            return response;
        });

        // 신청 인원수를 실제 데이터로 설정
        List<CampaignListSimpleResponse> campaigns = responsePage.getContent();
        if (!campaigns.isEmpty()) {
            campaigns.forEach(campaignResponse -> {
                // 실제 캠페인에서 신청자 수를 가져와 설정
                Campaign actualCampaign = campaignPage.getContent().stream()
                        .filter(c -> c.getId().equals(campaignResponse.getId()))
                        .findFirst()
                        .orElse(null);
                if (actualCampaign != null) {
                    campaignResponse.setCurrentApplicants(actualCampaign.getCurrentApplicantCount());
                }
            });
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

        // 신청 인원수를 실제 데이터로 설정
        List<CampaignListSimpleResponse> campaigns = responsePage.getContent();
        if (!campaigns.isEmpty()) {
            campaigns.forEach(campaignResponse -> {
                Campaign actualCampaign = campaignPage.getContent().stream()
                        .filter(c -> c.getId().equals(campaignResponse.getId()))
                        .findFirst()
                        .orElse(null);
                if (actualCampaign != null) {
                    campaignResponse.setCurrentApplicants(actualCampaign.getCurrentApplicantCount());
                }
            });
        }

        return PageResponse.from(responsePage);
    }

    /**
     * 카테고리명 포함하여 캠페인 목록 조회 (페이징 처리) - 간소화된 응답
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignListWithFilters(int page, int size, String sort, boolean onlyActive,
                                                                               String categoryType, String categoryName, String campaignType) {
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
            campaignPage = getCampaignPageSortedByCurrentApplicantsWithFilters(categoryType, categoryName, campaignType, onlyActive, pageable);
        } else {
            // 기존 정렬 방식
            campaignPage = getCampaignPageWithStandardSortWithFilters(categoryType, categoryName, campaignType, onlyActive, pageable);
        }

        // 엔티티를 간소화된 DTO로 변환
        Page<CampaignListSimpleResponse> responsePage = campaignPage.map(campaign -> {
            CampaignListSimpleResponse response = CampaignListSimpleResponse.fromEntity(campaign);
            return response;
        });

        // 신청 인원수를 실제 데이터로 설정
        List<CampaignListSimpleResponse> campaigns = responsePage.getContent();
        if (!campaigns.isEmpty()) {
            campaigns.forEach(campaignResponse -> {
                // 실제 캠페인에서 신청자 수를 가져와 설정
                Campaign actualCampaign = campaignPage.getContent().stream()
                        .filter(c -> c.getId().equals(campaignResponse.getId()))
                        .findFirst()
                        .orElse(null);
                if (actualCampaign != null) {
                    campaignResponse.setCurrentApplicants(actualCampaign.getCurrentApplicantCount());
                }
            });
        }

        return PageResponse.from(responsePage);
    }

    /**
     * 카테고리명 포함하여 마감 임박순 캠페인 목록 조회 (페이징 처리) - 승인된 활성 캠페인만
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignListByDeadlineSoonWithFilters(int page, int size,
                                                                                             String categoryType, String categoryName, String campaignType) {
        // 마감일 오름차순 정렬 (마감 가까운 순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "applicationDeadlineDate"));
        LocalDate currentDate = getCurrentDate();

        Page<Campaign> campaignPage;
        CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);

        // 카테고리 타입과 카테고리명 모두 있는 경우
        if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty()) {
            campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, categoryTypeEnum, categoryName, currentDate, pageable);
        }
        // 카테고리 타입만 있는 경우
        else if (categoryTypeEnum != null) {
            campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, categoryTypeEnum, currentDate, pageable);
        }
        // 캠페인 타입만 있는 경우
        else if (campaignType != null && !campaignType.isEmpty()) {
            campaignPage = campaignRepository.findByApprovalStatusAndCampaignTypeAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, campaignType, currentDate, pageable);
        }
        // 필터 없이 모든 승인된 활성 캠페인
        else {
            campaignPage = campaignRepository.findByApprovalStatusAndApplicationDeadlineDateGreaterThanEqual(
                    APPROVED_STATUS, currentDate, pageable);
        }

        // 엔티티를 간소화된 DTO로 변환
        Page<CampaignListSimpleResponse> responsePage = campaignPage.map(campaign -> {
            CampaignListSimpleResponse response = CampaignListSimpleResponse.fromEntity(campaign);
            return response;
        });

        // 신청 인원수를 실제 데이터로 설정
        List<CampaignListSimpleResponse> campaigns = responsePage.getContent();
        if (!campaigns.isEmpty()) {
            campaigns.forEach(campaignResponse -> {
                // 실제 캠페인에서 신청자 수를 가져와 설정
                Campaign actualCampaign = campaignPage.getContent().stream()
                        .filter(c -> c.getId().equals(campaignResponse.getId()))
                        .findFirst()
                        .orElse(null);
                if (actualCampaign != null) {
                    campaignResponse.setCurrentApplicants(actualCampaign.getCurrentApplicantCount());
                }
            });
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
     * 인기순 정렬을 위한 승인된 활성 캠페인 조회
     */
    private Page<Campaign> getFilteredCampaignPageByPopularity(String categoryType, String categoryName,
                                                               List<String> campaignTypes, Pageable pageable) {
        CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);
        LocalDate currentDate = getCurrentDate();

        if (categoryTypeEnum == null) {
            // categoryType이 유효하지 않은 경우 모든 승인된 활성 캠페인을 인기순으로 반환
            return campaignRepository.findApprovedActiveOrderByCurrentApplicantsDesc(APPROVED_STATUS, currentDate, pageable);
        }

        if (categoryName != null && campaignTypes != null && !campaignTypes.isEmpty()) {
            // 카테고리명과 플랫폼 타입 모두 필터링 + 인기순
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameAndCampaignTypesOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, campaignTypes, pageable);
        } else if (categoryName != null) {
            // 카테고리명만 필터링 + 인기순
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, pageable);
        } else if (campaignTypes != null && !campaignTypes.isEmpty()) {
            // 플랫폼 타입만 필터링 + 인기순
            return campaignRepository.findApprovedActiveByCategoryTypeAndCampaignTypesOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, campaignTypes, pageable);
        } else {
            // 카테고리 타입만 필터링 + 인기순
            return campaignRepository.findApprovedActiveByCategoryTypeOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, pageable);
        }
    }

    /**
     * 일반 정렬을 위한 승인된 활성 캠페인 조회
     */
    private Page<Campaign> getFilteredCampaignPageByStandardSort(String categoryType, String categoryName,
                                                                 List<String> campaignTypes, Pageable pageable) {
        CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);
        LocalDate currentDate = getCurrentDate();

        if (categoryTypeEnum == null) {
            // categoryType이 유효하지 않은 경우 모든 승인된 활성 캠페인 반환
            return campaignRepository.findApprovedActiveOrderByLatest(APPROVED_STATUS, currentDate, pageable);
        }

        if (categoryName != null && campaignTypes != null && !campaignTypes.isEmpty()) {
            // 카테고리명과 플랫폼 타입 모두 필터링
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameAndCampaignTypesOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, campaignTypes, pageable);
        } else if (categoryName != null) {
            // 카테고리명만 필터링
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, pageable);
        } else if (campaignTypes != null && !campaignTypes.isEmpty()) {
            // 플랫폼 타입만 필터링
            return campaignRepository.findApprovedActiveByCategoryTypeAndCampaignTypesOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, campaignTypes, pageable);
        } else {
            // 카테고리 타입만 필터링
            return campaignRepository.findApprovedActiveByCategoryTypeOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, pageable);
        }
    }

    /**
     * 승인된 활성 캠페인을 인기순으로 정렬
     */
    private Page<Campaign> getCampaignPageSortedByCurrentApplicants(String categoryType, String campaignType,
                                                                    boolean onlyActive, Pageable pageable) {
        LocalDate currentDate = getCurrentDate();

        if (categoryType != null && !categoryType.isEmpty()) {
            CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);
            if (categoryTypeEnum != null) {
                return campaignRepository.findApprovedActiveByCategoryTypeOrderByCurrentApplicantsDesc(
                        APPROVED_STATUS, currentDate, categoryTypeEnum, pageable);
            }
        } else if (campaignType != null && !campaignType.isEmpty()) {
            return campaignRepository.findApprovedActiveByCampaignTypeOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, campaignType, pageable);
        }
        // 모든 승인된 활성 캠페인 조회 (인기순)
        return campaignRepository.findApprovedActiveOrderByCurrentApplicantsDesc(APPROVED_STATUS, currentDate, pageable);
    }

    /**
     * 승인된 활성 캠페인을 최신순으로 정렬
     */
    private Page<Campaign> getCampaignPageWithStandardSort(String categoryType, String campaignType,
                                                           boolean onlyActive, Pageable pageable) {
        LocalDate currentDate = getCurrentDate();

        if (categoryType != null && !categoryType.isEmpty()) {
            CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);
            if (categoryTypeEnum != null) {
                return campaignRepository.findApprovedActiveByCategoryTypeOrderByLatest(
                        APPROVED_STATUS, currentDate, categoryTypeEnum, pageable);
            }
        } else if (campaignType != null && !campaignType.isEmpty()) {
            return campaignRepository.findApprovedActiveByCampaignTypeOrderByLatest(
                    APPROVED_STATUS, currentDate, campaignType, pageable);
        }
        // 모든 승인된 활성 캠페인 조회 (최신순)
        return campaignRepository.findApprovedActiveOrderByLatest(APPROVED_STATUS, currentDate, pageable);
    }

    /**
     * 카테고리명 포함하여 승인된 활성 캠페인을 인기순으로 정렬
     */
    private Page<Campaign> getCampaignPageSortedByCurrentApplicantsWithFilters(String categoryType, String categoryName, String campaignType,
                                                                               boolean onlyActive, Pageable pageable) {
        LocalDate currentDate = getCurrentDate();
        CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);

        // 카테고리 타입과 카테고리명 모두 있는 경우
        if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty()) {
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, pageable);
        }
        // 카테고리 타입만 있는 경우
        else if (categoryTypeEnum != null) {
            return campaignRepository.findApprovedActiveByCategoryTypeOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, pageable);
        }
        // 캠페인 타입만 있는 경우
        else if (campaignType != null && !campaignType.isEmpty()) {
            return campaignRepository.findApprovedActiveByCampaignTypeOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, campaignType, pageable);
        }
        // 필터 없이 모든 승인된 활성 캠페인
        else {
            return campaignRepository.findApprovedActiveOrderByCurrentApplicantsDesc(APPROVED_STATUS, currentDate, pageable);
        }
    }

    /**
     * 카테고리명 포함하여 승인된 활성 캠페인을 최신순으로 정렬
     */
    private Page<Campaign> getCampaignPageWithStandardSortWithFilters(String categoryType, String categoryName, String campaignType,
                                                                      boolean onlyActive, Pageable pageable) {
        LocalDate currentDate = getCurrentDate();
        CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);

        // 카테고리 타입과 카테고리명 모두 있는 경우
        if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty()) {
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, pageable);
        }
        // 카테고리 타입만 있는 경우
        else if (categoryTypeEnum != null) {
            return campaignRepository.findApprovedActiveByCategoryTypeOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, pageable);
        }
        // 캠페인 타입만 있는 경우
        else if (campaignType != null && !campaignType.isEmpty()) {
            return campaignRepository.findApprovedActiveByCampaignTypeOrderByLatest(
                    APPROVED_STATUS, currentDate, campaignType, pageable);
        }
        // 필터 없이 모든 승인된 활성 캠페인
        else {
            return campaignRepository.findApprovedActiveOrderByLatest(APPROVED_STATUS, currentDate, pageable);
        }
    }

    // ===== 캠페인 상세 조회 메서드들 =====

    /**
     * 캠페인 썸네일 조회 (모든 캠페인)
     */
    @Transactional(readOnly = true)
    public CampaignThumbnailResponse getCampaignThumbnail(Long campaignId) {
        Campaign campaign = findCampaignById(campaignId);
        return CampaignThumbnailResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 기본 정보 조회 (모든 캠페인)
     */
    @Transactional(readOnly = true)
    public CampaignBasicInfoResponse getCampaignBasicInfo(Long campaignId) {
        Campaign campaign = findCampaignById(campaignId);
        return CampaignBasicInfoResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 상세 정보 조회 (모든 캠페인)
     */
    @Transactional(readOnly = true)
    public CampaignDetailInfoResponse getCampaignDetailInfo(Long campaignId) {
        Campaign campaign = findCampaignById(campaignId);
        return CampaignDetailInfoResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 미션 가이드 조회 (모든 캠페인)
     */
    @Transactional(readOnly = true)
    public CampaignMissionGuideResponse getCampaignMissionGuide(Long campaignId) {
        Campaign campaign = findCampaignById(campaignId);
        return CampaignMissionGuideResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 미션 키워드 조회 (모든 캠페인)
     */
    @Transactional(readOnly = true)
    public CampaignKeywordsResponse getCampaignKeywords(Long campaignId) {
        Campaign campaign = findCampaignById(campaignId);
        return CampaignKeywordsResponse.fromEntity(campaign);
    }

    /**
     * ID로 캠페인 조회 (승인 상태 무관)
     */
    private Campaign findCampaignById(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다."));
    }

    /**
     * ID로 승인된 캠페인 조회 (기존 메서드 유지)
     */
    private Campaign findApprovedCampaignById(Long campaignId) {
        return campaignRepository.findByIdAndApprovalStatus(campaignId, APPROVED_STATUS)
                .orElseThrow(() -> new ResourceNotFoundException("승인된 캠페인을 찾을 수 없습니다."));
    }

    /**
     * 디버그용: 모든 캠페인 조회 (승인 상태 무관)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllCampaignsForDebug() {
        var campaigns = campaignRepository.findAll();
        return campaigns.stream()
                .map(campaign -> {
                    Map<String, Object> campaignInfo = new java.util.HashMap<>();
                    campaignInfo.put("id", campaign.getId());
                    campaignInfo.put("title", campaign.getTitle());
                    campaignInfo.put("approvalStatus", campaign.getApprovalStatus().name());
                    campaignInfo.put("applicationDeadlineDate", campaign.getApplicationDeadlineDate());
                    campaignInfo.put("categoryType", campaign.getCategory() != null ?
                            campaign.getCategory().getCategoryType().name() : null);
                    campaignInfo.put("categoryName", campaign.getCategory() != null ?
                            campaign.getCategory().getCategoryName() : null);
                    campaignInfo.put("createdAt", campaign.getCreatedAt());
                    return campaignInfo;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 키워드로 캠페인 검색 (승인된 활성 캠페인만, 최신순 고정)
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> searchCampaigns(
            String keyword, int page, int size, String sort) {

        log.info("캠페인 검색 실행 - keyword: {}, page: {}, size: {}", keyword, page, size);

        // 승인된 활성 캠페인만 최신순으로 고정
        Pageable pageable = PageRequest.of(page, size);
        LocalDate currentDate = getCurrentDate();

        log.info("승인된 활성 캠페인만 최신순 정렬로 검색 실행");
        Page<Campaign> campaignPage = campaignRepository.searchApprovedActiveByKeywordOrderByLatest(
                APPROVED_STATUS, currentDate, keyword, null, null, null, pageable);

        log.info("검색 결과 - 총 {}개 캠페인 발견, 현재 페이지 {}개",
                campaignPage.getTotalElements(), campaignPage.getNumberOfElements());

        // DTO 변환
        Page<CampaignListSimpleResponse> responsePage = campaignPage.map(CampaignListSimpleResponse::fromEntity);

        // 신청 인원수를 실제 데이터로 설정
        List<CampaignListSimpleResponse> campaigns = responsePage.getContent();
        if (!campaigns.isEmpty()) {
            campaigns.forEach(campaignResponse -> {
                Campaign actualCampaign = campaignPage.getContent().stream()
                        .filter(c -> c.getId().equals(campaignResponse.getId()))
                        .findFirst()
                        .orElse(null);
                if (actualCampaign != null) {
                    campaignResponse.setCurrentApplicants(actualCampaign.getCurrentApplicantCount());
                }
            });
        }

        log.info("최종 응답 준비 완료 - {}개 캠페인", campaigns.size());
        return PageResponse.from(responsePage);
    }
}