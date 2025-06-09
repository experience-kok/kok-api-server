package com.example.auth.repository;

import com.example.auth.domain.UserSnsPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSnsPlatformRepository extends JpaRepository<UserSnsPlatform, Long> {
    List<UserSnsPlatform> findByUserId(Long userId);

    Optional<UserSnsPlatform> findByUserIdAndId(Long userId, Long platformId);

    Optional<UserSnsPlatform> findByUserIdAndPlatformTypeAndAccountUrl(Long userId, String platformType, String accountUrl);
    
    // 특정 플랫폼 타입과 계정 URL로 조회 (사용자 무관)
    Optional<UserSnsPlatform> findByPlatformTypeAndAccountUrl(String platformType, String accountUrl);

    void deleteByUserIdAndId(Long userId, Long platformId);

    // 특정 시점 이전에 크롤링된 플랫폼 또는 크롤링되지 않은 플랫폼 조회
    List<UserSnsPlatform> findByLastCrawledAtBeforeOrLastCrawledAtIsNull(LocalDateTime dateTime);

    // 특정 사용자의 플랫폼 중 특정 시점 이전에 크롤링된 플랫폼 조회
    @Query("SELECT p FROM UserSnsPlatform p WHERE p.user.id = :userId AND (p.lastCrawledAt < :dateTime OR p.lastCrawledAt IS NULL)")
    List<UserSnsPlatform> findUserPlatformsToRecrawl(@Param("userId") Long userId, @Param("dateTime") LocalDateTime dateTime);

    // 특정 플랫폼 타입과 팔로워 수 이상인 플랫폼 조회
    List<UserSnsPlatform> findByPlatformTypeAndFollowerCountGreaterThanEqual(String platformType, Integer followerCount);
    
    /**
     * 특정 사용자의 모든 SNS 플랫폼 정보를 삭제합니다.
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}