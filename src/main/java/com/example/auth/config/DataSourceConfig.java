package com.example.auth.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableScheduling
public class DataSourceConfig {

    private DataSource dataSource;

    public DataSourceConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 5분마다 DB 연결 풀 상태를 로그로 출력
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    public void logConnectionPoolStats() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            try {
                int active = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
                int idle = hikariDataSource.getHikariPoolMXBean().getIdleConnections();
                int total = hikariDataSource.getHikariPoolMXBean().getTotalConnections();
                int maxSize = hikariDataSource.getMaximumPoolSize();
                int awaitingConnection = hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
                
                double usagePercent = (double) active / maxSize * 100;
                
                if (usagePercent > 70) {
                    log.warn("⚠️ DB 연결 풀 사용률 높음: 활성={}/{} ({:.1f}%), 유휴={}, 대기중={}", 
                            active, maxSize, usagePercent, idle, awaitingConnection);
                } else {
                    log.info("📊 DB 연결 풀 상태: 활성={}/{} ({:.1f}%), 유휴={}, 대기중={}", 
                            active, maxSize, usagePercent, idle, awaitingConnection);
                }
                
                // 대기 중인 연결이 있으면 경고
                if (awaitingConnection > 0) {
                    log.warn("🚨 DB 연결 풀 대기: {}개의 스레드가 연결을 기다리고 있습니다!", awaitingConnection);
                }
                
            } catch (Exception e) {
                log.error("DB 연결 풀 상태 조회 실패: {}", e.getMessage());
            }
        }
    }

    /**
     * 1분마다 연결 풀 위험 상황 체크
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60,000ms
    public void checkConnectionPoolHealth() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            try {
                int active = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
                int maxSize = hikariDataSource.getMaximumPoolSize();
                int awaitingConnection = hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
                
                double usagePercent = (double) active / maxSize * 100;
                
                // 90% 이상이면 긴급 알림
                if (usagePercent >= 90) {
                    log.error("🚨 CRITICAL: HikariCP 연결 풀 사용률 {}% - 즉시 확인 필요!", String.format("%.1f", usagePercent));
                }
                // 80% 이상이면 경고
                else if (usagePercent >= 80) {
                    log.warn("⚠️ WARNING: HikariCP 연결 풀 사용률 {}% - 주의 필요", String.format("%.1f", usagePercent));
                }

                // 대기 중인 스레드가 있으면 알림
                if (awaitingConnection > 5) {
                    log.error("🚨 CRITICAL: {}개의 스레드가 DB 연결을 기다리고 있습니다!", awaitingConnection);
                } else if (awaitingConnection > 0) {
                    log.warn("⚠️ WARNING: {}개의 스레드가 DB 연결을 기다리고 있습니다!", awaitingConnection);
                }
                
            } catch (Exception e) {
                log.error("HikariCP 상태 체크 실패: {}", e.getMessage());
            }
        }
    }
}
