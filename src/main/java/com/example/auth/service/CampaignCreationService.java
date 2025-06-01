package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignCategory;
import com.example.auth.domain.Company;
import com.example.auth.domain.User;
import com.example.auth.dto.campaign.CreateCampaignRequest;
import com.example.auth.dto.campaign.CreateCampaignResponse;
import com.example.auth.dto.company.CompanyRequest;
import com.example.auth.exception.AccessDeniedException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignCategoryRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.CompanyRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 캠페인 생성 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 
 * 클라이언트(광고주)가 새로운 캠페인을 등록하는 기능을 제공합니다.
 * 사용자 권한 확인, 카테고리 유효성 검증, 캠페인 정보 저장 등의
 * 과정을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignCreationService {

    private final CampaignRepository campaignRepository;
    private final CampaignCategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final UserRepository userRepository;

    /**
     * 캠페인 생성 메서드
     * 
     * 요청된 캠페인 정보를 검증하고 데이터베이스에 저장합니다.
     * CLIENT 권한을 가진 사용자만 캠페인을 생성할 수 있으며,
     * 캠페인은 기본적으로 'PENDING' 상태로 생성되어 관리자의 승인이 필요합니다.
     * 
     * @param userId 캠페인을 생성하려는 사용자의 ID (클라이언트)
     * @param request 캠페인 생성 요청 데이터
     * @return 생성된 캠페인 정보를 담은 응답 객체
     * @throws ResourceNotFoundException 사용자나 카테고리가 존재하지 않는 경우
     * @throws AccessDeniedException 사용자가 CLIENT 권한이 없는 경우
     */
    @Transactional
    public CreateCampaignResponse createCampaign(Long userId, CreateCampaignRequest request) {
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        
        // 사용자 권한 확인 (CLIENT 권한만 가능)
        if (!"CLIENT".equals(user.getRole())) {
            throw new AccessDeniedException("캠페인 등록은 클라이언트 권한을 가진 사용자만 가능합니다.");
        }
        
        // 날짜 유효성 검증
        validateCampaignDates(request);
        
        // 카테고리 조회 (type과 name으로)
        CampaignCategory category = findCategoryByTypeAndName(request.getCategory());
        
        // 업체 정보 생성 (요청에 업체 정보가 있는 경우)
        Company company = null;
        if (request.getCompanyInfo() != null) {
            CompanyRequest companyRequest = CompanyRequest.fromCompanyInfo(request.getCompanyInfo());
            company = companyService.createCompany(companyRequest);
        }
        
        // Campaign 엔티티 생성
        Campaign campaign = request.toEntity(user, category, company);
        
        // 캠페인 저장
        Campaign savedCampaign = campaignRepository.save(campaign);
        
        log.info("캠페인이 성공적으로 생성되었습니다. ID: {}, 제목: {}, 생성자: {}", 
                savedCampaign.getId(), savedCampaign.getTitle(), user.getNickname());
        
        return CreateCampaignResponse.fromEntity(savedCampaign);
    }

    /**
     * 캠페인 날짜 유효성을 검증합니다.
     * @param request 캠페인 생성 요청
     */
    private void validateCampaignDates(CreateCampaignRequest request) {
        LocalDate recruitmentStart = request.getRecruitmentStartDate();
        LocalDate recruitmentEnd = request.getRecruitmentEndDate();
        LocalDate applicationDeadline = request.getApplicationDeadlineDate();
        LocalDate selectionDate = request.getSelectionDate();
        LocalDate reviewDeadline = request.getReviewDeadlineDate();

        // 모집 시작일 <= 모집 종료일
        if (recruitmentStart.isAfter(recruitmentEnd)) {
            throw new IllegalArgumentException("모집 시작일은 모집 종료일보다 이전이어야 합니다.");
        }

        // 신청 마감일 >= 모집 시작일
        if (applicationDeadline.isBefore(recruitmentStart)) {
            throw new IllegalArgumentException("신청 마감일은 모집 시작일 이후여야 합니다.");
        }

        // 선정일 >= 모집 종료일
        if (selectionDate.isBefore(recruitmentEnd)) {
            throw new IllegalArgumentException("선정일은 모집 종료일 이후여야 합니다.");
        }

        // 리뷰 마감일 >= 선정일
        if (reviewDeadline.isBefore(selectionDate)) {
            throw new IllegalArgumentException("리뷰 마감일은 선정일 이후여야 합니다.");
        }
    }

    /**
     * 캠페인 수정 메서드
     * @param userId 사용자 ID
     * @param campaignId 캠페인 ID
     * @param request 수정 요청
     * @return 수정된 캠페인 정보
     */
    @Transactional
    public CreateCampaignResponse updateCampaign(Long userId, Long campaignId, CreateCampaignRequest request) {
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        // 캠페인 조회 및 권한 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다."));

        if (!campaign.getCreator().getId().equals(userId)) {
            throw new AccessDeniedException("본인이 생성한 캠페인만 수정할 수 있습니다.");
        }

        // 승인된 캠페인은 수정 불가
        if (campaign.getApprovalStatus() == Campaign.ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("승인된 캠페인은 수정할 수 없습니다.");
        }

        // 날짜 유효성 검증
        validateCampaignDates(request);

        // 카테고리 조회 (type과 name으로)
        CampaignCategory category = findCategoryByTypeAndName(request.getCategory());

        // 업체 정보 업데이트 (요청에 업체 정보가 있는 경우)
        Company company = campaign.getCompany();
        if (request.getCompanyInfo() != null) {
            CompanyRequest companyRequest = CompanyRequest.fromCompanyInfo(request.getCompanyInfo());
            
            if (company == null) {
                // 새로운 업체 생성
                company = companyService.createCompany(companyRequest);
            } else {
                // 기존 업체 정보 업데이트
                company = companyService.updateCompany(company, companyRequest);
            }
        }

        // 캠페인 정보 업데이트
        updateCampaignFields(campaign, request, category, company);

        // 승인 상태를 다시 PENDING으로 변경
        campaign.resetApprovalStatus();

        log.info("캠페인이 수정되었습니다. ID: {}, 제목: {}", campaign.getId(), campaign.getTitle());

        return CreateCampaignResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 필드를 업데이트합니다.
     */
    private void updateCampaignFields(Campaign campaign, CreateCampaignRequest request, 
                                    CampaignCategory category, Company company) {
        campaign.setCompany(company);
        campaign.setThumbnailUrl(request.getThumbnailUrl());
        campaign.setCampaignType(request.getCampaignType());
        campaign.setTitle(request.getTitle());
        campaign.setProductShortInfo(request.getProductShortInfo());
        campaign.setMaxApplicants(request.getMaxApplicants());
        campaign.setProductDetails(request.getProductDetails());
        campaign.setRecruitmentStartDate(request.getRecruitmentStartDate());
        campaign.setRecruitmentEndDate(request.getRecruitmentEndDate());
        campaign.setApplicationDeadlineDate(request.getApplicationDeadlineDate());
        campaign.setSelectionDate(request.getSelectionDate());
        campaign.setReviewDeadlineDate(request.getReviewDeadlineDate());
        campaign.setSelectionCriteria(request.getSelectionCriteria());
        campaign.setMissionGuide(request.getMissionGuide());
        campaign.setMissionKeywords(request.getMissionKeywords());
        campaign.setCategory(category);
    }

    /**
     * 카테고리 정보로 카테고리를 조회합니다.
     * @param categoryInfo 카테고리 타입과 이름 정보
     * @return 찾은 카테고리
     * @throws ResourceNotFoundException 카테고리를 찾을 수 없는 경우
     */
    private CampaignCategory findCategoryByTypeAndName(CreateCampaignRequest.CategoryInfo categoryInfo) {
        // 문자열 타입을 Enum으로 변환
        CampaignCategory.CategoryType categoryType;
        try {
            categoryType = CampaignCategory.CategoryType.valueOf(categoryInfo.getType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리 타입입니다: " + categoryInfo.getType() + ". 가능한 값: 방문, 배송");
        }

        return categoryRepository.findByCategoryTypeAndCategoryName(categoryType, categoryInfo.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("카테고리를 찾을 수 없습니다. (타입: %s, 이름: %s)", 
                    categoryInfo.getType(), categoryInfo.getName())));
    }
}
