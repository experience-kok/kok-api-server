package com.example.auth.service;

import com.example.auth.constant.ApplicationStatus;
import com.example.auth.domain.*;
import com.example.auth.dto.mission.*;
import com.example.auth.exception.BusinessException;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 미션 관리 서비스
 * 인플루언서 선정, 미션 제출, 검토, 포트폴리오 관리 등의 기능을 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionManagementService {

    private final CampaignApplicationRepository campaignApplicationRepository;
    private final MissionSubmissionRepository missionSubmissionRepository;
    private final UserMissionHistoryRepository userMissionHistoryRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 1-2. 인플루언서 다중 선정 (PENDING → SELECTED)
     */
    @Transactional
    public MultipleSelectionResponse selectMultipleInfluencers(Long campaignId, List<Long> applicationIds, Long clientId) {
        // 캠페인 검증 및 권한 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없어요."));

        if (!campaign.getCreator().getId().equals(clientId)) {
            throw new BusinessException("캠페인 생성자만 인플루언서를 선정할 수 있어요.");
        }

        List<Long> successfulSelections = new java.util.ArrayList<>();
        List<MultipleSelectionResponse.SelectionFailure> failedSelections = new java.util.ArrayList<>();

        for (Long applicationId : applicationIds) {
            try {
                // 개별 신청 검증 및 선정 처리
                CampaignApplication application = campaignApplicationRepository.findById(applicationId)
                        .orElseThrow(() -> new ResourceNotFoundException("캠페인 신청을 찾을 수 없어요. ID: " + applicationId));

                // 캠페인 일치 검증
                if (!application.getCampaign().getId().equals(campaignId)) {
                    throw new BusinessException("해당 캠페인의 신청이 아니에요.");
                }

                // 상태 검증
                if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
                    throw new BusinessException("선정 대기 상태의 신청만 선정할 수 있어요.");
                }

                // 상태 변경: PENDING → SELECTED
                application.updateStatus(ApplicationStatus.SELECTED);
                campaignApplicationRepository.save(application);

                // 선정 알림 발송
                notificationService.sendInfluencerSelectedNotification(
                    application.getUser().getId(), 
                    application.getCampaign().getTitle()
                );

                successfulSelections.add(applicationId);

                log.info("인플루언서 선정 성공 - applicationId: {}, userId: {}, campaignTitle: {}", 
                        applicationId, application.getUser().getId(), campaign.getTitle());

            } catch (Exception e) {
                // 개별 실패 처리
                MultipleSelectionResponse.SelectionFailure failure = MultipleSelectionResponse.SelectionFailure.builder()
                        .applicationId(applicationId)
                        .reason(e.getMessage())
                        .build();
                
                failedSelections.add(failure);

                log.warn("인플루언서 선정 실패 - applicationId: {}, 사유: {}", applicationId, e.getMessage());
            }
        }

        int successCount = successfulSelections.size();
        int failCount = failedSelections.size();

        MultipleSelectionResponse response = MultipleSelectionResponse.builder()
                .totalRequested(applicationIds.size())
                .successCount(successCount)
                .failCount(failCount)
                .successfulSelections(successfulSelections)
                .failedSelections(failedSelections)
                .build();

        log.info("인플루언서 다중 선정 완료 - campaignId: {}, 총 요청: {}, 성공: {}, 실패: {}", 
                campaignId, applicationIds.size(), successCount, failCount);

        return response;
    }

    /**
     * 2. 미션 제출 (MissionSubmission 생성, ApplicationStatus는 SELECTED 유지)
     */
    @Transactional
    public MissionSubmissionResponse submitMission(Long applicationId, MissionSubmissionRequest request, Long userId) {
        // 권한 검증 및 상태 확인 (SELECTED 상태여야 함)
        CampaignApplication application = validateSelectedApplication(applicationId, userId);
        
        // 미션 제출 엔티티 생성
        MissionSubmission submission = MissionSubmission.builder()
            .campaignApplication(application)
            .submissionUrl(request.getSubmissionUrl())
            .submissionTitle(request.getSubmissionTitle())
            .submissionDescription(request.getSubmissionDescription())
            .platformType(request.getPlatformType())
            .reviewStatus(MissionSubmission.ReviewStatus.PENDING)
            .build();
        
        submission = missionSubmissionRepository.save(submission);
        
        // ApplicationStatus는 SELECTED 상태 유지 (변경하지 않음)
        
        log.info("미션 제출 완료 - submissionId: {}", submission.getId());
        return MissionSubmissionResponse.fromEntity(submission);
    }

    /**
     * 선정된 신청 검증 헬퍼 메소드
     */
    private CampaignApplication validateSelectedApplication(Long applicationId, Long userId) {
        // 신청 검증
        CampaignApplication application = campaignApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인 신청을 찾을 수 없어요."));

        // 권한 검증 (신청자 본인만 제출 가능)
        if (!application.getUser().getId().equals(userId)) {
            throw new BusinessException("본인의 신청에만 미션을 제출할 수 있어요.");
        }

        // 상태 검증 (선정된 신청만 미션 제출 가능)
        if (application.getApplicationStatus() != ApplicationStatus.SELECTED) {
            throw new BusinessException("선정된 신청에만 미션을 제출할 수 있어요.");
        }

        // 이미 제출된 미션이 있는지 확인
        if (missionSubmissionRepository.existsByCampaignApplication(application)) {
            throw new BusinessException("이미 미션을 제출했어요.");
        }

        return application;
    }

    /**
     * 3. 미션 검토
     */
    @Transactional
    public void reviewMissionSubmission(Long submissionId, MissionReviewRequest request, Long clientId) {
        MissionSubmission submission = validateSubmissionForReview(submissionId, clientId);
        CampaignApplication application = submission.getCampaignApplication();
        
        if (request.getReviewStatus() == MissionSubmission.ReviewStatus.APPROVED) {
            // 미션 승인 처리
            submission.approve(request.getClientFeedback());
            
            // ApplicationStatus: SELECTED → COMPLETED
            application.updateStatus(ApplicationStatus.COMPLETED);
            
            // 포트폴리오에 추가
            createMissionHistory(submission, request.getClientRating(), request.getClientFeedback());
            
        } else if (request.getReviewStatus() == MissionSubmission.ReviewStatus.REVISION_REQUESTED) {
            // 수정 요청 처리
            submission.requestRevision(request.getClientFeedback());
            
            // ApplicationStatus는 SELECTED 상태 유지 (재제출 가능하도록)
        }
        
        missionSubmissionRepository.save(submission);
        campaignApplicationRepository.save(application);
        
        log.info("미션 검토 완료 - submissionId: {}, status: {}", submissionId, request.getReviewStatus());
    }

    /**
     * 미션 제출 검토 검증 헬퍼 메소드
     */
    private MissionSubmission validateSubmissionForReview(Long submissionId, Long clientId) {
        // 미션 제출 검증
        MissionSubmission submission = missionSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("미션 제출을 찾을 수 없어요."));

        // 권한 검증 (캠페인 생성자만 검토 가능)
        if (!submission.getCampaignApplication().getCampaign().getCreator().getId().equals(clientId)) {
            throw new BusinessException("캠페인 관리자만 미션을 검토할 수 있어요.");
        }

        // 상태 검증 (검토 대기 상태만 검토 가능)
        if (submission.getReviewStatus() != MissionSubmission.ReviewStatus.PENDING) {
            throw new BusinessException("검토 대기 상태의 미션만 검토할 수 있어요.");
        }

        return submission;
    }

    /**
     * 4. 포트폴리오 이력 생성 (미션 승인 시 포트폴리오에 추가)
     */
    private void createMissionHistory(MissionSubmission submission, Integer rating, String review) {
        Campaign campaign = submission.getCampaignApplication().getCampaign();
        User user = submission.getCampaignApplication().getUser();
        
        UserMissionHistory history = UserMissionHistory.builder()
            .user(user)
            .campaign(campaign)
            .campaignTitle(campaign.getTitle())
            .campaignCategory(campaign.getCategory().getCategoryName())
            .platformType(submission.getPlatformType())
            .submissionUrl(submission.getSubmissionUrl())
            .completionDate(ZonedDateTime.now())
            .clientRating(rating)
            .clientReview(review)
            .isPublic(true)
            .isFeatured(false)
            .build();
        
        userMissionHistoryRepository.save(history);
        log.info("미션 이력 생성 완료 - userId: {}, campaignId: {}", user.getId(), campaign.getId());
    }

    /**
     * 5. 캠페인별 미션 제출 목록 조회 (클라이언트용)
     */
    @Transactional(readOnly = true)
    public List<MissionSubmissionResponse> getMissionSubmissionsByCampaign(Long campaignId, Long clientId) {
        // 권한 검증
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없어요."));

        if (!campaign.getCreator().getId().equals(clientId)) {
            throw new BusinessException("캠페인 생성자만 미션 제출 목록을 조회할 수 있어요.");
        }

        List<MissionSubmission> submissions = missionSubmissionRepository.findByCampaignId(campaignId);
        return submissions.stream()
                .map(MissionSubmissionResponse::fromEntity)
                .collect(Collectors.toList());
    }


    /**
     * 7. 내 미션 이력 조회 (본인만 볼 수 있는 전체 이력)
     */
    @Transactional(readOnly = true)
    public List<UserMissionHistoryResponse> getMyMissionHistory(Long userId) {
        List<UserMissionHistory> histories = userMissionHistoryRepository.findByUserIdOrderByCompletionDateDesc(userId);
        return histories.stream()
                .map(UserMissionHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }


    /**
     * 10. 캠페인 미션 통계 조회
     */
    @Transactional(readOnly = true)
    public CampaignMissionStatisticsResponse getCampaignMissionStatistics(Long campaignId, Long clientId) {
        // 권한 검증
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없어요."));

        if (!campaign.getCreator().getId().equals(clientId)) {
            throw new BusinessException("캠페인 생성자만 통계를 조회할 수 있어요.");
        }

        // 미션 제출 통계
        List<MissionSubmission> submissions = missionSubmissionRepository.findByCampaignId(campaignId);
        
        long totalSubmissions = submissions.size();
        long approvedSubmissions = submissions.stream()
                .mapToLong(s -> s.getReviewStatus() == MissionSubmission.ReviewStatus.APPROVED ? 1 : 0)
                .sum();
        long pendingSubmissions = submissions.stream()
                .mapToLong(s -> s.getReviewStatus() == MissionSubmission.ReviewStatus.PENDING ? 1 : 0)
                .sum();
        long revisionRequested = submissions.stream()
                .mapToLong(s -> s.getReviewStatus() == MissionSubmission.ReviewStatus.REVISION_REQUESTED ? 1 : 0)
                .sum();

        // 평균 평점 계산 (승인된 미션 중)
        Double averageRating = userMissionHistoryRepository.findAverageRatingByCampaignId(campaignId);

        return CampaignMissionStatisticsResponse.builder()
                .campaignId(campaignId)
                .campaignTitle(campaign.getTitle())
                .totalSubmissions(totalSubmissions)
                .approvedSubmissions(approvedSubmissions)
                .pendingSubmissions(pendingSubmissions)
                .revisionRequested(revisionRequested)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .build();
    }

    /**
     * 11. 내 미션 통계 조회 (인플루언서용)
     */
    @Transactional(readOnly = true)
    public UserMissionStatisticsResponse getMyMissionStatistics(Long userId) {
        // 전체 완료 미션 수
        long totalCompletedMissions = userMissionHistoryRepository.countByUserId(userId);
        
        // 평균 평점
        Double averageRating = userMissionHistoryRepository.findAverageRatingByUserId(userId);
        
        // 카테고리별 미션 수
        List<Object[]> categoryStats = userMissionHistoryRepository.findMissionCountByCategory(userId);
        
        // 플랫폼별 미션 수
        List<Object[]> platformStats = userMissionHistoryRepository.findMissionCountByPlatform(userId);
        
        // 공개 미션 수
        long publicMissions = userMissionHistoryRepository.countByUserIdAndIsPublic(userId, true);

        return UserMissionStatisticsResponse.builder()
                .userId(userId)
                .totalCompletedMissions(totalCompletedMissions)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .publicMissions(publicMissions)
                .categoryStats(categoryStats)
                .platformStats(platformStats)
                .build();
    }
}
