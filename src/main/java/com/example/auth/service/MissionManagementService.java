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
    private final SESService sesService;

    /**
     * 1-1. 인플루언서 다중 선정 (PENDING → SELECTED)
     */
    @Transactional
    public MultipleSelectionResponse selectMultipleInfluencers(Long campaignId, List<Long> applicationIds, Long clientId) {
        log.info("인플루언서 선정 처리 시작: campaignId={}, clientUserId={}, count={}",
                campaignId, clientId, applicationIds.size());

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

                // 상태 검증 (PENDING 상태만 선정 가능)
                if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
                    throw new BusinessException("선정 대기 상태의 신청만 선정할 수 있어요.");
                }

                // 상태 변경: PENDING → SELECTED
                application.select();
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
     * 1-2. 인플루언서 다중 반려 (PENDING/SELECTED → REJECTED)
     */
    @Transactional
    public MultipleSelectionResponse rejectMultipleInfluencers(Long campaignId, List<Long> applicationIds, Long clientId) {
        log.info("인플루언서 반려 처리 시작: campaignId={}, clientUserId={}, count={}",
                campaignId, clientId, applicationIds.size());

        // 캠페인 검증 및 권한 확인
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없어요."));

        if (!campaign.getCreator().getId().equals(clientId)) {
            throw new BusinessException("캠페인 생성자만 인플루언서를 반려할 수 있어요.");
        }

        List<Long> successfulRejections = new java.util.ArrayList<>();
        List<MultipleSelectionResponse.SelectionFailure> failedRejections = new java.util.ArrayList<>();

        for (Long applicationId : applicationIds) {
            try {
                // 개별 신청 검증 및 반려 처리
                CampaignApplication application = campaignApplicationRepository.findById(applicationId)
                        .orElseThrow(() -> new ResourceNotFoundException("캠페인 신청을 찾을 수 없어요. ID: " + applicationId));

                // 캠페인 일치 검증
                if (!application.getCampaign().getId().equals(campaignId)) {
                    throw new BusinessException("해당 캠페인의 신청이 아니에요.");
                }

                // 상태 검증 (PENDING, SELECTED 상태만 반려 가능)
                ApplicationStatus currentStatus = application.getApplicationStatus();
                if (currentStatus != ApplicationStatus.PENDING && currentStatus != ApplicationStatus.SELECTED) {
                    throw new BusinessException("선정 대기 또는 선정된 상태의 신청만 반려할 수 있어요.");
                }

                // 상태 변경: → REJECTED
                application.reject();  // ApplicationStatus.REJECTED로 변경
                campaignApplicationRepository.save(application);

                // 반려 알림 발송
                notificationService.sendInfluencerRejectedNotification(
                        application.getUser().getId(),
                        campaign.getTitle()
                );

                successfulRejections.add(applicationId);

                log.info("인플루언서 반려 성공 - applicationId: {}, userId: {}, campaignTitle: {}",
                        applicationId, application.getUser().getId(), campaign.getTitle());

            } catch (Exception e) {
                // 개별 실패 처리
                MultipleSelectionResponse.SelectionFailure failure = MultipleSelectionResponse.SelectionFailure.builder()
                        .applicationId(applicationId)
                        .reason(e.getMessage())
                        .build();

                failedRejections.add(failure);

                log.warn("인플루언서 반려 실패 - applicationId: {}, 사유: {}", applicationId, e.getMessage());
            }
        }

        int successCount = successfulRejections.size();
        int failCount = failedRejections.size();

        MultipleSelectionResponse response = MultipleSelectionResponse.builder()
                .totalRequested(applicationIds.size())
                .successCount(successCount)
                .failCount(failCount)
                .successfulSelections(successfulRejections)
                .failedSelections(failedRejections)
                .build();

        log.info("인플루언서 다중 반려 완료 - campaignId: {}, 총 요청: {}, 성공: {}, 실패: {}",
                campaignId, applicationIds.size(), successCount, failCount);

        return response;
    }

    /**
     * 미선정 신청자들 정리 (알림 발송 + REJECTED 상태로 변경)
     */
    @Transactional
    private void cleanupUnselectedApplications(Long campaignId, List<Long> selectedApplicationIds) {
        // 해당 캠페인 조회
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("캠페인을 찾을 수 없어요."));

        // 해당 캠페인의 PENDING 상태인 신청들 조회
        List<CampaignApplication> remainingApplications = campaignApplicationRepository
                .findByCampaignAndApplicationStatus(campaign, ApplicationStatus.PENDING);

        // 선정된 신청 ID들을 제외한 나머지 신청자들
        List<CampaignApplication> unselectedApplications = remainingApplications.stream()
                .filter(app -> !selectedApplicationIds.contains(app.getId()))
                .collect(Collectors.toList());

        if (unselectedApplications.isEmpty()) {
            log.info("처리할 미선정 신청이 없습니다 - campaignId: {}", campaignId);
            return;
        }

        // 1. 미선정 알림 발송
        for (CampaignApplication application : unselectedApplications) {
            try {
                notificationService.sendInfluencerRejectedNotification(
                        application.getUser().getId(),
                        campaign.getTitle()
                );

                log.info("미선정 알림 발송 - applicationId: {}, userId: {}, campaignTitle: {}",
                        application.getId(), application.getUser().getId(), campaign.getTitle());

            } catch (Exception e) {
                log.warn("미선정 알림 발송 실패 - applicationId: {}, 사유: {}", application.getId(), e.getMessage());
                // 개별 실패는 전체 프로세스에 영향주지 않도록 로그만 기록
            }
        }

        // 2. 미선정 신청을 REJECTED 상태로 변경 (데이터 삭제 대신)
        try {
            unselectedApplications.forEach(application -> {
                application.reject(); // ApplicationStatus.REJECTED로 변경
            });
            campaignApplicationRepository.saveAll(unselectedApplications);

            log.info("미선정 신청 상태 변경 완료 - campaignId: {}, REJECTED로 변경된 신청 수: {}",
                    campaignId, unselectedApplications.size());
        } catch (Exception e) {
            log.error("미선정 신청 상태 변경 실패 - campaignId: {}, 사유: {}", campaignId, e.getMessage());
            // 상태 변경 실패 시에도 전체 프로세스는 계속 진행 (알림은 이미 발송됨)
        }

        // 3. 선정된 참가자들에게 선정 확정 알림 발송 (기존 알림 메서드 사용)
        List<CampaignApplication> selectedApplications = campaignApplicationRepository.findAllById(selectedApplicationIds);
        for (CampaignApplication selectedApp : selectedApplications) {
            try {
                // 기존 선정 알림 메서드 재사용 (선정 확정 의미)
                notificationService.sendInfluencerSelectedNotification(
                        selectedApp.getUser().getId(),
                        campaign.getTitle()
                );
                log.info("선정 확정 알림 발송 - applicationId: {}, userId: {}",
                        selectedApp.getId(), selectedApp.getUser().getId());
            } catch (Exception e) {
                log.warn("선정 확정 알림 발송 실패 - applicationId: {}, 사유: {}", selectedApp.getId(), e.getMessage());
            }
        }

        log.info("선정 완료 및 정리 작업 완료 - campaignId: {}, 선정: {}명, 거절: {}명",
                campaignId, selectedApplicationIds.size(), unselectedApplications.size());
    }

    /**
     * 2. 미션 제출 (MissionSubmission 생성 또는 업데이트, ApplicationStatus는 SELECTED 유지)
     */
    @Transactional
    public InfluencerMissionSubmissionResponse submitMission(Long applicationId, MissionSubmissionRequest request, Long userId) {
        // 권한 검증 및 상태 확인 (SELECTED 상태여야 함)
        CampaignApplication application = validateSelectedApplication(applicationId, userId);

        // 기존 미션 제출이 있는지 확인
        MissionSubmission existingSubmission = missionSubmissionRepository.findByCampaignApplication(application)
                .orElse(null);

        if (existingSubmission != null) {
            // 기존 제출이 있는 경우 - 완료되지 않은 상태에서만 재제출 가능
            if (!existingSubmission.getIsCompleted()) {
                // 미완료 상태 → 재제출 (기존 미션 업데이트)
                existingSubmission.resubmit(request.getMissionUrl());
                existingSubmission.setPlatformType(detectPlatformType(request.getMissionUrl()));
                
                // 수정 요청된 상태에서 재제출하는 경우, 가장 최근 revision을 완료 처리
                if (existingSubmission.isRevisionRequested() && !existingSubmission.getRevisions().isEmpty()) {
                    MissionRevision latestRevision = existingSubmission.getRevisions().stream()
                            .sorted((r1, r2) -> r2.getRequestedAt().compareTo(r1.getRequestedAt()))
                            .findFirst()
                            .orElse(null);
                    
                    if (latestRevision != null && !latestRevision.isCompleted()) {
                        latestRevision.completeRevision(request.getMissionUrl(), "재제출됨");
                    }
                }
                
                existingSubmission = missionSubmissionRepository.save(existingSubmission);

                log.info("미션 재제출 완료 - submissionId: {}, applicationId: {}",
                        existingSubmission.getId(), applicationId);
                return InfluencerMissionSubmissionResponse.fromEntity(existingSubmission);

            } else {
                // 이미 완료된 미션
                throw new BusinessException("이미 완료된 미션은 재제출할 수 없어요.");
            }
        }

        // 신규 미션 제출
        MissionSubmission submission = MissionSubmission.builder()
                .campaignApplication(application)
                .submissionUrl(request.getMissionUrl())
                .platformType(detectPlatformType(request.getMissionUrl()))
                .isCompleted(false)
                .build();

        submission = missionSubmissionRepository.save(submission);

        log.info("미션 신규 제출 완료 - submissionId: {}", submission.getId());
        return InfluencerMissionSubmissionResponse.fromEntity(submission);
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

        // 중복 제출 검사는 submitMission 메서드에서 처리 (수정 요청 상태 고려)

        return application;
    }

    /**
     * 3. 미션 검토
     */
    @Transactional
    public void reviewMissionSubmission(Long submissionId, MissionReviewRequest request, Long clientId) {
        MissionSubmission submission = validateSubmissionForReview(submissionId, clientId);
        CampaignApplication application = submission.getCampaignApplication();

        // revisionReason이 있으면 수정 요청, 없으면 승인으로 판단
        if (request.getRevisionReason() != null && !request.getRevisionReason().trim().isEmpty()) {
            // 수정 요청 처리
            submission.requestRevision(request.getClientFeedback());

            // 수정 요청 이력 추가
            User requestedBy = userRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
            
            int nextRevisionNumber = submission.getRevisions().size() + 1;
            
            MissionRevision revision = MissionRevision.builder()
                    .missionSubmission(submission)
                    .revisionNumber(nextRevisionNumber)
                    .requestedBy(requestedBy)
                    .revisionReason(request.getRevisionReason())
                    .build();
            
            submission.addRevision(revision);

            // ApplicationStatus는 SELECTED 상태 유지 (재제출 가능하도록)

            // 미션 수정 요청 이메일 발송
            try {
                sesService.sendMissionRevisionRequestEmail(
                        application.getUser().getEmail(),
                        application.getUser().getNickname(),
                        application.getCampaign().getTitle(),
                        request.getRevisionReason()
                );
                log.info("미션 수정 요청 이메일 전송 성공: userId={}, submissionId={}", 
                        application.getUser().getId(), submissionId);
            } catch (Exception e) {
                log.error("미션 수정 요청 이메일 전송 실패: userId={}, submissionId={}, error={}", 
                        application.getUser().getId(), submissionId, e.getMessage(), e);
            }

        } else {
            // 미션 승인 처리
            submission.approve(request.getClientFeedback(), null);

            // 승인 시 모든 revision 이력을 완료 처리 (더 이상 수정 요청 사유 표시 안함)
            for (MissionRevision revision : submission.getRevisions()) {
                if (!revision.isCompleted()) {
                    revision.completeRevision(submission.getSubmissionUrl(), "미션 승인됨");
                }
            }

            // ApplicationStatus: SELECTED → COMPLETED
            application.updateStatus(ApplicationStatus.COMPLETED);

            // 포트폴리오에 추가
            createMissionHistory(submission, null, request.getClientFeedback());

            // 미션 완료 이메일 발송
            try {
                sesService.sendMissionApprovedEmail(
                        application.getUser().getEmail(),
                        application.getUser().getNickname(),
                        application.getCampaign().getTitle(),
                        request.getClientFeedback()
                );
                log.info("미션 완료 이메일 전송 성공: userId={}, submissionId={}", 
                        application.getUser().getId(), submissionId);
            } catch (Exception e) {
                log.error("미션 완료 이메일 전송 실패: userId={}, submissionId={}, error={}", 
                        application.getUser().getId(), submissionId, e.getMessage(), e);
            }
        }

        missionSubmissionRepository.save(submission);
        campaignApplicationRepository.save(application);

        String status = (request.getRevisionReason() != null && !request.getRevisionReason().trim().isEmpty())
                ? "REVISION_REQUESTED" : "APPROVED";
        log.info("미션 검토 완료 - submissionId: {}, status: {}", submissionId, status);
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

        // 상태 검증 (완료되지 않은 상태만 검토 가능)
        if (submission.getIsCompleted()) {
            throw new BusinessException("이미 완료된 미션은 검토할 수 없어요.");
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
                .platformType(submission.getPlatformType())  // ✅ platformType 추가
                .submissionUrl(submission.getSubmissionUrl())
                .completionDate(ZonedDateTime.now())
                .clientReview(review)
                .isPublic(true)
                .isFeatured(false)
                .build();

        userMissionHistoryRepository.save(history);
        log.info("미션 이력 생성 완료 - userId: {}, campaignId: {}, platformType: {}", 
                user.getId(), campaign.getId(), submission.getPlatformType());
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
        // 완료된 미션 이력 조회
        List<UserMissionHistory> histories = userMissionHistoryRepository.findByUserIdOrderByCompletionDateDesc(userId);
        
        // 진행 중인 미션 제출 조회
        List<MissionSubmission> ongoingSubmissions = missionSubmissionRepository.findByUserId(userId);
        
        List<UserMissionHistoryResponse> responses = new java.util.ArrayList<>();
        
        // 완료된 미션 이력 추가 (승인된 미션들 - revisionReason 없음)
        responses.addAll(histories.stream()
                .map(UserMissionHistoryResponse::fromEntity)
                .collect(Collectors.toList()));
        
        // 진행 중인 미션들 추가 (수정 요청 받은 미션들 포함)
        for (MissionSubmission submission : ongoingSubmissions) {
            if (!submission.isCompleted()) {
                // 미완료된 미션만 추가
                responses.add(UserMissionHistoryResponse.fromMissionSubmission(submission));
            }
        }
        
        // 완료일시/제출일시 기준으로 정렬
        responses.sort((a, b) -> {
            ZonedDateTime dateA = a.getMission().getCompletionDate() != null 
                ? a.getMission().getCompletionDate() 
                : ZonedDateTime.now(); // 진행중인 미션은 현재 시간으로
            ZonedDateTime dateB = b.getMission().getCompletionDate() != null 
                ? b.getMission().getCompletionDate() 
                : ZonedDateTime.now();
            return dateB.compareTo(dateA); // 최신순
        });
        
        return responses;
    }

    /**
     * 7-1. 유저 미션 이력 조회 (클라이언트용 - 간소화된 정보)
     */
    @Transactional(readOnly = true)
    public List<ClientUserMissionHistoryResponse> getClientUserMissionHistory(Long userId) {
        // 완료된 미션 이력만 조회 (클라이언트는 간단한 정보만 필요)
        List<UserMissionHistory> histories = userMissionHistoryRepository.findByUserIdOrderByCompletionDateDesc(userId);
        
        return histories.stream()
                .map(ClientUserMissionHistoryResponse::fromEntity)
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
        long completedSubmissions = submissions.stream()
                .mapToLong(s -> s.getIsCompleted() ? 1 : 0)
                .sum();
        long pendingSubmissions = submissions.stream()
                .mapToLong(s -> s.isPending() ? 1 : 0)
                .sum();
        long revisionRequested = submissions.stream()
                .mapToLong(s -> s.isRevisionRequested() ? 1 : 0)
                .sum();

        // 평균 평점 계산 (승인된 미션 중)
        Double averageRating = userMissionHistoryRepository.findAverageRatingByCampaignId(campaignId);

        return CampaignMissionStatisticsResponse.builder()
                .campaignId(campaignId)
                .campaignTitle(campaign.getTitle())
                .totalSubmissions(totalSubmissions)
                .approvedSubmissions(completedSubmissions)
                .pendingSubmissions(pendingSubmissions)
                .revisionRequested(revisionRequested)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .build();
    }

    /**
     * URL을 통해 플랫폼 타입 감지
     */
    private String detectPlatformType(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        String lowerUrl = url.toLowerCase();
        
        if (lowerUrl.contains("instagram.com")) {
            return "INSTAGRAM";
        } else if (lowerUrl.contains("blog.naver.com")) {
            return "NAVER_BLOG";
        } else if (lowerUrl.contains("tistory.com")) {
            return "TISTORY";
        } else if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) {
            return "YOUTUBE";
        } else if (lowerUrl.contains("facebook.com")) {
            return "FACEBOOK";
        } else if (lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com")) {
            return "TWITTER";
        } else {
            return "OTHER";
        }
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
