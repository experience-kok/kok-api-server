package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "체험콕 글 상세 응답 래퍼")
public class KokPostDetailWrapper {

    @Schema(description = "체험콕 글 상세 정보")
    private KokPostDetailResponse kokPost;

    @Builder
    private KokPostDetailWrapper(KokPostDetailResponse kokPost) {
        this.kokPost = kokPost;
    }

    /**
     * KokPostDetailResponse를 래핑하는 정적 팩토리 메서드
     */
    public static KokPostDetailWrapper of(KokPostDetailResponse kokPostDetailResponse) {
        return KokPostDetailWrapper.builder()
                .kokPost(kokPostDetailResponse)
                .build();
    }
}
