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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

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
     * 네이티브 쿼리로 최고 성능 캠페인 목록 조회 - N+1 문제 완전 해결
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getOptimizedCampaignList(
            int page, int size, String sort, String categoryType) {
        
        log.info("최적화된 캠페인 목록 조회 - page: {}, size: {}, sort: {}, categoryType: {}", 
                page, size, sort, categoryType);
        
        LocalDate currentDate = getCurrentDate();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Object[]> resultPage;
        
        if ("popular".equals(sort)) {
            // 인기순 - 네이티브 쿼리로 한 번에 조회
            resultPage = campaignRepository.findOptimizedCampaignListByPopularity(
                    Campaign.ApprovalStatus.APPROVED.name(), currentDate, categoryType, null, null, pageable);
        } else {
            // 최신순 - 네이티브 쿼리로 한 번에 조회
            resultPage = campaignRepository.findOptimizedCampaignListByLatest(
                    Campaign.ApprovalStatus.APPROVED.name(), currentDate, categoryType, null, null, pageable);
        }
        
        // DTO 변환 (모든 데이터가 이미 조회됨)
        Page<CampaignListSimpleResponse> responsePage = resultPage.map(this::mapOptimizedResultToResponse);
        
        log.info("최적화된 조회 완료 - {}개 캠페인, 쿼리 1회만 실행", responsePage.getNumberOfElements());
        
        return PageResponse.from(responsePage);
    }

    /**
     * 최적화된 네이티브 쿼리 결과를 DTO로 변환 (인기순/최신순 공통)
     */
    private CampaignListSimpleResponse mapOptimizedResultToResponse(Object[] result) {
        return CampaignListSimpleResponse.builder()
                .id((Long) result[0])                                    // c.id
                .title((String) result[1])                               // c.title
                .thumbnailUrl((String) result[2])                        // c.thumbnail_url
                .recruitmentEndDate(((java.sql.Date) result[3]).toLocalDate()) // c.recruitment_end_date
                .maxApplicants((Integer) result[4])                      // c.max_applicants
                .campaignType((String) result[5])                        // c.campaign_type
                .productShortInfo((String) result[6])                    // c.product_short_info
                // result[7]은 c.created_at (사용하지 않음)
                .category(CampaignListSimpleResponse.CategoryInfo.builder()
                        .name((String) result[8])                        // cc.category_name
                        .type((String) result[9])                        // cc.category_type
                        .build())
                .currentApplicants(((Number) result[10]).intValue())     // current_applicants (PENDING 신청 수)
                .build();
    }

    /**
     * 네이티브 쿼리로 최고 성능 캠페인 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getNativeCampaignList(
            int page, int size, String sort, String categoryType) {
        
        log.info("네이티브 쿼리 캠페인 목록 조회 - page: {}, size: {}, sort: {}", page, size, sort);
        
        LocalDate currentDate = getCurrentDate();
        Pageable pageable = PageRequest.of(page, size);
        
        // 네이티브 쿼리로 모든 데이터를 한 번에 조회
        Page<Object[]> resultPage = campaignRepository.findOptimizedCampaignListNative(
                APPROVED_STATUS.name(),
                currentDate,
                categoryType,
                sort,
                pageable
        );
        
        // DTO 변환
        Page<CampaignListSimpleResponse> responsePage = resultPage.map(this::mapNativeResultToResponse);
        
        log.info("네이티브 쿼리 조회 완료 - {}개 캠페인", responsePage.getNumberOfElements());
        
        return PageResponse.from(responsePage);
    }

    /**
     * 네이티브 쿼리 결과를 DTO로 변환
     */
    private CampaignListSimpleResponse mapNativeResultToResponse(Object[] result) {
        return CampaignListSimpleResponse.builder()
                .id((Long) result[0])
                .title((String) result[1])
                .thumbnailUrl((String) result[2])
                .recruitmentEndDate(((Date) result[3]).toLocalDate())
                .maxApplicants((Integer) result[4])
                // createdAt 필드는 CampaignListSimpleResponse에 없으므로 제거
                .category(CampaignListSimpleResponse.CategoryInfo.builder()
                        .name((String) result[6])
                        .type((String) result[7])
                        .build())
                .currentApplicants(((Number) result[8]).intValue())
                .build();
    }
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
     * 마감 임박순 캠페인 목록 조회 (페이징 처리) - 승인된 활성 캠페인만, 상시 캠페인 제외
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignListByDeadlineSoon(int page, int size,
                                                                                  String categoryType, String campaignType) {
        // 마감일 오름차순 정렬 (마감 가까운 순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "recruitmentEndDate"));
        LocalDate currentDate = getCurrentDate();

        Page<Campaign> campaignPage;

        if (categoryType != null && !categoryType.isEmpty()) {
            CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);
            if (categoryTypeEnum != null) {
                // 상시 캠페인 제외 - 마감일이 있는 캠페인만 조회
                campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
                        APPROVED_STATUS, categoryTypeEnum, currentDate, pageable);
            } else {
                campaignPage = campaignRepository.findByApprovalStatusAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
                        APPROVED_STATUS, currentDate, pageable);
            }
        } else if (campaignType != null && !campaignType.isEmpty()) {
            campaignPage = campaignRepository.findByApprovalStatusAndCampaignTypeAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
                    APPROVED_STATUS, campaignType, currentDate, pageable);
        } else {
            // 상시 캠페인 제외 - 마감일이 있는 캠페인만 조회
            campaignPage = campaignRepository.findByApprovalStatusAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
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

        log.info("최적화된 캠페인 목록 조회 시작 - page: {}, size: {}, categoryType: {}, categoryName: {}, sort: {}",
                page, size, categoryType, categoryName, sort);

        // 정렬 기준 변환
        String actualSort = convertSortParameter(sort);
        boolean sortByCurrentApplicants = "currentApplicants".equals(actualSort);

        // 최적화된 메서드 사용
        if (sortByCurrentApplicants) {
            log.info("최적화된 인기순 조회 사용");
            return getOptimizedCampaignListByPopularityWithTypes(page, size, categoryType, categoryName, null);
        } else {
            log.info("최적화된 최신순 조회 사용");
            return getOptimizedCampaignListByLatestWithTypes(page, size, categoryType, categoryName, null);
        }
    }

    /**
     * 카테고리명 포함하여 캠페인 목록 조회 (페이징 처리) - 기존 방식으로 임시 복구
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignListWithFilters(int page, int size, String sort, boolean onlyActive,
                                                                               String categoryType, String categoryName, String campaignType) {
        log.info("기존 방식 캠페인 목록 조회 - page: {}, size: {}, sort: {}, categoryType: {}, categoryName: {}",
                page, size, sort, categoryType, categoryName);
        
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

        log.info("기존 방식 조회 완료 - {}개 캠페인", campaigns.size());
        return PageResponse.from(responsePage);
    }

    /**
     * 플랫폼 타입 리스트를 포함한 모든 필터 조건으로 캠페인 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignListWithAllFilters(int page, int size, String sort, boolean onlyActive,
                                                                                  String categoryType, String categoryName, List<String> campaignTypes) {
        log.info("모든 필터 조건 캠페인 목록 조회 - page: {}, size: {}, sort: {}, categoryType: {}, categoryName: {}, campaignTypes: {}",
                page, size, sort, categoryType, categoryName, campaignTypes);
        
        // 최적화된 네이티브 쿼리 사용 - 복수 campaignTypes 지원
        if ("currentApplicants".equals(sort)) {
            log.info("인기순 정렬 - 복수 campaignTypes 지원 네이티브 쿼리 사용");
            return getOptimizedCampaignListByPopularityWithTypes(page, size, categoryType, categoryName, campaignTypes);
        } else {
            log.info("최신순 정렬 - 복수 campaignTypes 지원 네이티브 쿼리 사용");
            return getOptimizedCampaignListByLatestWithTypes(page, size, categoryType, categoryName, campaignTypes);
        }
    }

    /**
     * 최적화된 네이티브 쿼리로 인기순 캠페인 목록 조회 (복수 campaignTypes 지원)
     */
    @Transactional(readOnly = true)
    private PageResponse<CampaignListSimpleResponse> getOptimizedCampaignListByPopularityWithTypes(int page, int size, String categoryType, String categoryName, List<String> campaignTypes) {
        LocalDate currentDate = getCurrentDate();
        Pageable pageable = PageRequest.of(page, size);
        
        log.info("네이티브 인기순 쿼리 실행 (복수 타입) - categoryType: {}, categoryName: {}, campaignTypes: {}", categoryType, categoryName, campaignTypes);
        
        // campaignTypes를 PostgreSQL 배열 형태로 변환
        Integer campaignTypesSize = (campaignTypes != null) ? campaignTypes.size() : 0;
        String campaignTypesArray = null;
        
        if (campaignTypes != null && !campaignTypes.isEmpty()) {
            campaignTypesArray = "{" + String.join(",", campaignTypes) + "}";
        }
        
        Page<Object[]> resultPage = campaignRepository.findOptimizedCampaignListByPopularityWithTypes(
                Campaign.ApprovalStatus.APPROVED.name(), currentDate, categoryType, categoryName, 
                campaignTypesSize, campaignTypesArray, pageable);
        
        // DTO 변환
        Page<CampaignListSimpleResponse> responsePage = resultPage.map(this::mapOptimizedResultToResponse);
        
        log.info("인기순 조회 완료 - {}개 캠페인", responsePage.getNumberOfElements());
        
        return PageResponse.from(responsePage);
    }

    /**
     * 최적화된 네이티브 쿼리로 최신순 캠페인 목록 조회 (복수 campaignTypes 지원)
     */
    @Transactional(readOnly = true)
    private PageResponse<CampaignListSimpleResponse> getOptimizedCampaignListByLatestWithTypes(int page, int size, String categoryType, String categoryName, List<String> campaignTypes) {
        LocalDate currentDate = getCurrentDate();
        Pageable pageable = PageRequest.of(page, size);
        
        log.info("네이티브 최신순 쿼리 실행 (복수 타입) - categoryType: {}, categoryName: {}, campaignTypes: {}", categoryType, categoryName, campaignTypes);
        
        // campaignTypes를 PostgreSQL 배열 형태로 변환
        Integer campaignTypesSize = (campaignTypes != null) ? campaignTypes.size() : 0;
        String campaignTypesArray = null;
        
        if (campaignTypes != null && !campaignTypes.isEmpty()) {
            campaignTypesArray = "{" + String.join(",", campaignTypes) + "}";
        }
        
        Page<Object[]> resultPage = campaignRepository.findOptimizedCampaignListByLatestWithTypes(
                Campaign.ApprovalStatus.APPROVED.name(), currentDate, categoryType, categoryName, 
                campaignTypesSize, campaignTypesArray, pageable);
        
        // DTO 변환
        Page<CampaignListSimpleResponse> responsePage = resultPage.map(this::mapOptimizedResultToResponse);
        
        log.info("최신순 조회 완료 - {}개 캠페인", responsePage.getNumberOfElements());
        
        return PageResponse.from(responsePage);
    }

    /**
     * 플랫폼 타입 리스트를 포함하여 마감 임박순 캠페인 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignListByDeadlineSoonWithCampaignTypes(int page, int size,
                                                                                                   String categoryType, String categoryName, List<String> campaignTypes) {
        // 마감일 오름차순 정렬 (마감 가까운 순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "recruitmentEndDate"));
        LocalDate currentDate = getCurrentDate();

        Page<Campaign> campaignPage;
        CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);

        log.info("마감 임박순 조회 - categoryType: {}, categoryName: {}, campaignTypes: {}", categoryType, categoryName, campaignTypes);

        // 모든 필터 조건을 고려한 조회
        if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty() && campaignTypes != null && !campaignTypes.isEmpty()) {
            // 카테고리 타입, 카테고리명, 캠페인 타입들 모두 있는 경우
            campaignPage = campaignRepository.findApprovedActiveByCategoryTypeAndNameAndCampaignTypesOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, campaignTypes, pageable);
        } else if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty()) {
            // 카테고리 타입과 카테고리명만 있는 경우
            campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndRecruitmentEndDateGreaterThanEqual(
                    APPROVED_STATUS, categoryTypeEnum, categoryName, currentDate, pageable);
        } else if (categoryTypeEnum != null && campaignTypes != null && !campaignTypes.isEmpty()) {
            // 카테고리 타입과 캠페인 타입들만 있는 경우
            campaignPage = campaignRepository.findApprovedActiveByCategoryTypeAndCampaignTypesOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, campaignTypes, pageable);
        } else if (categoryTypeEnum != null) {
            // 카테고리 타입만 있는 경우
            campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndRecruitmentEndDateGreaterThanEqual(
                    APPROVED_STATUS, categoryTypeEnum, currentDate, pageable);
        } else if (campaignTypes != null && !campaignTypes.isEmpty()) {
            // 캠페인 타입들만 있는 경우 - 복수 캠페인 타입 처리
            if (campaignTypes.size() == 1) {
                // 단일 캠페인 타입
                String firstCampaignType = campaignTypes.get(0);
                campaignPage = campaignRepository.findByApprovalStatusAndCampaignTypeAndRecruitmentEndDateGreaterThanEqual(
                        APPROVED_STATUS, firstCampaignType, currentDate, pageable);
            } else {
                // 복수 캠페인 타입 - 전체 조회 후 필터링 (임시 방법)
                campaignPage = campaignRepository.findByApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
                        APPROVED_STATUS, currentDate, pageable);
                // TODO: 복수 campaignType 지원하는 전용 쿼리 추가 필요
            }
        } else {
            // 필터 없이 모든 승인된 활성 캠페인
            campaignPage = campaignRepository.findByApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
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

        // campaignTypes 필터링이 복수인 경우 후처리 필터링
        if (campaignTypes != null && campaignTypes.size() > 1) {
            campaigns = campaigns.stream()
                    .filter(campaign -> campaignTypes.contains(campaign.getCampaignType()))
                    .collect(Collectors.toList());
            log.info("복수 campaignTypes 후처리 필터링 완료 - {}개 캠페인", campaigns.size());
        }

        return PageResponse.from(responsePage);
    }
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> getCampaignListByDeadlineSoonWithFilters(int page, int size,
                                                                                             String categoryType, String categoryName, String campaignType) {
        // 마감일 오름차순 정렬 (마감 가까운 순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "recruitmentEndDate"));
        LocalDate currentDate = getCurrentDate();

        Page<Campaign> campaignPage;
        CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);

        // 카테고리 타입과 카테고리명 모두 있는 경우 - 상시 캠페인 제외
        if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty()) {
            campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndCategoryCategoryNameAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
                    APPROVED_STATUS, categoryTypeEnum, categoryName, currentDate, pageable);
        }
        // 카테고리 타입만 있는 경우 - 상시 캠페인 제외
        else if (categoryTypeEnum != null) {
            campaignPage = campaignRepository.findByApprovalStatusAndCategoryCategoryTypeAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
                    APPROVED_STATUS, categoryTypeEnum, currentDate, pageable);
        }
        // 캠페인 타입만 있는 경우 - 상시 캠페인 제외
        else if (campaignType != null && !campaignType.isEmpty()) {
            campaignPage = campaignRepository.findByApprovalStatusAndCampaignTypeAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
                    APPROVED_STATUS, campaignType, currentDate, pageable);
        }
        // 필터 없이 모든 승인된 활성 캠페인 - 상시 캠페인 제외
        else {
            campaignPage = campaignRepository.findByApprovalStatusAndIsAlwaysOpenFalseAndRecruitmentEndDateGreaterThanEqual(
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
            case "deadline" -> "recruitmentEndDate";
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
     * 카테고리명과 플랫폼 타입 리스트 포함하여 승인된 활성 캠페인을 인기순으로 정렬
     */
    private Page<Campaign> getCampaignPageSortedByCurrentApplicantsWithAllFilters(String categoryType, String categoryName, List<String> campaignTypes,
                                                                                  boolean onlyActive, Pageable pageable) {
        LocalDate currentDate = getCurrentDate();
        CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);

        // 모든 필터 조건을 고려한 인기순 정렬
        if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty() && campaignTypes != null && !campaignTypes.isEmpty()) {
            // 카테고리 타입, 카테고리명, 캠페인 타입들 모두 있는 경우
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameAndCampaignTypesOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, campaignTypes, pageable);
        } else if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty()) {
            // 카테고리 타입과 카테고리명만 있는 경우
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, pageable);
        } else if (categoryTypeEnum != null && campaignTypes != null && !campaignTypes.isEmpty()) {
            // 카테고리 타입과 캠페인 타입들만 있는 경우
            return campaignRepository.findApprovedActiveByCategoryTypeAndCampaignTypesOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, campaignTypes, pageable);
        } else if (categoryTypeEnum != null) {
            // 카테고리 타입만 있는 경우
            return campaignRepository.findApprovedActiveByCategoryTypeOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, pageable);
        } else if (campaignTypes != null && !campaignTypes.isEmpty()) {
            // 캠페인 타입들만 있는 경우 - 단일 캠페인 타입으로 처리 (첫 번째 타입만 사용)
            String firstCampaignType = campaignTypes.get(0);
            return campaignRepository.findApprovedActiveByCampaignTypeOrderByCurrentApplicantsDesc(
                    APPROVED_STATUS, currentDate, firstCampaignType, pageable);
        } else {
            // 필터 없이 모든 승인된 활성 캠페인
            return campaignRepository.findApprovedActiveOrderByCurrentApplicantsDesc(APPROVED_STATUS, currentDate, pageable);
        }
    }

    /**
     * 카테고리명과 플랫폼 타입 리스트 포함하여 승인된 활성 캠페인을 최신순으로 정렬
     */
    private Page<Campaign> getCampaignPageWithStandardSortWithAllFilters(String categoryType, String categoryName, List<String> campaignTypes,
                                                                         boolean onlyActive, Pageable pageable) {
        LocalDate currentDate = getCurrentDate();
        CampaignCategory.CategoryType categoryTypeEnum = convertCategoryType(categoryType);

        // 모든 필터 조건을 고려한 최신순 정렬
        if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty() && campaignTypes != null && !campaignTypes.isEmpty()) {
            // 카테고리 타입, 카테고리명, 캠페인 타입들 모두 있는 경우
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameAndCampaignTypesOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, campaignTypes, pageable);
        } else if (categoryTypeEnum != null && categoryName != null && !categoryName.isEmpty()) {
            // 카테고리 타입과 카테고리명만 있는 경우
            return campaignRepository.findApprovedActiveByCategoryTypeAndNameOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, categoryName, pageable);
        } else if (categoryTypeEnum != null && campaignTypes != null && !campaignTypes.isEmpty()) {
            // 카테고리 타입과 캠페인 타입들만 있는 경우
            return campaignRepository.findApprovedActiveByCategoryTypeAndCampaignTypesOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, campaignTypes, pageable);
        } else if (categoryTypeEnum != null) {
            // 카테고리 타입만 있는 경우
            return campaignRepository.findApprovedActiveByCategoryTypeOrderByLatest(
                    APPROVED_STATUS, currentDate, categoryTypeEnum, pageable);
        } else if (campaignTypes != null && !campaignTypes.isEmpty()) {
            // 캠페인 타입들만 있는 경우 - 단일 캠페인 타입으로 처리 (첫 번째 타입만 사용)
            String firstCampaignType = campaignTypes.get(0);
            return campaignRepository.findApprovedActiveByCampaignTypeOrderByLatest(
                    APPROVED_STATUS, currentDate, firstCampaignType, pageable);
        } else {
            // 필터 없이 모든 승인된 활성 캠페인
            return campaignRepository.findApprovedActiveOrderByLatest(APPROVED_STATUS, currentDate, pageable);
        }
    }
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
     * 캠페인 썸네일 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public CampaignThumbnailResponse getCampaignThumbnail(Long campaignId) {
        Campaign campaign = findApprovedCampaignOnlyById(campaignId);
        return CampaignThumbnailResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 기본 정보 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public CampaignBasicInfoResponse getCampaignBasicInfo(Long campaignId) {
        Campaign campaign = findApprovedCampaignOnlyById(campaignId);
        return CampaignBasicInfoResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 상세 정보 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public CampaignDetailInfoResponse getCampaignDetailInfo(Long campaignId) {
        Campaign campaign = findApprovedCampaignOnlyById(campaignId);
        return CampaignDetailInfoResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 미션 가이드 조회 (승인된 캠페인만)
     */
    @Transactional(readOnly = true)
    public CampaignMissionGuideResponse getCampaignMissionGuide(Long campaignId) {
        Campaign campaign = findApprovedCampaignOnlyById(campaignId);
        return CampaignMissionGuideResponse.fromEntity(campaign);
    }


    /**
     * ID로 승인된 캠페인만 조회 (거절된 캠페인과 대기 중인 캠페인 제외)
     * 일반 사용자에게는 승인된 캠페인만 보여줍니다.
     */
    private Campaign findApprovedCampaignOnlyById(Long campaignId) {
        return campaignRepository.findByIdAndApprovalStatus(campaignId, APPROVED_STATUS)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없거나 접근할 수 없습니다."));
    }

    /**
     * ID로 승인된 또는 대기 중인 캠페인 조회 (거절된 캠페인 제외)
     * 관리자용 또는 특별한 경우에만 사용
     */
    private Campaign findApprovedOrPendingCampaignById(Long campaignId) {
        return campaignRepository.findByIdAndApprovalStatusIn(campaignId, 
                List.of(Campaign.ApprovalStatus.APPROVED, Campaign.ApprovalStatus.PENDING))
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다."));
    }

    /**
     * ID로 캠페인 조회 (승인 상태 무관) - 관리자용
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
     * 인기순 정렬 디버그를 위한 메서드
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> debugPopularitySort(String categoryType, String categoryName) {
        LocalDate currentDate = getCurrentDate();
        
        // 네이티브 쿼리로 직접 결과 확인
        Page<Object[]> resultPage = campaignRepository.findOptimizedCampaignListByPopularity(
                Campaign.ApprovalStatus.APPROVED.name(), currentDate, categoryType, categoryName, null,
                PageRequest.of(0, 10));
        
        List<Map<String, Object>> debugResults = new ArrayList<>();
        
        for (Object[] result : resultPage.getContent()) {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("campaignId", result[0]);
            debugInfo.put("title", result[1]);
            debugInfo.put("currentApplicants", result[10]);
            debugInfo.put("categoryType", result[9]);
            debugInfo.put("categoryName", result[8]);
            debugResults.add(debugInfo);
        }
        
        log.info("인기순 정렬 디버그 결과 - 총 {}개 캠페인", debugResults.size());
        debugResults.forEach(debug -> 
            log.info("캠페인 ID: {}, 제목: {}, 신청자 수: {}, 카테고리: {}-{}", 
                debug.get("campaignId"), debug.get("title"), debug.get("currentApplicants"),
                debug.get("categoryType"), debug.get("categoryName")));
        
        return debugResults;
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllCampaignsForDebug() {
        var campaigns = campaignRepository.findAll();
        return campaigns.stream()
                .map(campaign -> {
                    Map<String, Object> campaignInfo = new java.util.HashMap<>();
                    campaignInfo.put("id", campaign.getId());
                    campaignInfo.put("title", campaign.getTitle());
                    campaignInfo.put("approvalStatus", campaign.getApprovalStatus().name());
                    campaignInfo.put("recruitmentEndDate", campaign.getRecruitmentEndDate());
                    campaignInfo.put("categoryType", campaign.getCategory() != null ?
                            campaign.getCategory().getCategoryType().name() : null);
                    campaignInfo.put("categoryName", campaign.getCategory() != null ?
                            campaign.getCategory().getCategoryName() : null);
                    campaignInfo.put("createdAt", campaign.getCreatedAt());
                    return campaignInfo;
                })
                .collect(Collectors.toList());
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

    /**
     * 키워드로 캠페인 검색 (정렬 및 플랫폼 필터링 지원)
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignListSimpleResponse> searchCampaignsWithFilters(
            String keyword, int page, int size, String sortField, boolean isDescending, List<String> campaignTypes) {

        log.info("필터링된 캠페인 검색 실행 - keyword: {}, page: {}, size: {}, sortField: {}, campaignTypes: {}", 
                keyword, page, size, sortField, campaignTypes);

        LocalDate currentDate = getCurrentDate();
        Pageable pageable = PageRequest.of(page, size);

        Page<Campaign> campaignPage;

        if ("currentApplicants".equals(sortField)) {
            // 인기순 (신청자 수 기준) 검색
            log.info("인기순 검색 실행");
            if (campaignTypes != null && !campaignTypes.isEmpty()) {
                campaignPage = campaignRepository.searchApprovedActiveByKeywordAndCampaignTypesOrderByPopularity(
                        APPROVED_STATUS, currentDate, keyword, campaignTypes, pageable);
            } else {
                campaignPage = campaignRepository.searchApprovedActiveByKeywordOrderByPopularity(
                        APPROVED_STATUS, currentDate, keyword, pageable);
            }
        } else {
            // 최신순 검색
            log.info("최신순 검색 실행");
            if (campaignTypes != null && !campaignTypes.isEmpty()) {
                campaignPage = campaignRepository.searchApprovedActiveByKeywordAndCampaignTypesOrderByLatest(
                        APPROVED_STATUS, currentDate, keyword, campaignTypes, pageable);
            } else {
                campaignPage = campaignRepository.searchApprovedActiveByKeywordOrderByLatest(
                        APPROVED_STATUS, currentDate, keyword, null, null, null, pageable);
            }
        }

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

        log.info("필터링된 검색 완료 - {}개 캠페인", campaigns.size());
        return PageResponse.from(responsePage);
    }
}