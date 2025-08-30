package com.example.auth.scheduler;

import com.example.auth.constant.ApplicationStatus;
import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignApplication;
import com.example.auth.repository.CampaignRepository;
import com.example.auth.repository.CampaignApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 캠페인 상태 자동 관리 스케줄러
 * 일반 캠페인의 모집 종료일이 되면 APPLIED → PENDING 상태로 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignStatusScheduler {

    private final CampaignRepository campaignRepository;
    private final CampaignApplicationRepository applicationRepository;

    /**
     * 매일 자정(0시)에 캠페인 모집 상태 체크 및 업데이트
     * 일반 캠페인의 모집 종료일이 지나면 신청자 상태를 APPLIED → PENDING으로 변경
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정(0시)
    @Transactional
    public void updateExpiredCampaignApplications() {
        log.info("캠페인 모집 종료 상태 업데이트 스케줄러 시작");
        
        try {
            LocalDate today = LocalDate.now();
            
            // 일반 캠페인 중 모집 종료일이 지난 캠페인들 조회
            List<Campaign> expiredCampaigns = campaignRepository.findExpiredCampaigns(today);
            
            if (expiredCampaigns.isEmpty()) {
                log.info("모집 종료된 캠페인이 없습니다.");
                return;
            }
            
            log.info("모집 종료된 캠페인 {}개 처리 시작", expiredCampaigns.size());
            
            int totalUpdated = 0;
            
            for (Campaign campaign : expiredCampaigns) {
                // 해당 캠페인의 APPLIED 상태 신청자들을 PENDING으로 변경
                List<CampaignApplication> appliedApplications = 
                    applicationRepository.findByCampaignAndApplicationStatus(campaign, ApplicationStatus.APPLIED);
                
                if (!appliedApplications.isEmpty()) {
                    appliedApplications.forEach(app -> app.updateStatus(ApplicationStatus.PENDING));
                    applicationRepository.saveAll(appliedApplications);
                    
                    totalUpdated += appliedApplications.size();
                    log.info("캠페인 '{}' (ID: {}) 신청자 {}명 상태 변경: APPLIED → PENDING", 
                            campaign.getTitle(), campaign.getId(), appliedApplications.size());
                }
            }
            
            log.info("캠페인 모집 종료 상태 업데이트 완료: 총 {}명의 신청자 상태 변경", totalUpdated);
            
        } catch (Exception e) {
            log.error("캠페인 상태 업데이트 스케줄러 실행 중 오류 발생", e);
        }
    }
    
    /**
     * 매시간마다 상태 점검 (선택적)
     * 더 빈번한 체크가 필요한 경우 사용
     */
    @Scheduled(cron = "0 0 * * * ?") // 매시간 정각
    @Transactional(readOnly = true)
    public void checkCampaignStatus() {
        LocalDate today = LocalDate.now();
        
        // 오늘 종료되는 캠페인 개수 체크
        long expiringSoon = campaignRepository.countExpiringSoonCampaigns(today);
        
        if (expiringSoon > 0) {
            log.info("오늘 모집 종료 예정인 캠페인: {}개", expiringSoon);
        }
    }
}
