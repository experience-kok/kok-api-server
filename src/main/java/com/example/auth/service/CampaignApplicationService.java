package com.example.auth.service;

import com.example.auth.constant.ApplicationStatus;
import com.example.auth.constant.UserRole;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignApplication;
import com.example.auth.domain.Notification;
import com.example.auth.domain.User;
import com.example.auth.domain.UserSnsPlatform;
import com.example.auth.dto.application.ApplicationResponse;
import com.example.auth.dto.application.CampaignApplicantResponse;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.exception.AccessDeniedException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignApplicationRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.repository.UserSnsPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 캠페인 신청 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignApplicationService {

    private final CampaignApplicationRepository applicationRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final UserSnsPlatformRepository userSnsPlatformRepository;
    private final NotificationService notificationService;

    /**
     * 캠페인 신청을 생성합니다.
     *
     * @param campaignId 신청할 캠페인 ID
     * @param userId     신청하는 사용자 ID
     * @return 생성된 신청 정보
     * @throws ResourceNotFoundException 캠페인이나 사용자를 찾을 수 없는 경우
     * @throws IllegalStateException     이미 신청한 경우, 모집 마감된 경우, 사용자 정보 부족
     * @throws AccessDeniedException     권한이 없는 경우 (USER 역할이 아닌 경우)
     */
    @Transactional
    public ApplicationResponse createApplication(Long campaignId, Long userId) {
        // 캠페인과 사용자 조회
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 사용자 권한 검증: USER(인플루언서)만 캠페인 신청 가능
        if (!UserRole.USER.getValue().equals(user.getRole())) {
            throw new AccessDeniedException("인플루언서만 캠페인에 신청할 수 있어요.");
        }

        // 이미 신청한 경우 체크
        if (applicationRepository.existsByUserAndCampaign(user, campaign)) {
            throw new IllegalStateException("이미 해당 캠페인에 신청하셨어요.");
        }

        // 신청 마감 체크 - applicationDeadlineDate 기준으로 수정
        if (LocalDate.now().isAfter(campaign.getRecruitmentEndDate())) {
            throw new IllegalStateException("신청이 마감된 캠페인이에요.");
        }

        // === 새로운 사용자 정보 검증 시작 ===
        
        // 1. 닉네임 확인
        if (user.getNickname() == null || user.getNickname().trim().isEmpty()) {
            throw new IllegalStateException("프로필을 설정해주세요. 캠페인 신청을 위해 프로필 정보가 필요해요.");
        }

        // 2. 나이 확인
        if (user.getAge() == null) {
            throw new IllegalStateException("프로필을 설정해주세요. 캠페인 신청을 위해 프로필 정보가 필요해요.");
        }

        // 3. 성별 확인
        if (user.getGender() == null || user.getGender() == com.example.auth.constant.Gender.UNKNOWN) {
            throw new IllegalStateException("프로필을 설정해주세요. 캠페인 신청을 위해 프로필 정보가 필요해요.");
        }

        // 4. 캠페인 타입에 맞는 SNS 연동 확인
        String campaignType = campaign.getCampaignType(); // "인스타그램", "유튜브", "블로그" 등
        List<UserSnsPlatform> userPlatforms = userSnsPlatformRepository.findByUserId(userId);
        
        if (userPlatforms.isEmpty()) {
            throw new IllegalStateException("캠페인 신청을 위해 SNS 계정 연동이 필요해요. 프로필에서 SNS 계정을 연동해주세요.");
        }

        // 캠페인 타입에 맞는 SNS 플랫폼이 연동되어 있는지 확인
        boolean hasMatchingPlatform = userPlatforms.stream()
                .anyMatch(platform -> isPlatformMatching(campaignType, platform.getPlatformType()));

        if (!hasMatchingPlatform) {
            String requiredPlatform = getRequiredPlatformName(campaignType);
            throw new IllegalStateException(String.format("이 캠페인은 %s 계정이 필요해요. 프로필에서 %s 계정을 연동해주세요.", requiredPlatform, requiredPlatform));
        }

        // === 사용자 정보 검증 종료 ===

        // 신청 생성
        CampaignApplication application = CampaignApplication.builder()
                .campaign(campaign)
                .user(user)
                .applicationStatus(ApplicationStatus.APPLIED)
                .build();

        CampaignApplication savedApplication = applicationRepository.save(application);
        log.info("캠페인 신청 생성 완료: userId={}, campaignId={}, applicationId={}, userInfo=[nickname:{}, age:{}, gender:{}]", 
                userId, campaignId, savedApplication.getId(), user.getNickname(), user.getAge(), user.getGender());

        // 신청 접수 알림 전송
        try {
            notificationService.sendCampaignApplicationReceivedNotification(userId, campaignId, campaign.getTitle());
        } catch (Exception e) {
            log.error("신청 접수 알림 전송 실패 (메인 로직은 계속 진행): userId={}, campaignId={}, error={}",
                    userId, campaignId, e.getMessage(), e);
        }

        return ApplicationResponse.fromEntity(savedApplication);
    }

    /**
     * 캠페인 타입과 사용자의 SNS 플랫폼이 매칭되는지 확인
     */
    private boolean isPlatformMatching(String campaignType, String userPlatformType) {
        if (campaignType == null || userPlatformType == null) {
            return false;
        }

        String normalizedCampaignType = campaignType.toLowerCase().trim();
        String normalizedUserPlatform = userPlatformType.toLowerCase().trim();

        return switch (normalizedCampaignType) {
            case "인스타그램", "instagram" -> "instagram".equals(normalizedUserPlatform);
            case "유튜브", "youtube" -> "youtube".equals(normalizedUserPlatform);
            case "블로그", "blog", "네이버블로그" -> "blog".equals(normalizedUserPlatform);
            case "페이스북", "facebook" -> "facebook".equals(normalizedUserPlatform);
            default -> false;
        };
    }

    /**
     * 캠페인 타입에 필요한 플랫폼 이름 반환
     */
    private String getRequiredPlatformName(String campaignType) {
        if (campaignType == null) {
            return "SNS";
        }

        String normalizedType = campaignType.toLowerCase().trim();
        return switch (normalizedType) {
            case "인스타그램", "instagram" -> "인스타그램";
            case "유튜브", "youtube" -> "유튜브";
            case "블로그", "blog", "네이버블로그" -> "블로그";
            case "페이스북", "facebook" -> "페이스북";
            default -> "SNS";
        };
    }

    /**
     * 특정 사용자의 모든 캠페인 신청 목록을 조회합니다. (기존 버전 - 호환성 유지)
     *
     * @param userId 사용자 ID
     * @param page   페이지 번호 (0부터 시작)
     * @param size   페이지 크기
     * @return 페이징된 신청 목록
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getUserApplications(Long userId, int page, int size) {
        return getUserApplications(userId, page, size, null);
    }

    /**
     * 특정 사용자의 모든 캠페인 신청 목록을 조회합니다.
     *
     * @param userId            사용자 ID
     * @param page              페이지 번호 (0부터 시작)
     * @param size              페이지 크기
     * @param applicationStatus 신청 상태 필터 (선택사항)
     * @return 페이징된 신청 목록
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getUserApplications(Long userId, int page, int size, String applicationStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CampaignApplication> applications;

        // applicationStatus 필터링
        if (applicationStatus != null && !applicationStatus.trim().isEmpty()) {
            try {
                ApplicationStatus status = ApplicationStatus.valueOf(applicationStatus.toUpperCase());
                applications = applicationRepository.findByUserAndApplicationStatus(user, status, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 신청 상태 값: {}", applicationStatus);
                // 잘못된 상태값인 경우 빈 결과 반환
                applications = Page.empty(pageable);
            }
        } else {
            // 필터링 없이 모든 신청 조회
            applications = applicationRepository.findByUser(user, pageable);
        }

        List<ApplicationResponse> content = applications.getContent().stream()
                .map(ApplicationResponse::fromEntity)
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
     * 특정 사용자의 모든 캠페인 신청 목록을 리스트로 조회합니다. (페이징 없음)
     *
     * @param userId            사용자 ID
     * @param applicationStatus 신청 상태 필터 (선택사항)
     * @return 신청 목록
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getUserApplicationsList(Long userId, String applicationStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        List<CampaignApplication> applications;

        // applicationStatus 필터링
        if (applicationStatus != null && !applicationStatus.trim().isEmpty()) {
            try {
                ApplicationStatus status = ApplicationStatus.valueOf(applicationStatus.toUpperCase());
                applications = applicationRepository.findByUserAndApplicationStatus(user, status);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 신청 상태 값: {}", applicationStatus);
                // 잘못된 상태값인 경우 빈 결과 반환
                return Collections.emptyList();
            }
        } else {
            // 필터링 없이 모든 신청 조회
            applications = applicationRepository.findByUser(user);
        }

        return applications.stream()
                .map(ApplicationResponse::fromEntity)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // 최신순 정렬
                .collect(Collectors.toList());
    }

    /**
     * CLIENT 역할 사용자가 자신이 만든 캠페인 목록을 리스트로 조회합니다. (페이징 없음)
     *
     * @param clientUserId      CLIENT 사용자 ID
     * @param applicationStatus 캠페인 승인 상태 필터 (선택사항)
     * @return 캠페인 목록 (ApplicationResponse 형태로 변환)
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getClientCampaignsList(Long clientUserId, String applicationStatus) {
        User clientUser = userRepository.findById(clientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + clientUserId));

        List<Campaign> campaigns;

        // applicationStatus 필터링 (CLIENT의 경우 캠페인 승인 상태로 필터링)
        if (applicationStatus != null && !applicationStatus.trim().isEmpty()) {
            String statusFilter = applicationStatus.toUpperCase();

            if ("EXPIRED".equals(statusFilter)) {
                // EXPIRED 상태: 승인됐지만 모집 마감일이 지난 캠페인
                campaigns = campaignRepository.findByCreatorAndApprovalStatusAndRecruitmentEndDateBefore(
                        clientUser,
                        Campaign.ApprovalStatus.APPROVED,
                        LocalDate.now()
                );
            } else {
                try {
                    Campaign.ApprovalStatus approvalStatus = Campaign.ApprovalStatus.valueOf(statusFilter);
                    if (approvalStatus == Campaign.ApprovalStatus.APPROVED) {
                        // APPROVED 상태: 승인되고 아직 모집 마감일이 지나지 않은 캠페인
                        campaigns = campaignRepository.findByCreatorAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
                                clientUser,
                                approvalStatus,
                                LocalDate.now()
                        );
                    } else {
                        // PENDING, REJECTED 상태
                        campaigns = campaignRepository.findByCreatorAndApprovalStatus(clientUser, approvalStatus);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("잘못된 캠페인 상태 값: {}", applicationStatus);
                    // 잘못된 상태값인 경우 빈 결과 반환
                    return Collections.emptyList();
                }
            }
        } else {
            // 필터링 없이 모든 캠페인 조회
            campaigns = campaignRepository.findByCreator(clientUser);
        }

        // Campaign을 ApplicationResponse 형태로 변환 (일관된 응답 구조를 위해)
        return campaigns.stream()
                .map(campaign -> ApplicationResponse.fromCampaign(campaign, clientUser))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // 최신순 정렬
                .collect(Collectors.toList());
    }

    /**
     * CLIENT 역할 사용자가 자신이 만든 캠페인 목록을 조회합니다. (기존 버전 - 호환성 유지)
     *
     * @param clientUserId CLIENT 사용자 ID
     * @param page         페이지 번호 (0부터 시작)
     * @param size         페이지 크기
     * @return 페이징된 캠페인 목록 (ApplicationResponse 형태로 변환)
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getClientCampaigns(Long clientUserId, int page, int size) {
        return getClientCampaigns(clientUserId, page, size, null);
    }

    /**
     * CLIENT 역할 사용자가 자신이 만든 캠페인 목록을 조회합니다.
     *
     * @param clientUserId      CLIENT 사용자 ID
     * @param page              페이지 번호 (0부터 시작)
     * @param size              페이지 크기
     * @param applicationStatus 캠페인 승인 상태 필터 (선택사항)
     * @return 페이징된 캠페인 목록 (ApplicationResponse 형태로 변환)
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getClientCampaigns(Long clientUserId, int page, int size, String applicationStatus) {
        User clientUser = userRepository.findById(clientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + clientUserId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Campaign> campaigns;

        // applicationStatus 필터링 (CLIENT의 경우 캠페인 승인 상태로 필터링)
        if (applicationStatus != null && !applicationStatus.trim().isEmpty()) {
            String statusFilter = applicationStatus.toUpperCase();

            if ("EXPIRED".equals(statusFilter)) {
                // EXPIRED 상태: 승인됐지만 모집 마감일이 지난 캠페인
                campaigns = campaignRepository.findByCreatorAndApprovalStatusAndRecruitmentEndDateBefore(
                        clientUser,
                        Campaign.ApprovalStatus.APPROVED,
                        LocalDate.now(),
                        pageable
                );
            } else {
                try {
                    Campaign.ApprovalStatus approvalStatus = Campaign.ApprovalStatus.valueOf(statusFilter);
                    if (approvalStatus == Campaign.ApprovalStatus.APPROVED) {
                        // APPROVED 상태: 승인되고 아직 모집 마감일이 지나지 않은 캠페인
                        campaigns = campaignRepository.findByCreatorAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
                                clientUser,
                                approvalStatus,
                                LocalDate.now(),
                                pageable
                        );
                    } else {
                        // PENDING, REJECTED 상태
                        campaigns = campaignRepository.findByCreatorAndApprovalStatus(clientUser, approvalStatus, pageable);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("잘못된 캠페인 상태 값: {}", applicationStatus);
                    // 잘못된 상태값인 경우 빈 결과 반환
                    campaigns = Page.empty(pageable);
                }
            }
        } else {
            // 필터링 없이 모든 캠페인 조회
            campaigns = campaignRepository.findByCreator(clientUser, pageable);
        }

        // Campaign을 ApplicationResponse 형태로 변환 (일관된 응답 구조를 위해)
        List<ApplicationResponse> content = campaigns.getContent().stream()
                .map(campaign -> ApplicationResponse.fromCampaign(campaign, clientUser))
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                campaigns.getNumber(),
                campaigns.getSize(),
                campaigns.getTotalPages(),
                campaigns.getTotalElements(),
                campaigns.isFirst(),
                campaigns.isLast()
        );
    }

    /**
     * 캠페인 신청을 취소합니다. (신청자 본인만 가능)
     *
     * @param applicationId 취소할 신청 ID
     * @param currentUserId 현재 로그인한 사용자 ID (권한 체크용)
     * @throws ResourceNotFoundException 신청 정보를 찾을 수 없는 경우
     * @throws AccessDeniedException     권한이 없는 경우
     * @throws IllegalStateException     이미 승인/거절된 신청인 경우
     */
    @Transactional
    public void cancelApplication(Long applicationId, Long currentUserId) {
        CampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("신청 정보를 찾을 수 없습니다. ID: " + applicationId));

        // 권한 체크: 본인만 취소 가능
        if (!application.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("본인의 신청만 취소할 수 있어요.");
        }

        // 대기 상태인 경우만 취소 가능
        if (application.getApplicationStatus() != ApplicationStatus.APPLIED) {
            throw new IllegalStateException("이미 처리된 신청은 취소할 수 없어요.");
        }

        applicationRepository.delete(application);
        log.info("캠페인 신청 취소 완료: applicationId={}, userId={}", applicationId, currentUserId);
    }

    /**
     * 특정 캠페인의 신청자 목록을 조회합니다. (CLIENT 전용)
     *
     * @param campaignId        캠페인 ID
     * @param clientUserId      CLIENT 사용자 ID (권한 확인용)
     * @param page              페이지 번호 (0부터 시작)
     * @param size              페이지 크기
     * @param applicationStatus 신청 상태 필터 (선택사항)
     * @return 페이징된 신청자 목록
     * @throws ResourceNotFoundException 캠페인이나 사용자를 찾을 수 없는 경우
     * @throws AccessDeniedException     권한이 없는 경우 (본인이 만든 캠페인이 아닌 경우)
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignApplicantResponse> getCampaignApplicants(Long campaignId, Long clientUserId, int page, int size, String applicationStatus) {
        // 캠페인 조회 및 소유자 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        User clientUser = userRepository.findById(clientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + clientUserId));

        // 권한 체크: 본인이 만든 캠페인인지 확인
        if (!campaign.getCreator().getId().equals(clientUserId)) {
            throw new AccessDeniedException("본인이 만든 캠페인의 신청자만 조회할 수 있어요.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CampaignApplication> applications;

        // applicationStatus 필터링
        if (applicationStatus != null && !applicationStatus.trim().isEmpty()) {
            try {
                ApplicationStatus status = ApplicationStatus.valueOf(applicationStatus.toUpperCase());
                applications = applicationRepository.findByCampaignAndApplicationStatus(campaign, status, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 신청 상태 값: {}", applicationStatus);
                // 잘못된 상태값인 경우 빈 결과 반환
                applications = Page.empty(pageable);
            }
        } else {
            // 필터링 없이 모든 신청 조회
            applications = applicationRepository.findByCampaign(campaign, pageable);
        }

        // ApplicationResponse를 CampaignApplicantResponse로 변환
        List<CampaignApplicantResponse> content = applications.getContent().stream()
                .map(application -> {
                    // 각 신청자의 SNS 플랫폼 정보 조회
                    List<UserSnsPlatform> snsPlatforms = userSnsPlatformRepository.findByUserId(application.getUser().getId());
                    return CampaignApplicantResponse.fromEntity(application, snsPlatforms);
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
     * 사용자가 특정 캠페인에 신청한 정보를 조회합니다.
     *
     * @param campaignId 캠페인 ID
     * @param userId     사용자 ID
     * @return 신청 정보 (없으면 null)
     * @throws ResourceNotFoundException 캠페인을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public ApplicationResponse getUserApplicationInfo(Long campaignId, Long userId) {
        // 캠페인 존재 여부 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다: " + campaignId));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        // 신청 정보 조회
        return applicationRepository.findByUserAndCampaign(user, campaign)
                .map(ApplicationResponse::fromEntity)
                .orElse(null);
    }

    /**
     * 사용자가 특정 캠페인에 이미 신청했는지 확인합니다.
     *
     * @param campaignId 캠페인 ID
     * @param userId     사용자 ID
     * @return 신청 여부
     */
    @Transactional(readOnly = true)
    public boolean hasUserApplied(Long campaignId, Long userId) {
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (campaign == null || user == null) {
            return false;
        }

        return applicationRepository.existsByUserAndCampaign(user, campaign);
    }

    /**
     * 캠페인 신청자를 선정합니다. (CLIENT 전용)
     *
     * @param campaignId           캠페인 ID
     * @param clientUserId         CLIENT 사용자 ID (권한 확인용)
     * @param selectedApplicationIds 선정할 신청자 ID 목록
     * @param selectionReason      선정 사유 (선택사항)
     * @param notifyUnselected     미선정자 알림 여부
     * @param messageToSelected    선정자 추가 메시지
     * @param messageToUnselected  미선정자 추가 메시지
     * @return 선정 결과 정보
     * @throws ResourceNotFoundException 캠페인이나 사용자를 찾을 수 없는 경우
     * @throws AccessDeniedException     권한이 없는 경우
     * @throws IllegalStateException     이미 선정이 완료된 경우
     */
    @Transactional
    public com.example.auth.dto.application.CampaignSelectionResponse selectCampaignApplicants(
            Long campaignId,
            Long clientUserId,
            List<Long> selectedApplicationIds,
            String selectionReason,
            Boolean notifyUnselected,
            String messageToSelected,
            String messageToUnselected) {

        log.info("캠페인 신청자 선정 시작: campaignId={}, clientUserId={}, selectedCount={}",
                campaignId, clientUserId, selectedApplicationIds.size());

        // 캠페인 조회 및 소유자 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        User clientUser = userRepository.findById(clientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + clientUserId));

        // 권한 체크: 본인이 만든 캠페인인지 확인
        if (!campaign.getCreator().getId().equals(clientUserId)) {
            throw new AccessDeniedException("본인이 만든 캠페인의 신청자만 선정할 수 있어요.");
        }

        // 이미 선정된 신청자가 있는지 확인
        List<CampaignApplication> existingSelectedApplications =
                applicationRepository.findByCampaignAndApplicationStatus(campaign, ApplicationStatus.SELECTED);

        if (!existingSelectedApplications.isEmpty()) {
            throw new IllegalStateException("이미 선정이 완료된 캠페인이에요. 기존 선정을 취소한 후 다시 선정해주세요.");
        }

        // 모든 신청자 조회 (APPLIED 상태만)
        List<CampaignApplication> allApplications =
                applicationRepository.findByCampaignAndApplicationStatus(campaign, ApplicationStatus.APPLIED);

        if (allApplications.isEmpty()) {
            throw new IllegalStateException("신청자가 없는 캠페인이에요.");
        }

        // 선정할 신청자들 필터링
        List<CampaignApplication> applicationsToSelect = allApplications.stream()
                .filter(app -> selectedApplicationIds.contains(app.getId()))
                .collect(Collectors.toList());

        if (applicationsToSelect.size() != selectedApplicationIds.size()) {
            throw new IllegalArgumentException("일부 신청을 찾을 수 없어요. 신청 ID를 확인해주세요.");
        }

        // 선정 처리
        applicationsToSelect.forEach(application -> {
            application.select(); // ApplicationStatus.SELECTED로 변경
        });

        // 일괄 저장
        List<CampaignApplication> selectedApplications = applicationRepository.saveAll(applicationsToSelect);

        // 미선정자 목록 (APPLIED 상태 그대로 유지)
        List<CampaignApplication> unselectedApplications = allApplications.stream()
                .filter(app -> !selectedApplicationIds.contains(app.getId()))
                .collect(Collectors.toList());

        log.info("캠페인 신청자 선정 완료: campaignId={}, selectedCount={}, unselectedCount={}",
                campaignId, selectedApplications.size(), unselectedApplications.size());

        // 선정자들에게 알림 전송
        selectedApplications.forEach(application -> {
            try {
                notificationService.sendCampaignSelectionNotification(
                        application.getUser().getId(),
                        campaignId,
                        campaign.getTitle(),
                        messageToSelected
                );
            } catch (Exception e) {
                log.error("선정 알림 전송 실패: userId={}, campaignId={}, error={}",
                        application.getUser().getId(), campaignId, e.getMessage(), e);
            }
        });

        // 미선정자들에게 알림 전송 (항상 전송)
        unselectedApplications.forEach(application -> {
            try {
                notificationService.sendCampaignNotSelectedNotification(
                        application.getUser().getId(),
                        campaignId,
                        campaign.getTitle(),
                        messageToUnselected
                );
            } catch (Exception e) {
                log.error("미선정 알림 전송 실패: userId={}, campaignId={}, error={}",
                        application.getUser().getId(), campaignId, e.getMessage(), e);
            }
        });

        // 응답 데이터 생성
        List<com.example.auth.dto.application.CampaignSelectionResponse.SelectedApplicantInfo> selectedApplicantInfos =
                selectedApplications.stream()
                        .map(app -> com.example.auth.dto.application.CampaignSelectionResponse.SelectedApplicantInfo.builder()
                                .applicationId(app.getId())
                                .userId(app.getUser().getId())
                                .nickname(app.getUser().getNickname())
                                .email(app.getUser().getEmail())
                                .selectedAt(app.getUpdatedAt())
                                .build())
                        .collect(Collectors.toList());

        return com.example.auth.dto.application.CampaignSelectionResponse.builder()
                .campaignId(campaignId)
                .campaignTitle(campaign.getTitle())
                .selectedApplicants(selectedApplicantInfos)
                .selectionProcessedAt(ZonedDateTime.now())
                .notificationSent(true)
                .build();
    }

    /**
     * 캠페인 선정을 취소합니다. (CLIENT 전용)
     *
     * @param campaignId   캠페인 ID
     * @param clientUserId CLIENT 사용자 ID (권한 확인용)
     * @throws ResourceNotFoundException 캠페인이나 사용자를 찾을 수 없는 경우
     * @throws AccessDeniedException     권한이 없는 경우
     * @throws IllegalStateException     선정된 신청자가 없는 경우
     */
    @Transactional
    public void cancelCampaignSelection(Long campaignId, Long clientUserId) {
        log.info("캠페인 선정 취소 시작: campaignId={}, clientUserId={}", campaignId, clientUserId);

        // 캠페인 조회 및 소유자 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));

        User clientUser = userRepository.findById(clientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + clientUserId));

        // 권한 체크: 본인이 만든 캠페인인지 확인
        if (!campaign.getCreator().getId().equals(clientUserId)) {
            throw new AccessDeniedException("본인이 만든 캠페인의 선정만 취소할 수 있어요.");
        }

        // 선정된 신청자들 조회
        List<CampaignApplication> selectedApplications =
                applicationRepository.findByCampaignAndApplicationStatus(campaign, ApplicationStatus.SELECTED);

        if (selectedApplications.isEmpty()) {
            throw new IllegalStateException("선정된 신청자가 없는 캠페인이에요.");
        }

        // 선정 취소 (APPLIED 상태로 되돌림)
        selectedApplications.forEach(application -> {
            application.updateStatus(ApplicationStatus.APPLIED);
        });

        applicationRepository.saveAll(selectedApplications);

        log.info("캠페인 선정 취소 완료: campaignId={}, canceledCount={}", campaignId, selectedApplications.size());

        // 선정 취소 알림 전송 (선택사항)
        selectedApplications.forEach(application -> {
            try {
                notificationService.sendNotification(
                        application.getUser().getId(),
                        Notification.NotificationType.SYSTEM_NOTICE,
                        "캠페인 선정이 취소되었습니다",
                        String.format("'%s' 캠페인의 선정이 취소되었어요. 새로운 선정 결과를 기다려주세요.", campaign.getTitle()),
                        campaignId,
                        "CAMPAIGN"
                );
            } catch (Exception e) {
                log.error("선정 취소 알림 전송 실패: userId={}, campaignId={}, error={}",
                        application.getUser().getId(), campaignId, e.getMessage(), e);
            }
        });
    }
}