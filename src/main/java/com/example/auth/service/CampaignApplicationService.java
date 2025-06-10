package com.example.auth.service;

import com.example.auth.constant.ApplicationStatus;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignApplication;
import com.example.auth.domain.User;
import com.example.auth.dto.application.ApplicationResponse;
import com.example.auth.dto.common.PageResponse;
import com.example.auth.exception.AccessDeniedException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.CampaignApplicationRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    /**
     * 캠페인 신청을 생성합니다.
     * @param campaignId 신청할 캠페인 ID
     * @param userId 신청하는 사용자 ID
     * @return 생성된 신청 정보
     * @throws ResourceNotFoundException 캠페인이나 사용자를 찾을 수 없는 경우
     * @throws IllegalStateException 이미 신청한 경우, 모집 마감된 경우
     */
    @Transactional
    public ApplicationResponse createApplication(Long campaignId, Long userId) {
        // 캠페인과 사용자 조회
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없습니다. ID: " + campaignId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 이미 신청한 경우 체크
        if (applicationRepository.existsByUserAndCampaign(user, campaign)) {
            throw new IllegalStateException("이미 해당 캠페인에 신청하셨습니다.");
        }
        
        // 모집 마감 체크
        if (LocalDate.now().isAfter(campaign.getRecruitmentEndDate())) {
            throw new IllegalStateException("모집이 마감된 캠페인입니다.");
        }
        
        // 최대 인원 체크는 제거 - 최대 인원에 상관없이 신청 가능
        
        // 신청 생성
        CampaignApplication application = CampaignApplication.builder()
                .campaign(campaign)
                .user(user)
                .applicationStatus(ApplicationStatus.PENDING)
                .build();
        
        CampaignApplication savedApplication = applicationRepository.save(application);
        log.info("캠페인 신청 생성 완료: userId={}, campaignId={}, applicationId={}", userId, campaignId, savedApplication.getId());
        
        return ApplicationResponse.fromEntity(savedApplication);
    }

    /**
     * 특정 사용자의 모든 캠페인 신청 목록을 조회합니다. (기존 버전 - 호환성 유지)
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 신청 목록
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getUserApplications(Long userId, int page, int size) {
        return getUserApplications(userId, page, size, null);
    }

    /**
     * 특정 사용자의 모든 캠페인 신청 목록을 조회합니다.
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
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
     * @param userId 사용자 ID
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
     * @param clientUserId CLIENT 사용자 ID
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
                // EXPIRED 상태: 승인됐지만 모집기간이 끝난 캠페인
                campaigns = campaignRepository.findByCreatorAndApprovalStatusAndRecruitmentEndDateBefore(
                    clientUser, 
                    Campaign.ApprovalStatus.APPROVED, 
                    LocalDate.now()
                );
            } else {
                try {
                    Campaign.ApprovalStatus approvalStatus = Campaign.ApprovalStatus.valueOf(statusFilter);
                    if (approvalStatus == Campaign.ApprovalStatus.APPROVED) {
                        // APPROVED 상태: 승인되고 아직 모집기간이 끝나지 않은 캠페인
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
     * @param clientUserId CLIENT 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 캠페인 목록 (ApplicationResponse 형태로 변환)
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getClientCampaigns(Long clientUserId, int page, int size) {
        return getClientCampaigns(clientUserId, page, size, null);
    }

    /**
     * CLIENT 역할 사용자가 자신이 만든 캠페인 목록을 조회합니다.
     * @param clientUserId CLIENT 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
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
                // EXPIRED 상태: 승인됐지만 모집기간이 끝난 캠페인
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
                        // APPROVED 상태: 승인되고 아직 모집기간이 끝나지 않은 캠페인
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
     * @param applicationId 취소할 신청 ID
     * @param currentUserId 현재 로그인한 사용자 ID (권한 체크용)
     * @throws ResourceNotFoundException 신청 정보를 찾을 수 없는 경우
     * @throws AccessDeniedException 권한이 없는 경우
     * @throws IllegalStateException 이미 승인/거절된 신청인 경우
     */
    @Transactional
    public void cancelApplication(Long applicationId, Long currentUserId) {
        CampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("신청 정보를 찾을 수 없습니다. ID: " + applicationId));
        
        // 권한 체크: 본인만 취소 가능
        if (!application.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("본인의 신청만 취소할 수 있습니다.");
        }
        
        // 대기 상태인 경우만 취소 가능
        if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 신청은 취소할 수 없습니다.");
        }
        
        applicationRepository.delete(application);
        log.info("캠페인 신청 취소 완료: applicationId={}, userId={}", applicationId, currentUserId);
    }

    /**
     * 사용자가 특정 캠페인에 이미 신청했는지 확인합니다.
     * @param campaignId 캠페인 ID
     * @param userId 사용자 ID
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
     * 사용자가 특정 캠페인에 신청한 정보를 조회합니다.
     * @param campaignId 캠페인 ID
     * @param userId 사용자 ID
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
}