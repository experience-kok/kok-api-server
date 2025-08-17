package com.example.auth.service;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignCategory;
import com.example.auth.domain.CampaignMissionInfo;
import com.example.auth.domain.User;
import com.example.auth.dto.campaign.UpdateCampaignRequest;
import com.example.auth.dto.campaign.CreateCampaignResponse;
import com.example.auth.exception.AccessDeniedException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignApplicationRepository;
import com.example.auth.repository.CampaignCategoryRepository;
import com.example.auth.repository.CampaignMissionInfoRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * 캠페인 수정 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 
 * 수정 권한 분류:
 * 1. 상시 수정 가능: 썸네일, 캠페인 제목, 최대 지원 가능 인원, 방문 정보 일부
 * 2. 신청자 없을 때만 수정 가능: 대부분의 캠페인 핵심 정보
 * 3. 수정 불가: 사업체 담당자 정보, 위치 정보
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignUpdateService {

    private final CampaignRepository campaignRepository;
    private final CampaignCategoryRepository categoryRepository;
    private final CampaignLocationService campaignLocationService;
    private final CampaignApplicationRepository applicationRepository;
    private final CampaignMissionInfoRepository missionInfoRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ImageProcessingService imageProcessingService;

    // 상시 수정 가능한 필드들
    private static final Set<String> ALWAYS_EDITABLE_FIELDS = Set.of(
        "thumbnailUrl", "title", "maxApplicants", 
        "officialWebsite", "contactNumber", "visitReservationInfo"
    );

    // 신청자 없을 때만 수정 가능한 필드들
    private static final Set<String> EDITABLE_WHEN_NO_APPLICANTS = Set.of(
        "campaignType", "categoryType", "category", "productShortInfo",
        "recruitmentStartDate", "recruitmentEndDate", "productDetails",
        "selectionCriteria", "selectionDate", "missionInfo"
    );

    // 수정 불가 필드들
    private static final Set<String> NEVER_EDITABLE_FIELDS = Set.of(
        "contactPerson", "phoneNumber", "location"
    );

    /**
     * 캠페인 수정 메서드 (권한 기반)
     * @param userId 사용자 ID
     * @param campaignId 캠페인 ID
     * @param request 수정 요청
     * @return 수정된 캠페인 정보
     */
    @Transactional
    public CreateCampaignResponse updateCampaign(Long userId, Long campaignId, UpdateCampaignRequest request) {
        // 기본 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없어요."));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없어요."));

        validateUpdatePermission(user, campaign);

        // 신청자 수 확인
        int currentApplicants = getCurrentApplicantCount(campaignId);
        boolean hasApplicants = currentApplicants > 0;

        log.info("캠페인 수정 시도 - ID: {}, 현재 신청자 수: {}", campaignId, currentApplicants);

        // 수정 권한 검증 및 필드별 업데이트
        updateCampaignFieldsWithPermission(campaign, request, hasApplicants);

        // 승인 상태를 다시 PENDING으로 변경 (수정 사항이 있는 경우)
        if (hasSignificantChanges(request)) {
            campaign.resetApprovalStatus();
            log.info("캠페인 수정으로 인해 승인 상태가 PENDING으로 변경되었습니다. 캠페인 ID: {}", campaignId);
        }

        log.info("캠페인이 수정되었습니다. ID: {}, 제목: {}", campaign.getId(), campaign.getTitle());

        return CreateCampaignResponse.fromEntity(campaign);
    }

    /**
     * 캠페인 수정 권한 검증
     */
    private void validateUpdatePermission(User user, Campaign campaign) {
        // 본인이 생성한 캠페인인지 확인
        if (!campaign.getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException("본인이 생성한 캠페인만 수정할 수 있어요.");
        }

        // 승인 상태별 수정 가능 여부 확인
        Campaign.ApprovalStatus status = campaign.getApprovalStatus();
        switch (status) {
            case PENDING:
                // 대기 중인 캠페인은 수정 가능
                break;
            case APPROVED:
                // 승인된 캠페인은 제한적 수정만 가능
                log.info("승인된 캠페인의 제한적 수정 요청. 캠페인 ID: {}", campaign.getId());
                break;
            case REJECTED:
                // 거절된 캠페인도 수정 가능 (재신청 목적)
                break;
            default:
                throw new IllegalArgumentException("알 수 없는 승인 상태입니다: " + status);
        }
    }

    /**
     * 현재 캠페인의 신청자 수 조회
     */
    private int getCurrentApplicantCount(Long campaignId) {
        Integer count = campaignRepository.countCurrentApplicationsByCampaignId(campaignId);
        return count != null ? count : 0;
    }

    /**
     * 권한 기반으로 캠페인 필드 업데이트
     */
    private void updateCampaignFieldsWithPermission(Campaign campaign, UpdateCampaignRequest request, boolean hasApplicants) {
        // 1. 상시 수정 가능한 필드들
        updateAlwaysEditableFields(campaign, request, hasApplicants);

        // 2. 신청자 없을 때만 수정 가능한 필드들
        if (!hasApplicants) {
            updateEditableWhenNoApplicantsFields(campaign, request);
        } else {
            validateRestrictedFieldsNotChanged(campaign, request);
        }

        // 3. 방문 정보 처리 (특별 로직)
        updateVisitInfoFields(campaign, request);
    }

    /**
     * 상시 수정 가능한 필드들 업데이트
     */
    private void updateAlwaysEditableFields(Campaign campaign, UpdateCampaignRequest request, boolean hasApplicants) {
        // 상시 등록 여부
        if (request.getIsAlwaysOpen() != null) {
            updateAlwaysOpenStatus(campaign, request.getIsAlwaysOpen(), hasApplicants);
        }

        // 썸네일 URL
        if (request.getThumbnailUrl() != null) {
            updateThumbnailUrl(campaign, request.getThumbnailUrl());
        }

        // 캠페인 제목
        if (request.getTitle() != null) {
            campaign.setTitle(request.getTitle());
            log.debug("캠페인 제목 수정: {}", request.getTitle());
        }

        // 최대 지원 가능 인원 (특별 로직)
        if (request.getMaxApplicants() != null) {
            updateMaxApplicants(campaign, request.getMaxApplicants(), hasApplicants);
        }
    }

    /**
     * 신청자 없을 때만 수정 가능한 필드들 업데이트
     */
    private void updateEditableWhenNoApplicantsFields(Campaign campaign, UpdateCampaignRequest request) {
        // 캠페인 타입
        if (request.getCampaignType() != null) {
            campaign.setCampaignType(request.getCampaignType());
        }

        // 카테고리 정보
        if (request.getCategory() != null) {
            CampaignCategory category = findCategoryByTypeAndName(request.getCategory());
            campaign.setCategory(category);
        }

        // 제품 서비스 간략 정보
        if (request.getProductShortInfo() != null) {
            campaign.setProductShortInfo(request.getProductShortInfo());
        }

        // 날짜 정보들
        updateDateFields(campaign, request);

        // 상세 정보들
        updateDetailFields(campaign, request);

        // 미션 정보들
        updateMissionInfo(campaign, request);

        log.info("신청자가 없는 상태에서 캠페인 핵심 정보가 수정되었습니다. 캠페인 ID: {}", campaign.getId());
    }

    /**
     * 상시 등록 여부 업데이트
     */
    private void updateAlwaysOpenStatus(Campaign campaign, Boolean newIsAlwaysOpen, boolean hasApplicants) {
        Boolean currentIsAlwaysOpen = campaign.getIsAlwaysOpen();
        
        // 현재 상태와 동일하면 변경하지 않음
        if (currentIsAlwaysOpen.equals(newIsAlwaysOpen)) {
            return;
        }

        // 상시 캠페인으로 변경하려는 경우
        if (newIsAlwaysOpen) {
            // 상시 캠페인은 방문형만 가능
            if (!"방문".equals(campaign.getCategory().getCategoryType().name())) {
                throw new IllegalArgumentException("상시 캠페인은 방문형 캠페인만 가능해요.");
            }

            // 상시 캠페인에서 허용되는 카테고리 확인
            String categoryName = campaign.getCategory().getCategoryName();
            if (!isValidAlwaysOpenCategory(categoryName)) {
                throw new IllegalArgumentException("상시 캠페인은 카페, 맛집, 뷰티, 숙박 카테고리만 가능해요.");
            }

            // 방문 정보가 있는지 확인
            if (!campaign.hasLocation()) {
                throw new IllegalArgumentException("상시 캠페인으로 변경하려면 방문 정보가 필요해요.");
            }

            log.info("캠페인을 상시 캠페인으로 변경합니다. 캠페인 ID: {}", campaign.getId());
        } else {
            // 일반 캠페인으로 변경하려는 경우
            if (hasApplicants) {
                throw new IllegalArgumentException("신청자가 있는 상시 캠페인은 일반 캠페인으로 변경할 수 없어요.");
            }

            // 일반 캠페인에 필요한 필드들이 있는지 확인
            if (campaign.getRecruitmentEndDate() == null || campaign.getSelectionDate() == null) {
                throw new IllegalArgumentException("일반 캠페인으로 변경하려면 모집 종료일과 선정일이 필요해요.");
            }

            log.info("상시 캠페인을 일반 캠페인으로 변경합니다. 캠페인 ID: {}", campaign.getId());
        }

        campaign.setIsAlwaysOpen(newIsAlwaysOpen);
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
    private void updateMaxApplicants(Campaign campaign, Integer newMaxApplicants, boolean hasApplicants) {
        int currentMax = campaign.getMaxApplicants();
        
        if (hasApplicants) {
            int currentApplicantCount = getCurrentApplicantCount(campaign.getId());
            
            // 신청자가 있을 때는 늘리기만 가능, 줄일 때는 현재 신청자 수보다 많아야 함
            if (newMaxApplicants < currentMax) {
                if (newMaxApplicants < currentApplicantCount) {
                    throw new IllegalArgumentException(
                        String.format("신청자가 있는 경우 최대 인원을 현재 신청자 수(%d)보다 적게 설정할 수 없어요.", 
                        currentApplicantCount));
                }
                log.warn("신청자가 있는 상태에서 최대 인원을 {}에서 {}로 감소시켰습니다. 캠페인 ID: {}", 
                        currentMax, newMaxApplicants, campaign.getId());
            }
        }
        
        campaign.setMaxApplicants(newMaxApplicants);
        log.info("최대 지원 가능 인원 수정: {} -> {}, 캠페인 ID: {}", 
                currentMax, newMaxApplicants, campaign.getId());
    }

    /**
     * 방문 정보 필드들 업데이트 (상시 수정 가능)
     */
    private void updateVisitInfoFields(Campaign campaign, UpdateCampaignRequest request) {
        if (request.getVisitInfo() != null && "방문".equals(campaign.getCategory().getCategoryType().name())) {
            // 방문형 캠페인의 경우 방문 정보 업데이트
            campaignLocationService.updateCampaignLocationVisitInfo(campaign, request.getVisitInfo());
            log.info("방문 정보가 업데이트되었습니다. 캠페인 ID: {}", campaign.getId());
        }
    }

    /**
     * 날짜 필드들 업데이트
     */
    private void updateDateFields(Campaign campaign, UpdateCampaignRequest request) {
        if (request.getRecruitmentStartDate() != null) {
            campaign.setRecruitmentStartDate(request.getRecruitmentStartDate());
        }
        if (request.getRecruitmentEndDate() != null) {
            campaign.setRecruitmentEndDate(request.getRecruitmentEndDate());
        }
        if (request.getSelectionDate() != null) {
            campaign.setSelectionDate(request.getSelectionDate());
        }

        // 날짜 유효성 검증
        validateCampaignDates(campaign);
    }

    /**
     * 상세 정보 필드들 업데이트
     */
    private void updateDetailFields(Campaign campaign, UpdateCampaignRequest request) {
        if (request.getProductDetails() != null) {
            campaign.setProductDetails(request.getProductDetails());
        }
        if (request.getSelectionCriteria() != null) {
            campaign.setSelectionCriteria(request.getSelectionCriteria());
        }
    }

    /**
     * 미션 정보 업데이트
     */
    private void updateMissionInfo(Campaign campaign, UpdateCampaignRequest request) {
        if (request.getMissionInfo() == null) {
            return;
        }

        UpdateCampaignRequest.MissionInfo missionInfoRequest = request.getMissionInfo();
        
        // 기존 미션 정보 조회 또는 신규 생성
        CampaignMissionInfo missionInfo = campaign.getMissionInfo();
        if (missionInfo == null) {
            missionInfo = CampaignMissionInfo.builder()
                    .campaign(campaign)
                    .build();
        }

        // 키워드 정보 업데이트
        if (missionInfoRequest.getTitleKeywords() != null) {
            missionInfo.setTitleKeywords(missionInfoRequest.getTitleKeywords().toArray(new String[0]));
        }
        if (missionInfoRequest.getBodyKeywords() != null) {
            missionInfo.setBodyKeywords(missionInfoRequest.getBodyKeywords().toArray(new String[0]));
        }

        // 콘텐츠 요구사항 업데이트
        if (missionInfoRequest.getNumberOfVideo() != null) {
            missionInfo.setNumberOfVideo(missionInfoRequest.getNumberOfVideo());
        }
        if (missionInfoRequest.getNumberOfImage() != null) {
            missionInfo.setNumberOfImage(missionInfoRequest.getNumberOfImage());
        }
        if (missionInfoRequest.getNumberOfText() != null) {
            missionInfo.setNumberOfText(missionInfoRequest.getNumberOfText());
        }
        if (missionInfoRequest.getIsMap() != null) {
            missionInfo.setIsMap(missionInfoRequest.getIsMap());
        }

        // 미션 가이드 및 날짜 업데이트
        if (missionInfoRequest.getMissionGuide() != null) {
            missionInfo.setMissionGuide(missionInfoRequest.getMissionGuide());
        }
        if (missionInfoRequest.getMissionStartDate() != null) {
            missionInfo.setMissionStartDate(missionInfoRequest.getMissionStartDate());
        }
        if (missionInfoRequest.getMissionDeadlineDate() != null) {
            missionInfo.setMissionDeadlineDate(missionInfoRequest.getMissionDeadlineDate());
        }

        // 미션 날짜 유효성 검증
        if (missionInfo.getMissionStartDate() != null && missionInfo.getMissionDeadlineDate() != null) {
            if (missionInfo.getMissionStartDate().isAfter(missionInfo.getMissionDeadlineDate())) {
                throw new IllegalArgumentException("미션 시작일은 미션 종료일보다 이전이어야 해요.");
            }
            if (missionInfo.getMissionStartDate().isBefore(campaign.getSelectionDate())) {
                throw new IllegalArgumentException("미션 시작일은 참가자 선정일 이후여야 해요.");
            }
        }

        // 저장
        CampaignMissionInfo savedMissionInfo = missionInfoRepository.save(missionInfo);
        campaign.setMissionInfo(savedMissionInfo);
        
        log.info("미션 정보가 업데이트되었습니다. 캠페인 ID: {}, 미션 정보 ID: {}", 
                campaign.getId(), savedMissionInfo.getId());
    }

    /**
     * 제한된 필드가 변경되지 않았는지 검증
     */
    private void validateRestrictedFieldsNotChanged(Campaign campaign, UpdateCampaignRequest request) {
        StringBuilder errorMessage = new StringBuilder();

        // 신청자가 있을 때 변경 불가한 필드들 검증
        if (request.getCampaignType() != null && !request.getCampaignType().equals(campaign.getCampaignType())) {
            errorMessage.append("신청자가 있는 경우 캠페인 타입을 변경할 수 없어요. ");
        }

        if (request.getCategory() != null) {
            CampaignCategory newCategory = findCategoryByTypeAndName(request.getCategory());
            if (!newCategory.getId().equals(campaign.getCategory().getId())) {
                errorMessage.append("신청자가 있는 경우 카테고리를 변경할 수 없어요. ");
            }
        }

        if (request.getRecruitmentStartDate() != null && 
            !request.getRecruitmentStartDate().equals(campaign.getRecruitmentStartDate())) {
            errorMessage.append("신청자가 있는 경우 모집 시작일을 변경할 수 없어요. ");
        }

        if (request.getRecruitmentEndDate() != null && 
            !request.getRecruitmentEndDate().equals(campaign.getRecruitmentEndDate())) {
            errorMessage.append("신청자가 있는 경우 모집 종료일을 변경할 수 없어요. ");
        }

        if (request.getMissionInfo() != null) {
            errorMessage.append("신청자가 있는 경우 미션 정보를 변경할 수 없어요. ");
        }

        if (errorMessage.length() > 0) {
            throw new IllegalArgumentException(errorMessage.toString().trim());
        }
    }

    /**
     * 썸네일 URL 업데이트
     */
    private void updateThumbnailUrl(Campaign campaign, String thumbnailUrl) {
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            String cleanUrl = cleanPresignedUrl(thumbnailUrl);
            String originalCloudFrontUrl = s3Service.getImageUrl(cleanUrl);
            campaign.setThumbnailUrl(originalCloudFrontUrl);
            
            // 비동기로 리사이징 완료 후 썸네일 URL 업데이트
            imageProcessingService.updateCampaignThumbnailWhenReady(campaign.getId(), cleanUrl);
            log.debug("썸네일 URL 수정: {}", originalCloudFrontUrl);
        }
    }

    /**
     * 캠페인 날짜 유효성 검증
     */
    private void validateCampaignDates(Campaign campaign) {
        LocalDate recruitmentStart = campaign.getRecruitmentStartDate();
        LocalDate recruitmentEnd = campaign.getRecruitmentEndDate();
        LocalDate selectionDate = campaign.getSelectionDate();
        // 미션 날짜 유효성 검증
        if (campaign.getMissionInfo() != null) {
            LocalDate missionDeadlineDate = campaign.getMissionInfo().getMissionDeadlineDate();
            if (missionDeadlineDate != null && missionDeadlineDate.isBefore(selectionDate)) {
                throw new IllegalArgumentException("미션 마감일은 선정일 이후여야 해요.");
            }
        }
    }

    /**
     * 카테고리 정보로 카테고리 조회
     */
    private CampaignCategory findCategoryByTypeAndName(UpdateCampaignRequest.CategoryInfo categoryInfo) {
        CampaignCategory.CategoryType categoryType;
        try {
            categoryType = CampaignCategory.CategoryType.valueOf(categoryInfo.getType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리 타입이에요: " + categoryInfo.getType());
        }

        return categoryRepository.findByCategoryTypeAndCategoryName(categoryType, categoryInfo.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("카테고리를 찾을 수 없어요. (타입: %s, 이름: %s)", 
                    categoryInfo.getType(), categoryInfo.getName())));
    }

    /**
     * Presigned URL 정리
     */
    private String cleanPresignedUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            return url.substring(0, queryIndex);
        }
        
        return url;
    }

    /**
     * 중요한 변경사항이 있는지 확인 (승인 상태 재설정 여부 결정)
     */
    private boolean hasSignificantChanges(UpdateCampaignRequest request) {
        // 제목, 카테고리, 내용 등 중요한 필드 변경 시 true 반환
        return request.getTitle() != null || 
               request.getCategory() != null || 
               request.getProductDetails() != null ||
               request.getCampaignType() != null ||
               request.getMissionInfo() != null;
    }

    /**
     * 캠페인 삭제 (논리적 삭제)
     * @param userId 사용자 ID
     * @param campaignId 캠페인 ID
     */
    @Transactional
    public void deleteCampaign(Long userId, Long campaignId) {
        // 기본 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없어요."));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없어요."));

        // 권한 검증: 본인이 생성한 캠페인인지 확인
        if (!campaign.getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException("본인이 생성한 캠페인만 삭제할 수 있어요.");
        }

        // 신청자 수 확인
        int currentApplicants = getCurrentApplicantCount(campaignId);
        
        // 신청자가 있는 경우 삭제 불가
        if (currentApplicants > 0) {
            throw new IllegalArgumentException(
                String.format("신청자가 있는 캠페인은 삭제할 수 없어요. (현재 신청자 수: %d명)", currentApplicants));
        }

        // 승인된 캠페인은 삭제 제한
        if (campaign.getApprovalStatus() == Campaign.ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("승인된 캠페인은 삭제할 수 없어요. 관리자에게 문의해주세요.");
        }

        // 미션 정보가 있다면 먼저 삭제
        if (campaign.getMissionInfo() != null) {
            missionInfoRepository.delete(campaign.getMissionInfo());
            log.info("캠페인 미션 정보가 삭제되었습니다. 캠페인 ID: {}", campaignId);
        }

        // 캠페인 삭제 (CASCADE로 연관 데이터도 함께 삭제됨)
        campaignRepository.delete(campaign);
        
        log.info("캠페인이 삭제되었습니다. ID: {}, 제목: {}, 삭제자: {}", 
                campaignId, campaign.getTitle(), user.getNickname());
    }
}
