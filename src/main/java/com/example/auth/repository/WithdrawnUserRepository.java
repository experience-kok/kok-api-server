package com.example.auth.repository;

import com.example.auth.domain.WithdrawnUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 탈퇴 사용자 정보 관리 Repository
 */
@Repository
public interface WithdrawnUserRepository extends JpaRepository<WithdrawnUser, Long> {

    /**
     * 이메일로 현재 재가입 제한 중인 탈퇴 기록 조회
     * @param email 이메일
     * @param now 현재 시간
     * @return 재가입 제한 중인 탈퇴 기록
     */
    @Query("SELECT w FROM WithdrawnUser w WHERE w.email = :email AND w.canRejoinAt > :now")
    Optional<WithdrawnUser> findActiveWithdrawalByEmail(
            @Param("email") String email,
            @Param("now") ZonedDateTime now
    );

    /**
     * 소셜 ID로 현재 재가입 제한 중인 탈퇴 기록 조회
     * @param socialId 소셜 ID
     * @param now 현재 시간
     * @return 재가입 제한 중인 탈퇴 기록
     */
    @Query("SELECT w FROM WithdrawnUser w WHERE w.socialId = :socialId AND w.canRejoinAt > :now")
    Optional<WithdrawnUser> findActiveWithdrawalBySocialId(
            @Param("socialId") String socialId,
            @Param("now") ZonedDateTime now
    );

    /**
     * 종합적인 재가입 제한 체크 (이메일 + 소셜ID + 제공업체)
     * @param provider 제공업체 ('kakao', 'email')
     * @param email 이메일 (null 가능)
     * @param socialId 소셜 ID (null 가능)
     * @param now 현재 시간
     * @return 재가입 제한 중인 탈퇴 기록
     */
    @Query("SELECT w FROM WithdrawnUser w WHERE w.provider = :provider AND " +
           "((w.email = :email AND :email IS NOT NULL) OR " +
           "(w.socialId = :socialId AND :socialId IS NOT NULL)) AND " +
           "w.canRejoinAt > :now")
    Optional<WithdrawnUser> findActiveWithdrawal(
            @Param("provider") String provider,
            @Param("email") String email,
            @Param("socialId") String socialId,
            @Param("now") ZonedDateTime now
    );

    /**
     * 특정 사용자의 모든 탈퇴 기록 조회 (이력 추적용)
     * @param originalUserId 원래 사용자 ID
     * @return 탈퇴 기록 목록
     */
    List<WithdrawnUser> findByOriginalUserIdOrderByWithdrawnAtDesc(Long originalUserId);

    /**
     * 특정 기간 이전의 오래된 탈퇴 기록 삭제 (데이터 정리용)
     * @param cutoff 삭제 기준 시간
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM WithdrawnUser w WHERE w.withdrawnAt < :cutoff")
    int deleteOldWithdrawalRecords(@Param("cutoff") ZonedDateTime cutoff);

    /**
     * 재가입 가능한 만료된 탈퇴 기록들 조회 (정리 대상)
     * @param now 현재 시간
     * @return 만료된 탈퇴 기록 목록
     */
    @Query("SELECT w FROM WithdrawnUser w WHERE w.canRejoinAt <= :now")
    List<WithdrawnUser> findExpiredWithdrawals(@Param("now") ZonedDateTime now);

    /**
     * 특정 제공업체의 탈퇴 통계 조회
     * @param provider 제공업체
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 탈퇴 건수
     */
    @Query("SELECT COUNT(w) FROM WithdrawnUser w WHERE w.provider = :provider AND " +
           "w.withdrawnAt BETWEEN :startDate AND :endDate")
    long countWithdrawalsByProviderAndDateRange(
            @Param("provider") String provider,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate
    );
}
