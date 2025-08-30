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
 * 공지사항 엔티티
 */
@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 공지사항 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 공지사항 내용
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
     * 필독 여부
     */
    @Column(nullable = false, name = "is_must_read")
    private Boolean isMustRead = false;

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
    public Notice(String title, String content, Boolean isMustRead, Long authorId, String authorName) {
        this.title = title;
        this.content = content;
        this.isMustRead = isMustRead != null ? isMustRead : false;
        this.authorId = authorId;
        this.authorName = authorName;
        this.viewCount = 0L;
    }

    // 비즈니스 로직 메서드들

    /**
     * 공지사항 내용 업데이트
     */
    public void updateContent(String title, String content, Boolean isMustRead) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (isMustRead != null) {
            this.isMustRead = isMustRead;
        }
    }

    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount++;
    }

    /**
     * 필독 여부 설정
     */
    public void setMustRead(boolean isMustRead) {
        this.isMustRead = isMustRead;
    }
}
