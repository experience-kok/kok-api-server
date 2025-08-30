package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 인플루언서용 유저 미션 이력 데이터 래퍼 DTO - histories 필드로 감싸서 응답
 * - 상세 정보 포함 (isCompleted, completionDate, clientReview, revisionReason)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "인플루언서용 유저 미션 이력 데이터 래퍼 (상세 정보 포함)")
public class UserMissionHistoryDataWrapper {

    @Schema(description = "인플루언서용 미션 이력 목록 (상세 정보 포함)", example = """
            [
              {
                "id": 1,
                "campaign": {
                  "title": "이탈리안 레스토랑 신메뉴 체험단",
                  "category": "맛집"
                },
                "mission": {
                  "missionUrl": "https://instagram.com/p/abc123",
                  "isCompleted": true,
                  "completionDate": "2024-03-16T10:30:00Z",
                  "clientReview": "미션을 잘 수행해주셨습니다.",
                  "revisionReason": null
                }
              },
              {
                "id": 2,
                "campaign": {
                  "title": "카페 체험단",
                  "category": "카페"
                },
                "mission": {
                  "missionUrl": "https://instagram.com/p/def456",
                  "isCompleted": false,
                  "completionDate": null,
                  "clientReview": "수정이 필요합니다.",
                  "revisionReason": "제품명이 정확히 표기되지 않았습니다."
                }
              }
            ]
            """)
    private List<UserMissionHistoryResponse> histories;

    /**
     * 미션 이력 목록으로부터 래퍼 생성
     */
    public static UserMissionHistoryDataWrapper of(List<UserMissionHistoryResponse> histories) {
        return UserMissionHistoryDataWrapper.builder()
                .histories(histories)
                .build();
    }
}
