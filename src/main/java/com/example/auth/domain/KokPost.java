package com.example.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 체험콕에서 가게 홍보용으로 작성한 글 엔티티
 */
@Entity
@Table(name = "kokposts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class KokPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 글 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 글 내용
     */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 조회수
     */
    @Column(nullable = false, name = "view_count")
    private Long viewCount = 0L;

    /**
     * 캠페인 ID (어떤 캠페인의 글인지 확인용도)
     * Campaign 엔티티와 N:1 관계 (Campaign 1개 - KokPost 여러개)
     */
    @Column(nullable = false, name = "campaign_id")
    private Long campaignId;

    /**
     * 작성자 ID (어드민만 필요한 필드)
     */
    @Column(nullable = false, name = "author_id")
    private Long authorId;

    /**
     * 작성자 이름
     */
    @Column(nullable = false, length = 100, name = "author_name")
    private String authorName;

    /**
     * 방문 정보 (임베디드)
     */
    @Embedded
    private KokPostVisitInfo visitInfo;

    /**
     * 생성 시간
     */
    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public KokPost(String title, String content, Long campaignId, Long authorId,
                   String authorName, KokPostVisitInfo visitInfo) {
        this.title = title;
        this.content = content;
        this.campaignId = campaignId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.visitInfo = visitInfo;
        this.viewCount = 0L;
    }

    // 비즈니스 로직 메서드들

    /**
     * 포스트 내용 업데이트
     */
    public void updateContent(String title, String content) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
    }

    /**
     * 방문 정보 업데이트
     */
    public void updateVisitInfo(KokPostVisitInfo visitInfo) {
        this.visitInfo = visitInfo;
    }

    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount++;
    }
}
