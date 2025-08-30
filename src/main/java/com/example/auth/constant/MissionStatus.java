package com.example.auth.constant;

/**
 * 미션 상태 열거형
 * 인플루언서 미션 제출 상태를 나타냅니다.
 */
public enum MissionStatus {
    NOT_SUBMITTED("미제출"),
    SUBMITTED("제출"), 
    REVISION_REQUESTED("수정요청"),
    COMPLETED("완료");
    
    private final String description;
    
    MissionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
