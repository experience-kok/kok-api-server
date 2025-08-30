package com.example.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PostgreSQLMonitoringConfig {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 10분마다 PostgreSQL 서버 연결 상태를 로그로 출력
     */
    @Scheduled(fixedRate = 600000) // 10분 = 600,000ms
    public void logPostgreSQLStats() {
        try {
            Integer maxConnections = jdbcTemplate.queryForObject(
                "SHOW max_connections", Integer.class);
            
            Integer currentConnections = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity", Integer.class);
            
            Integer appConnections = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE application_name = 'ChkokAuthService'", 
                Integer.class);

            // 연결 상태별 통계
            List<Map<String, Object>> connectionStats = jdbcTemplate.queryForList(
                "SELECT state, count(*) as count FROM pg_stat_activity WHERE application_name = 'ChkokAuthService' GROUP BY state"
            );

            double serverUsage = (double) currentConnections / maxConnections * 100;
            
            log.info("🐘 PostgreSQL 연결 상태 - 서버: {}/{} ({:.1f}%), 앱: {}개", 
                    currentConnections, maxConnections, serverUsage, appConnections);

            // 앱별 연결 상태 출력
            if (!connectionStats.isEmpty()) {
                StringBuilder statsMsg = new StringBuilder("📊 앱 연결 상태별 통계: ");
                for (Map<String, Object> stat : connectionStats) {
                    statsMsg.append(String.format("%s=%s개 ", stat.get("state"), stat.get("count")));
                }
                log.info(statsMsg.toString());
            }

            // 서버 연결 사용률 경고
            if (serverUsage > 80) {
                log.warn("⚠️ PostgreSQL 서버 연결 사용률 높음: {:.1f}% (최대: {}개)", serverUsage, maxConnections);
            }

            // 장시간 실행 중인 쿼리 체크
            Integer longRunningQueries = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE state = 'active' AND query_start < now() - interval '30 seconds'", 
                Integer.class);
            
            if (longRunningQueries > 0) {
                log.warn("🐌 30초 이상 실행 중인 쿼리: {}개", longRunningQueries);
            }

        } catch (Exception e) {
            log.error("PostgreSQL 상태 조회 실패: {}", e.getMessage());
        }
    }

    /**
     * PostgreSQL 서버에서 연결 수가 임계값에 도달했을 때 알림 (2분마다 체크)
     */
    @Scheduled(fixedRate = 120000) // 2분마다 체크
    public void checkCriticalConnectionUsage() {
        try {
            Integer maxConnections = jdbcTemplate.queryForObject(
                "SHOW max_connections", Integer.class);
            
            Integer currentConnections = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity", Integer.class);

            double usage = (double) currentConnections / maxConnections * 100;

            // 95% 이상이면 긴급 알림
            if (usage >= 95) {
                log.error("🚨 CRITICAL: PostgreSQL 서버 연결 사용률 {}% - 즉시 확인 필요! (max_connections={})", 
                         String.format("%.1f", usage), maxConnections);
            }
            // 90% 이상이면 경고
            else if (usage >= 90) {
                log.warn("⚠️ WARNING: PostgreSQL 서버 연결 사용률 {}% - 주의 필요 (max_connections={})", 
                        String.format("%.1f", usage), maxConnections);
            }

            // 애플리케이션 연결 체크
            Integer appConnections = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE application_name = 'ChkokAuthService'", 
                Integer.class);
            
            if (appConnections > 70) {
                log.warn("⚠️ WARNING: ChkokAuthService 연결 수 {}개 - HikariCP 설정 점검 필요", appConnections);
            }

        } catch (Exception e) {
            log.error("PostgreSQL 임계값 체크 실패: {}", e.getMessage());
        }
    }

    /**
     * 시스템 시작 시 PostgreSQL 설정 정보 로그 출력
     */
    @Scheduled(initialDelay = 10000, fixedRate = Long.MAX_VALUE) // 시작 10초 후 1회만 실행
    public void logPostgreSQLConfig() {
        try {
            Integer maxConnections = jdbcTemplate.queryForObject(
                "SHOW max_connections", Integer.class);
            
            String sharedBuffers = jdbcTemplate.queryForObject(
                "SHOW shared_buffers", String.class);
            
            String workMem = jdbcTemplate.queryForObject(
                "SHOW work_mem", String.class);

            log.info("🐘 PostgreSQL 설정 정보 - max_connections: {}, shared_buffers: {}, work_mem: {}", 
                    maxConnections, sharedBuffers, workMem);

        } catch (Exception e) {
            log.error("PostgreSQL 설정 정보 조회 실패: {}", e.getMessage());
        }
    }
}
