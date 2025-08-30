package com.example.auth.service;

import com.example.auth.domain.User;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.WithdrawnUser;
import com.example.auth.dto.withdrawal.WithdrawalResponse;
import com.example.auth.exception.BusinessException;
import com.example.auth.exception.WithdrawalException;
import com.example.auth.repository.CampaignApplicationRepository;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.repository.WithdrawnUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * 회원 탈퇴 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final WithdrawnUserRepository withdrawnUserRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignApplicationRepository campaignApplicationRepository;

    /**
     * 회원 탈퇴 처리
     * @param userId 탈퇴할 사용자 ID
     * @param withdrawalReason 탈퇴 사유
     * @return 탈퇴 완료 응답
     */
    public WithdrawalResponse withdrawUser(Long userId, String withdrawalReason) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        // 2. 탈퇴 가능 여부 체크
        validateWithdrawalEligibility(user);

        // 3. 탈퇴 기록 저장 (재가입 제한용)
        WithdrawnUser withdrawnUser = WithdrawnUser.createFromUser(user, withdrawalReason);
        withdrawnUserRepository.save(withdrawnUser);

        log.info("사용자 탈퇴 처리 완료 - userId: {}, email: {}", userId, user.getEmail());

        // 4. 사용자 계정 완전 삭제 (Hard Delete)
        userRepository.deleteById(userId);

        // 5. 응답 생성
        return WithdrawalResponse.builder()
                .withdrawnAt(withdrawnUser.getWithdrawnAt())
                .canRejoinAt(withdrawnUser.getCanRejoinAt())
                .withdrawalReason(withdrawalReason)
                .message("탈퇴가 완료되었습니다. 24시간 후 재가입이 가능합니다.")
                .build();
    }

    /**
     * 탈퇴 가능 여부 검증
     * @param user 탈퇴하려는 사용자
     */
    private void validateWithdrawalEligibility(User user) {
        // 1. 이미 비활성화된 계정인지 체크
        if (!user.getActive()) {
            throw new WithdrawalException.WithdrawalNotAllowedException("이미 비활성화된 계정입니다.");
        }

        // 2. CLIENT 사용자의 경우: 승인되고 아직 만료되지 않은 캠페인이 있는지 체크
        if ("CLIENT".equals(user.getRole())) {
            boolean hasActiveApprovedCampaigns = campaignRepository.existsByCreatorIdAndApprovalStatusAndRecruitmentEndDateGreaterThanEqual(
                    user.getId(),
                    Campaign.ApprovalStatus.APPROVED,
                    java.time.LocalDate.now()
            );

            if (hasActiveApprovedCampaigns) {
                throw new WithdrawalException.WithdrawalNotAllowedException(
                    "진행 중인 승인된 캠페인이 있어 탈퇴할 수 없습니다. 캠페인 완료 후 다시 시도해주세요."
                );
            }
        }

        // 3. USER 사용자의 경우: 선정된 캠페인 신청이 있는지 체크
        if ("USER".equals(user.getRole())) {
            boolean hasSelectedApplications = campaignApplicationRepository.existsByUserIdAndApplicationStatus(
                    user.getId(),
                    com.example.auth.constant.ApplicationStatus.SELECTED
            );

            if (hasSelectedApplications) {
                throw new WithdrawalException.WithdrawalNotAllowedException(
                    "선정된 캠페인이 있어 탈퇴할 수 없습니다. 캠페인 완료 후 다시 시도해주세요."
                );
            }
        }
    }

    /**
     * 재가입 제한 체크 (가입 시 호출)
     * @param email 이메일
     * @param socialId 소셜 ID
     * @param provider 제공업체
     */
    @Transactional(readOnly = true)
    public void checkWithdrawalRestriction(String email, String socialId, String provider) {
        Optional<WithdrawnUser> withdrawal = withdrawnUserRepository.findActiveWithdrawal(
                provider, email, socialId, ZonedDateTime.now()
        );

        if (withdrawal.isPresent()) {
            WithdrawnUser w = withdrawal.get();
            long remainingHours = w.getRemainingHours();

            throw new WithdrawalException.RejoinRestrictionException(
                String.format("탈퇴 후 24시간 동안 재가입할 수 없습니다. (남은 시간: %d시간)", remainingHours),
                remainingHours
            );
        }
    }

    /**
     * 사용자의 탈퇴 이력 조회
     * @param userId 사용자 ID
     * @return 탈퇴 이력 목록
     */
    @Transactional(readOnly = true)
    public java.util.List<WithdrawnUser> getUserWithdrawalHistory(Long userId) {
        return withdrawnUserRepository.findByOriginalUserIdOrderByWithdrawnAtDesc(userId);
    }

    /**
     * 오래된 탈퇴 기록 정리 (스케줄러)
     * 매일 새벽 3시에 6개월 이전 탈퇴 기록 삭제
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldWithdrawalRecords() {
        try {
            ZonedDateTime cutoff = ZonedDateTime.now().minusMonths(6);
            int deletedCount = withdrawnUserRepository.deleteOldWithdrawalRecords(cutoff);

            if (deletedCount > 0) {
                log.info("오래된 탈퇴 기록 정리 완료 - 삭제된 레코드 수: {}", deletedCount);
            }
        } catch (Exception e) {
            log.error("탈퇴 기록 정리 중 오류 발생", e);
        }
    }
}
