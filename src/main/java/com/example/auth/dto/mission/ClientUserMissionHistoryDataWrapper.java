package com.example.auth.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 클라이언트용 유저 미션 이력 데이터 래퍼 DTO
 * - 간소화된 정보만 포함 (missionUrl, completionDate만)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "클라이언트용 유저 미션 이력 데이터 래퍼 (간소화)")
public class ClientUserMissionHistoryDataWrapper {

    @Schema(
        description = "클라이언트용 미션 이력 목록 (간소화 - missionUrl, completionDate만 포함)", 
        example = """
            [
              {
                "id": 1,
                "campaign": {
                  "title": "이탈리안 레스토랑 신메뉴 체험단",
                  "category": "맛집"
                },
                "mission": {
                  "missionUrl": "https://instagram.com/p/abc123",
                  "completionDate": "2024-03-16T10:30:00Z"
                }
              }
            ]
            """
    )
    private List<ClientUserMissionHistoryResponse> histories;

    /**
     * 미션 이력 목록으로부터 래퍼 생성
     */
    public static ClientUserMissionHistoryDataWrapper of(List<ClientUserMissionHistoryResponse> histories) {
        return ClientUserMissionHistoryDataWrapper.builder()
                .histories(histories)
                .build();
    }
}
