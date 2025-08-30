package com.example.auth.repository;

import com.example.auth.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 모든 공지사항을 페이지네이션으로 조회
     */
    Page<Notice> findAll(Pageable pageable);

    /**
     * 제목으로 공지사항 검색 (대소문자 구분 없이, 페이지네이션)
     */
    Page<Notice> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * 필독 공지사항만 조회 (페이지네이션)
     */
    Page<Notice> findByIsMustReadTrue(Pageable pageable);

    /**
     * 조회수 증가
     */
    @Modifying
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
