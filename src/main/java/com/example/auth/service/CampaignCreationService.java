package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignCategory;
import com.example.auth.domain.CampaignMissionInfo;
import com.example.auth.domain.Company;
import com.example.auth.domain.User;
import com.example.auth.dto.campaign.CreateCampaignRequest;
import com.example.auth.dto.campaign.CreateCampaignResponse;
import com.example.auth.exception.AccessDeniedException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignCategoryRepository;
import com.example.auth.repository.CampaignMissionInfoRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

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
    private final CampaignLocationService campaignLocationService;
    private final CampaignMissionInfoRepository missionInfoRepository;
    private final CompanyService companyService;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ImageProcessingService imageProcessingService;

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
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없어요."));

        // 사용자 권한 확인 (CLIENT 권한만 가능)
        if (!"CLIENT".equals(user.getRole())) {
            throw new AccessDeniedException("캠페인 등록은 클라이언트 권한을 가진 사용자만 가능해요.");
        }

        // 날짜 유효성 검증
        validateCampaignDates(request);

        // 상시 캠페인 유효성 검증
        validateAlwaysOpenCampaign(request);

        // 카테고리 조회 (type과 name으로)
        CampaignCategory category = findCategoryByTypeAndName(request.getCategory());

        // 사용자의 기존 업체 정보 조회 (CLIENT라면 반드시 있어야 함)
        Optional<Company> existingCompany = companyService.findByUserId(userId);
        Company company = null;

        if (existingCompany.isPresent()) {
            // 기존 업체 정보가 있다면 담당자 정보만 업데이트
            company = existingCompany.get();
            if (request.getCompanyInfo() != null) {
                company.updateCompanyInfo(
                        company.getCompanyName(), // 기존 업체명 유지
                        request.getCompanyInfo().getContactPerson(),
                        request.getCompanyInfo().getPhoneNumber(),
                        company.getBusinessRegistrationNumber() // 기존 사업자번호 유지
                );
                log.info("기존 업체의 담당자 정보를 업데이트했습니다. 사용자 ID: {}, 담당자: {}",
                        userId, request.getCompanyInfo().getContactPerson());
            }
        } else {
            // CLIENT 권한인데 업체 정보가 없다면 오류
            if ("CLIENT".equals(user.getRole())) {
                throw new IllegalStateException("CLIENT 권한 사용자는 사업자 정보가 등록되어 있어야 해요.");
            }

            // USER 권한이라면 담당자 정보로 임시 업체 생성 (하위 호환성)
            if (request.getCompanyInfo() != null) {
                company = Company.builder()
                        .user(user)
                        .companyName("임시 업체명") // 나중에 사업자 정보 등록 시 업데이트
                        .contactPerson(request.getCompanyInfo().getContactPerson())
                        .phoneNumber(request.getCompanyInfo().getPhoneNumber())
                        .build();
                company = companyService.saveCompany(company);
                log.info("USER 권한으로 임시 업체를 생성했습니다. 사용자 ID: {}, 담당자: {}",
                        userId, request.getCompanyInfo().getContactPerson());
            }
        }

        // Campaign 엔티티 생성
        Campaign campaign = request.toEntity(user, category);
        if (company != null) {
            campaign.setCompany(company);
        }

        // 썸네일 URL 처리 (일단 원본 CloudFront URL로 저장)
        if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().isEmpty()) {
            String cleanUrl = cleanPresignedUrl(request.getThumbnailUrl());
            String originalCloudFrontUrl = s3Service.getImageUrl(cleanUrl);
            campaign.setThumbnailUrl(originalCloudFrontUrl);
        }

        // 캠페인 저장
        Campaign savedCampaign = campaignRepository.save(campaign);

        // 미션 정보 처리
        if (request.getMissionInfo() != null) {
            createMissionInfo(savedCampaign, request.getMissionInfo());
        }

        // 방문 정보 처리 (카테고리 타입에 따라)
        if (request.getVisitInfo() != null) {
            if ("방문".equals(request.getCategory().getType())) {
                // 방문형 캠페인: visitInfo 저장
                campaignLocationService.createCampaignLocationWithVisitInfo(savedCampaign, request.getVisitInfo());
                log.info("방문형 캠페인의 방문 정보가 저장되었습니다. 캠페인 ID: {}", savedCampaign.getId());
            } else {
                // 배송형 캠페인: visitInfo를 받되 저장하지 않음 (유효성 검사만 수행됨)
                log.info("배송형 캠페인입니다. 방문 정보는 저장되지 않습니다. 캠페인 ID: {}", savedCampaign.getId());
            }
        }

        // 비동기로 리사이징 완료 후 썸네일 URL 업데이트
        if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().isEmpty()) {
            String cleanUrl = cleanPresignedUrl(request.getThumbnailUrl());
            imageProcessingService.updateCampaignThumbnailWhenReady(savedCampaign.getId(), cleanUrl);
        }

        log.info("캠페인이 성공적으로 생성되었습니다. ID: {}, 제목: {}, 생성자: {}",
                savedCampaign.getId(), savedCampaign.getTitle(), user.getNickname());

        return CreateCampaignResponse.fromEntity(savedCampaign);
    }

    /**
     * 캠페인 미션 정보 생성
     */
    private void createMissionInfo(Campaign campaign, CreateCampaignRequest.MissionInfo missionInfoRequest) {
        CampaignMissionInfo missionInfo = CampaignMissionInfo.builder()
                .campaign(campaign)
                .titleKeywords(missionInfoRequest.getTitleKeywords() != null ?
                        missionInfoRequest.getTitleKeywords().toArray(new String[0]) : null)
                .bodyKeywords(missionInfoRequest.getBodyKeywords() != null ?
                        missionInfoRequest.getBodyKeywords().toArray(new String[0]) : null)
                .numberOfVideo(missionInfoRequest.getNumberOfVideo())
                .numberOfImage(missionInfoRequest.getNumberOfImage())
                .numberOfText(missionInfoRequest.getNumberOfText())
                .isMap(missionInfoRequest.getIsMap())
                .missionGuide(missionInfoRequest.getMissionGuide())
                .missionStartDate(missionInfoRequest.getMissionStartDate())
                .missionDeadlineDate(missionInfoRequest.getMissionDeadlineDate())
                .build();

        CampaignMissionInfo savedMissionInfo = missionInfoRepository.save(missionInfo);
        campaign.setMissionInfo(savedMissionInfo);

        log.info("캠페인 미션 정보가 생성되었습니다. 캠페인 ID: {}, 미션 정보 ID: {}",
                campaign.getId(), savedMissionInfo.getId());
    }

    /**
     * 캠페인 날짜 유효성을 검증합니다.
     * @param request 캠페인 생성 요청
     */
    private void validateCampaignDates(CreateCampaignRequest request) {
        Boolean isAlwaysOpen = request.getIsAlwaysOpen();
        
        if (isAlwaysOpen != null && isAlwaysOpen) {
            // 상시 캠페인의 경우: recruitmentStartDate만 필수, 나머지는 선택사항
            if (request.getRecruitmentStartDate() == null) {
                throw new IllegalArgumentException("상시 캠페인에서도 모집 시작일은 필수예요.");
            }
            // 상시 캠페인에서는 recruitmentEndDate, selectionDate가 null일 수 있음
            return;
        }

        // 일반 캠페인의 경우: 기존 로직 그대로 적용
        LocalDate recruitmentStart = request.getRecruitmentStartDate();
        LocalDate recruitmentEnd = request.getRecruitmentEndDate();
        LocalDate selectionDate = request.getSelectionDate();

        // 일반 캠페인에서는 모든 날짜가 필수
        if (recruitmentStart == null) {
            throw new IllegalArgumentException("모집 시작일은 필수예요.");
        }
        if (recruitmentEnd == null) {
            throw new IllegalArgumentException("모집 종료일은 필수예요.");
        }
        if (selectionDate == null) {
            throw new IllegalArgumentException("선정일은 필수예요.");
        }

        // 기본 캠페인 날짜 검증
        if (recruitmentStart.isAfter(recruitmentEnd)) {
            throw new IllegalArgumentException("모집 시작일은 모집 종료일보다 이전이어야 해요.");
        }

        if (selectionDate.isBefore(recruitmentEnd)) {
            throw new IllegalArgumentException("선정일은 모집 종료일 이후여야 해요.");
        }

        // 미션 정보 날짜 검증 (일반 캠페인에서만)
        if (request.getMissionInfo() != null) {
            LocalDate missionStartDate = request.getMissionInfo().getMissionStartDate();
            LocalDate missionDeadlineDate = request.getMissionInfo().getMissionDeadlineDate();

            if (missionStartDate != null && missionDeadlineDate != null) {
                // 미션 시작일 >= 선정일
                if (missionStartDate.isBefore(selectionDate)) {
                    throw new IllegalArgumentException("미션 시작일은 선정일 이후여야 해요.");
                }

                // 미션 마감일 >= 미션 시작일
                if (missionDeadlineDate.isBefore(missionStartDate)) {
                    throw new IllegalArgumentException("미션 마감일은 미션 시작일 이후여야 해요.");
                }
            }
        }
    }

    /**
     * 상시 캠페인 유효성을 검증합니다.
     * @param request 캠페인 생성 요청
     */
    private void validateAlwaysOpenCampaign(CreateCampaignRequest request) {
        Boolean isAlwaysOpen = request.getIsAlwaysOpen();
        if (isAlwaysOpen == null || !isAlwaysOpen) {
            // 일반 캠페인인 경우 검증하지 않음
            return;
        }

        // 상시 캠페인은 방문형만 가능
        if (request.getCategory() == null || !"방문".equals(request.getCategory().getType())) {
            throw new IllegalArgumentException("상시 캠페인은 방문형 캠페인만 가능해요.");
        }

        // 상시 캠페인에서 허용되는 카테고리 확인
        String categoryName = request.getCategory().getName();
        if (!isValidAlwaysOpenCategory(categoryName)) {
            throw new IllegalArgumentException("상시 캠페인은 카페, 맛집, 뷰티, 숙박 카테고리만 가능해요.");
        }

        // 상시 캠페인에서는 방문 정보가 필수
        if (request.getVisitInfo() == null) {
            throw new IllegalArgumentException("상시 캠페인에서는 방문 정보가 필수예요.");
        }
    }

    /**
     * 상시 캠페인에서 허용되는 카테고리인지 확인합니다.
     * @param categoryName 카테고리명
     * @return 허용 여부
     */
    private boolean isValidAlwaysOpenCategory(String categoryName) {
        return "카페".equals(categoryName) || 
               "맛집".equals(categoryName) || 
               "뷰티".equals(categoryName) || 
               "숙박".equals(categoryName);
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
            throw new IllegalArgumentException("유효하지 않은 카테고리 타입이에요: " + categoryInfo.getType() + ". 가능한 값: 방문, 배송");
        }

        return categoryRepository.findByCategoryTypeAndCategoryName(categoryType, categoryInfo.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("카테고리를 찾을 수 없어요. (타입: %s, 이름: %s)",
                                categoryInfo.getType(), categoryInfo.getName())));
    }

    /**
     * Presigned URL에서 쿼리 파라미터를 제거하여 깨끗한 S3 URL을 반환
     * @param url Presigned URL 또는 일반 URL
     * @return 쿼리 파라미터가 제거된 깨끗한 URL
     */
    private String cleanPresignedUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // 쿼리 파라미터가 있는 경우 제거
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            return url.substring(0, queryIndex);
        }

        return url;
    }
}
