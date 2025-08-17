package com.example.auth.dto.campaign;

import com.example.auth.domain.Campaign;
import com.example.auth.domain.CampaignMissionInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 캠페인 미션 가이드 및 정보 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignMissionGuideResponse {
    
    private Long campaignId;
    
    // 미션 정보 추가
    private MissionInfo missionInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissionInfo {
        // 키워드 정보
        private String missionGuide;    // 미션 가이드
        private List<String> titleKeywords;      // 제목 키워드
        private List<String> bodyKeywords;       // 본문 키워드
        
        // 콘텐츠 요구사항
        private Integer numberOfVideo;           // 영상 개수
        private Integer numberOfImage;           // 이미지 개수
        private Integer numberOfText;            // 글자 수
        private Boolean isMap;                   // 지도 포함 여부
        
        // 미션 일정
        private LocalDate missionStartDate;      // 미션 시작일
        private LocalDate missionDeadlineDate;   // 미션 마감일
        
        public static MissionInfo fromEntity(CampaignMissionInfo missionInfo) {
            if (missionInfo == null) {
                return null;
            }
            
            return MissionInfo.builder()
                    .missionGuide(missionInfo.getMissionGuide() != null ?
                            missionInfo.getMissionGuide() : null)
                    .titleKeywords(missionInfo.getTitleKeywords() != null ? 
                            Arrays.asList(missionInfo.getTitleKeywords()) : null)
                    .bodyKeywords(missionInfo.getBodyKeywords() != null ? 
                            Arrays.asList(missionInfo.getBodyKeywords()) : null)
                    .numberOfVideo(missionInfo.getNumberOfVideo())
                    .numberOfImage(missionInfo.getNumberOfImage())
                    .numberOfText(missionInfo.getNumberOfText())
                    .isMap(missionInfo.getIsMap())
                    .missionStartDate(missionInfo.getMissionStartDate())
                    .missionDeadlineDate(missionInfo.getMissionDeadlineDate())
                    .build();
        }
    }
    
    public static CampaignMissionGuideResponse fromEntity(Campaign campaign) {
        CampaignMissionInfo missionInfo = campaign.getMissionInfo();
        
        return CampaignMissionGuideResponse.builder()
                .campaignId(campaign.getId())
                .missionInfo(MissionInfo.fromEntity(missionInfo))
                .build();
    }
}
