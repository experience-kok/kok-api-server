package com.example.auth.service;

import com.example.auth.constant.ApplicationStatus;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignApplication;
import com.example.auth.dto.application.ApplicationResponse;
import com.example.auth.dto.campaign.*;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.repository.CampaignApplicationRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

/**
 * 내 캠페인 목록 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyCampaignService {

    private final CampaignApplicationRepository applicationRepository;
    private final CampaignRepository campaignRepository;

    /**
     * 사용자 역할에 따른 캠페인 요약 정보 조회
     * 역할별로 다른 응답 타입 반환
     */
    public Object getMyCampaignSummary(Long userId, String userRole) {
        log.info("내 캠페인 요약 조회: userId={}, role={}", userId, userRole);

        if ("CLIENT".equals(userRole)) {
            return getClientCampaignSummary(userId);
        } else {
            return getUserCampaignSummary(userId);
        }
    }

    /**
     * 사용자 역할에 따른 캠페인 상세 목록 조회
     */
    public MyCampaignListResponse getMyCampaignList(Long userId, String userRole, String category, Pageable pageable) {
        log.info("내 캠페인 목록 조회: userId={}, role={}, category={}", userId, userRole, category);

        if ("CLIENT".equals(userRole)) {
            return getClientCampaignList(userId, category, pageable);
        } else {
            return getUserCampaignList(userId, category, pageable);
        }
    }

    /**
     * USER 역할 - 캠페인 요약 정보 조회
     */
    private UserCampaignSummaryResponse getUserCampaignSummary(Long userId) {
        LocalDate currentDate = LocalDate.now();

        // 기본 상태별 카운트 조회
        List<Object[]> statusCounts = applicationRepository.countByUserIdGroupByStatus(userId);

        // 모집 기간 기준으로 APPLIED 상태 분리 조회 (기존 PENDING을 APPLIED로 변경)
        Long appliedCount = applicationRepository.countPendingDuringRecruitment(userId, currentDate);
        Long pendingCount = applicationRepository.countPendingAfterRecruitment(userId, currentDate);

        UserCampaignSummaryResponse.UserSummary.UserSummaryBuilder summaryBuilder = UserCampaignSummaryResponse.UserSummary.builder();

        // 기본값 설정
        int selectedCount = 0;
        int completedCount = 0;
        int rejectedCount = 0;

        // SELECTED, COMPLETED, REJECTED 카운트 설정
        for (Object[] row : statusCounts) {
            ApplicationStatus status = (ApplicationStatus) row[0];
            Long count = (Long) row[1];

            switch (status) {
                case SELECTED:
                    selectedCount = count.intValue();
                    break;
                case COMPLETED:
                    completedCount = count.intValue();
                    break;
                case REJECTED:
                    rejectedCount = count.intValue();
                    break;
                // APPLIED, PENDING은 따로 처리하므로 제외
            }
        }

        summaryBuilder
                .applied(UserCampaignSummaryResponse.CategorySummary.builder()
                        .count(appliedCount.intValue()).label("지원").build()) // 모집 기간 중인 APPLIED
                .pending(UserCampaignSummaryResponse.CategorySummary.builder()
                        .count(pendingCount.intValue()).label("대기중").build()) // 모집 기간 끝난 PENDING
                .selected(UserCampaignSummaryResponse.CategorySummary.builder()
                        .count(selectedCount).label("선정").build())
                .rejected(UserCampaignSummaryResponse.CategorySummary.builder()
                        .count(rejectedCount).label("반려").build())
                .completed(UserCampaignSummaryResponse.CategorySummary.builder()
                        .count(completedCount).label("완료").build());

        return UserCampaignSummaryResponse.builder()
                .role("USER")
                .summary(summaryBuilder.build())
                .build();
    }

    /**
     * CLIENT 역할 - 캠페인 요약 정보 조회
     */
    private ClientCampaignSummaryResponse getClientCampaignSummary(Long userId) {
        List<Object[]> statusCounts = campaignRepository.countByCreatorIdGroupByApprovalStatus(userId);
        Long expiredCount = campaignRepository.countExpiredByCreatorId(userId, LocalDate.now());

        ClientCampaignSummaryResponse.ClientSummary.ClientSummaryBuilder summaryBuilder = ClientCampaignSummaryResponse.ClientSummary.builder();

        // 기본값 설정
        summaryBuilder
                .pending(ClientCampaignSummaryResponse.CategorySummary.builder()
                        .count(0).label("대기중").build())
                .approved(ClientCampaignSummaryResponse.CategorySummary.builder()
                        .count(0).label("승인됨").build())
                .rejected(ClientCampaignSummaryResponse.CategorySummary.builder()
                        .count(0).label("거절됨").build())
                .expired(ClientCampaignSummaryResponse.CategorySummary.builder()
                        .count(expiredCount.intValue()).label("만료됨").build());

        // 실제 카운트 설정
        for (Object[] row : statusCounts) {
            Campaign.ApprovalStatus status = (Campaign.ApprovalStatus) row[0];  // ApprovalStatus enum으로 받음
            Long count = (Long) row[1];

            switch (status) {
                case PENDING:
                    summaryBuilder.pending(ClientCampaignSummaryResponse.CategorySummary.builder()
                            .count(count.intValue()).label("대기중").build());
                    break;
                case APPROVED:
                    // 승인된 것 중 만료되지 않은 것만 카운트
                    int approvedCount = count.intValue() - expiredCount.intValue();
                    summaryBuilder.approved(ClientCampaignSummaryResponse.CategorySummary.builder()
                            .count(Math.max(0, approvedCount)).label("승인됨").build());
                    break;
                case REJECTED:
                    summaryBuilder.rejected(ClientCampaignSummaryResponse.CategorySummary.builder()
                            .count(count.intValue()).label("거절됨").build());
                    break;
            }
        }

        return ClientCampaignSummaryResponse.builder()
                .role("CLIENT")
                .summary(summaryBuilder.build())
                .build();
    }

    /**
     * USER 역할 - 캠페인 상세 목록 조회
     */
    private MyCampaignListResponse getUserCampaignList(Long userId, String category, Pageable pageable) {
        Page<CampaignApplication> applications;
        LocalDate currentDate = LocalDate.now();

        // 카테고리에 따른 필터링
        switch (category) {
            case "applied":
                // 모집 기간 중인 APPLIED 상태 신청
                applications = applicationRepository.findAppliedByUserId(userId, currentDate, pageable);
                break;
            case "pending":
                // 모집 기간 끝난 PENDING 상태 신청
                applications = applicationRepository.findPendingByUserId(userId, currentDate, pageable);
                break;
            case "selected":
                applications = applicationRepository.findByUserIdAndStatus(userId, ApplicationStatus.SELECTED, pageable);
                break;
            case "rejected":
                // REJECTED 상태 신청 조회
                applications = applicationRepository.findByUserIdAndStatus(userId, ApplicationStatus.REJECTED, pageable);
                break;
            case "completed":
                applications = applicationRepository.findByUserIdAndStatus(userId, ApplicationStatus.COMPLETED, pageable);
                break;
            default:
                // 전체 신청 목록
                applications = applicationRepository.findByUserId(userId, pageable);
        }

        // DTO 변환
        List<UserCampaignResponse> campaigns = applications.getContent().stream()
                .map(this::convertToUserCampaignResponse)
                .collect(Collectors.toList());

        return MyCampaignListResponse.builder()
                .role("USER")
                .category(category)
                .campaigns(campaigns)
                .pagination(PageResponse.PaginationInfo.builder()
                        .pageNumber(applications.getNumber())
                        .pageSize(applications.getSize())
                        .totalPages(applications.getTotalPages())
                        .totalElements(applications.getTotalElements())
                        .first(applications.isFirst())
                        .last(applications.isLast())
                        .build())
                .build();
    }

    /**
     * CLIENT 역할 - 캠페인 상세 목록 조회
     */
    private MyCampaignListResponse getClientCampaignList(Long userId, String category, Pageable pageable) {
        Page<Campaign> campaigns;

        // 카테고리에 따른 필터링
        switch (category) {
            case "pending":
                campaigns = campaignRepository.findByCreatorIdAndApprovalStatus(userId, Campaign.ApprovalStatus.PENDING, pageable);
                break;
            case "approved":
                campaigns = campaignRepository.findByCreatorIdAndApprovalStatus(userId, Campaign.ApprovalStatus.APPROVED, pageable);
                break;
            case "rejected":
                campaigns = campaignRepository.findByCreatorIdAndApprovalStatus(userId, Campaign.ApprovalStatus.REJECTED, pageable);
                break;
            case "expired":
                campaigns = campaignRepository.findExpiredByCreatorId(userId, LocalDate.now(), pageable);
                break;
            default:
                campaigns = campaignRepository.findByCreatorIdWithCompany(userId, pageable);
        }

        // DTO 변환
        List<ClientCampaignResponse> campaignList = campaigns.getContent().stream()
                .map(this::convertToClientCampaignResponse)
                .collect(Collectors.toList());

        return MyCampaignListResponse.builder()
                .role("CLIENT")
                .category(category)
                .campaigns(campaignList)
                .pagination(PageResponse.PaginationInfo.builder()
                        .pageNumber(campaigns.getNumber())
                        .pageSize(campaigns.getSize())
                        .totalPages(campaigns.getTotalPages())
                        .totalElements(campaigns.getTotalElements())
                        .first(campaigns.isFirst())
                        .last(campaigns.isLast())
                        .build())
                .build();
    }

    /**
     * CampaignApplication을 UserCampaignResponse로 변환
     */
    private UserCampaignResponse convertToUserCampaignResponse(CampaignApplication application) {
        Campaign campaign = application.getCampaign();
        LocalDate currentDate = LocalDate.now();

        // 신청 상태 계산 (상시 캠페인 고려)
        String displayStatus;
        if (application.getApplicationStatus() == ApplicationStatus.APPLIED) {
            if (campaign.getIsAlwaysOpen() != null && campaign.getIsAlwaysOpen()) {
                displayStatus = "APPLIED"; // 상시 캠페인은 항상 APPLIED 상태 유지
            } else if (currentDate.isAfter(campaign.getRecruitmentEndDate())) {
                displayStatus = "PENDING"; // 대기중 (모집 기간 종료)
            } else {
                displayStatus = "APPLIED"; // 지원 (모집 기간 중)
            }
        } else {
            displayStatus = application.getApplicationStatus().toString();
        }

        return UserCampaignResponse.builder()
                .applicationId(application.getId())
                .campaignId(campaign.getId())
                .title(campaign.getTitle())
                .applicationStatus(displayStatus)
                .campaign(UserCampaignResponse.CampaignDetail.builder()
                        .id(campaign.getId())
                        .title(campaign.getTitle())
                        .description(campaign.getProductDetails())
                        .imageUrl(campaign.getThumbnailUrl())
                        .applicationStartDate(campaign.getRecruitmentStartDate() != null ?
                                campaign.getRecruitmentStartDate().toString() : null)
                        .applicationEndDate(campaign.getIsAlwaysOpen() != null && campaign.getIsAlwaysOpen() ? 
                                null : // 상시 캠페인은 마감일 없음
                                (campaign.getRecruitmentEndDate() != null ? campaign.getRecruitmentEndDate().toString() : null))
                        .experienceStartDate(campaign.getSelectionDate() != null ?
                                campaign.getSelectionDate().toString() : null)
                        .experienceEndDate(campaign.getMissionInfo() != null && campaign.getMissionInfo().getMissionDeadlineDate() != null ?
                                campaign.getMissionInfo().getMissionDeadlineDate().toString() : null)
                        .recruitmentCount(campaign.getMaxApplicants())
                        .currentApplicationCount(getCurrentApplicationCount(campaign.getId()))
                        .category(campaign.getCategory() != null ? campaign.getCategory().getCategoryName() : null)
                        .type(campaign.getCampaignType())
                        .location(null) // Campaign 엔티티에 location 필드가 없음
                        .company(UserCampaignResponse.CompanyInfo.builder()
                                .id(campaign.getCompany().getId())
                                .name(campaign.getCompany().getCompanyName())
                                .businessNumber(campaign.getCompany().getBusinessRegistrationNumber())
                                .build())
                        .build())
                .build();
    }

    /**
     * Campaign을 ClientCampaignResponse로 변환
     */
    private ClientCampaignResponse convertToClientCampaignResponse(Campaign campaign) {
        // 캠페인 통계 정보 조회
        Object[] statistics = campaignRepository.getCampaignStatistics(campaign.getId());

        ClientCampaignResponse.CampaignStatistics stats = ClientCampaignResponse.CampaignStatistics.builder()
                .totalApplications(statistics[0] != null ? ((Number) statistics[0]).intValue() : 0)
                .selectedCount(statistics[1] != null ? ((Number) statistics[1]).intValue() : 0)
                .completedCount(statistics[2] != null ? ((Number) statistics[2]).intValue() : 0)
                .pendingApplications(statistics[3] != null ? ((Number) statistics[3]).intValue() : 0)
                .build();

        // 캠페인 상태 계산
        String campaignStatus = calculateCampaignStatus(campaign);

        return ClientCampaignResponse.builder()
                .campaignId(campaign.getId())
                .title(campaign.getTitle())
                .description(campaign.getProductDetails())
                .imageUrl(campaign.getThumbnailUrl())
                .approvalStatus(campaign.getApprovalStatus().toString())
                .campaignStatus(campaignStatus)
                .createdAt(campaign.getCreatedAt() != null ? campaign.getCreatedAt().toString() : null)
                .updatedAt(campaign.getUpdatedAt() != null ? campaign.getUpdatedAt().toString() : null)
                .applicationStartDate(campaign.getRecruitmentStartDate() != null ?
                        campaign.getRecruitmentStartDate().toString() : null)
                .applicationEndDate(campaign.getRecruitmentEndDate() != null ?
                        campaign.getRecruitmentEndDate().toString() : null)
                .experienceStartDate(campaign.getSelectionDate() != null ?
                        campaign.getSelectionDate().toString() : null)
                .experienceEndDate(campaign.getMissionInfo() != null && campaign.getMissionInfo().getMissionDeadlineDate() != null ?
                        campaign.getMissionInfo().getMissionDeadlineDate().toString() : null)
                .recruitmentCount(campaign.getMaxApplicants())
                .category(campaign.getCategory() != null ? campaign.getCategory().getCategoryName() : null)
                .type(campaign.getCampaignType())
                .location(null) // Campaign 엔티티에 location 필드가 없음
                .statistics(stats)
                .company(ClientCampaignResponse.CompanyInfo.builder()
                        .id(campaign.getCompany().getId())
                        .name(campaign.getCompany().getCompanyName())
                        .businessNumber(campaign.getCompany().getBusinessRegistrationNumber())
                        .build())
                .build();
    }

    /**
     * USER 역할 - 캠페인 신청 목록 조회 (기존 API 호환성을 위한 메서드)
     */
    public PageResponse<ApplicationResponse> getUserApplicationsCompat(Long userId, int page, int size, String applicationStatus) {
        LocalDate currentDate = LocalDate.now();
        Page<CampaignApplication> applications;

        // applicationStatus에 따른 필터링 (기존 API와 동일한 로직)
        if (applicationStatus != null && !applicationStatus.trim().isEmpty()) {
            String statusUpper = applicationStatus.toUpperCase();

            if ("APPLIED".equals(statusUpper)) {
                // 🔥 수정: APPLIED 상태 전체 조회 (LEFT JOIN으로 수정됨)
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                applications = applicationRepository.findAppliedApplicationsByUserId(userId, pageable);
                
                log.info("APPLIED 필터 결과 (전체): userId={}, totalElements={}", 
                        userId, applications.getTotalElements());
            } else if ("PENDING".equals(statusUpper)) {
                // 모집 기간 끝난 APPLIED 상태 신청  
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                applications = applicationRepository.findPendingByUserId(userId, currentDate, pageable);
            } else if ("REJECTED".equals(statusUpper)) {
                // REJECTED 상태 신청 조회
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
                applications = applicationRepository.findByUserIdAndStatus(userId, ApplicationStatus.REJECTED, pageable);
            } else {
                // 기존 로직: SELECTED, REJECTED, COMPLETED
                try {
                    ApplicationStatus status = ApplicationStatus.valueOf(statusUpper);
                    log.info("ApplicationStatus enum 변환 성공: {} -> {}", statusUpper, status);
                    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                    log.info("Repository 메서드 호출: findByUserIdAndStatus(userId={}, status={}, pageable={})", userId, status, pageable);
                    
                    // 🔥 디버깅: SELECTED 상태인 경우 추가 정보 확인
                    if ("SELECTED".equals(statusUpper)) {
                        log.info("=== SELECTED 상태 디버깅 시작 ===");
                        
                        // 1. 네이티브 쿼리로 원시 데이터 확인
                        List<Object[]> nativeResult = applicationRepository.findSelectedByUserIdNative(userId);
                        log.info("네이티브 쿼리 결과 수: {}", nativeResult.size());
                        for (Object[] row : nativeResult) {
                            log.info("Native result: ca_id={}, campaign_id={}, title={}, company_id={}", 
                                    row[0], row[1], row[2], row[3]);
                        }
                        
                        // 2. 간단한 JPQL 쿼리 시도
                        applications = applicationRepository.findByUserIdAndStatusSimple(userId, status, pageable);
                        log.info("Simple 쿼리 결과: totalElements={}, content.size={}", 
                                applications.getTotalElements(), applications.getContent().size());
                        
                        // 3. 간단한 쿼리에서 결과가 있으면 복잡한 쿼리도 시도
                        if (applications.getTotalElements() > 0) {
                            log.info("간단한 쿼리에서 결과 발견, 복잡한 쿼리도 시도");
                            Page<CampaignApplication> complexResult = applicationRepository.findByUserIdAndStatus(userId, status, pageable);
                            log.info("복잡한 쿼리 결과: totalElements={}, content.size={}", 
                                    complexResult.getTotalElements(), complexResult.getContent().size());
                        }
                        
                        log.info("=== SELECTED 상태 디버깅 종료 ===");
                    } else {
                        applications = applicationRepository.findByUserIdAndStatus(userId, status, pageable);
                    }
                    
                    log.info("Repository 메서드 결과: totalElements={}, content.size={}", 
                            applications.getTotalElements(), applications.getContent().size());
                } catch (IllegalArgumentException e) {
                    log.warn("잘못된 신청 상태 값: {}", applicationStatus);
                    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                    applications = Page.empty(pageable);
                }
            }
        } else {
            // 필터링 없이 모든 신청 조회
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            applications = applicationRepository.findByUserId(userId, pageable);
            
            log.info("전체 신청 조회 결과: userId={}, totalElements={}, content.size={}", 
                    userId, applications.getTotalElements(), applications.getContent().size());
        }

        // ApplicationResponse로 변환 (상시 캠페인을 고려한 모집 기간 기준으로 상태 구분)
        List<ApplicationResponse> content = applications.getContent().stream()
                .map(application -> {
                    Campaign campaign = application.getCampaign();

                    // 신청 상태 계산 (상시 캠페인 고려)
                    String displayStatus;
                    if (application.getApplicationStatus() == ApplicationStatus.APPLIED) {
                        if (campaign.getIsAlwaysOpen() != null && campaign.getIsAlwaysOpen()) {
                            displayStatus = "APPLIED"; // 상시 캠페인은 항상 APPLIED 상태 유지
                        } else if (currentDate.isAfter(campaign.getRecruitmentEndDate())) {
                            displayStatus = "PENDING"; // 대기중 (모집 기간 종료)
                        } else {
                            displayStatus = "APPLIED"; // 지원 (모집 기간 중)
                        }
                    } else if (application.getApplicationStatus() == ApplicationStatus.SELECTED) {
                        displayStatus = "SELECTED"; // 선정됨
                    } else if (application.getApplicationStatus() == ApplicationStatus.COMPLETED) {
                        displayStatus = "COMPLETED"; // 완료됨
                    } else if (application.isRejected()) {
                        displayStatus = "REJECTED"; // 반려됨
                    } else {
                        displayStatus = application.getApplicationStatus().toString(); // 기타
                    }

                    // ApplicationResponse 생성 시 계산된 상태 사용 (대문자 유지)
                    return ApplicationResponse.fromEntityWithCustomStatusUpperCase(application, displayStatus);
                })
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                applications.getNumber(),
                applications.getSize(),
                applications.getTotalPages(),
                applications.getTotalElements(),
                applications.isFirst(),
                applications.isLast()
        );
    }

    /**
     * 캠페인 상태 계산 (CLIENT용) - 상시 캠페인 고려
     */
    private String calculateCampaignStatus(Campaign campaign) {
        if (!Campaign.ApprovalStatus.APPROVED.equals(campaign.getApprovalStatus())) {
            return campaign.getApprovalStatus().name();
        }

        // 상시 캠페인인 경우 항상 ACTIVE 상태
        if (campaign.getIsAlwaysOpen() != null && campaign.getIsAlwaysOpen()) {
            return "ACTIVE"; // 상시 캠페인은 항상 활성
        }

        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = campaign.getRecruitmentStartDate();
        LocalDate deadlineDate = campaign.getRecruitmentEndDate();

        if (currentDate.isBefore(startDate)) {
            return "SCHEDULED"; // 시작 전
        } else if (currentDate.isAfter(deadlineDate)) {
            return "EXPIRED"; // 만료됨
        } else {
            return "ACTIVE"; // 진행중
        }
    }

    /**
     * 현재 신청자 수 조회
     */
    private Integer getCurrentApplicationCount(Long campaignId) {
        return campaignRepository.countCurrentApplicationsByCampaignId(campaignId);
    }
}
