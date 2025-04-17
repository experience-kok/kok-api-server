package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);  // 기본 실행 대기하는 Thread 수
        executor.setMaxPoolSize(5);   // 동시 동작하는 최대 Thread 수
        executor.setQueueCapacity(500);  // Thread Pool에서 대기하는 최대 작업 수
        executor.setThreadNamePrefix("CrawlTask-");
        executor.initialize();
        return executor;
    }
}