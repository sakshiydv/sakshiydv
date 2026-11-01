package com.example.urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated executor for analytics work so it never competes with request threads.
 *
 * This is our "queue" for now: a bounded in-memory task queue drained by a small
 * worker pool. It proves the read/write separation. Later we swap this out for
 * Kafka (a durable, cross-process queue) without touching the redirect path —
 * only {@code ClickAnalyticsListener} changes.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    public static final String ANALYTICS_EXECUTOR = "analyticsExecutor";

    @Bean(name = ANALYTICS_EXECUTOR)
    public Executor analyticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("analytics-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10_000);
        // If the queue fills, run the task on the caller thread rather than dropping
        // clicks. In a real system Kafka's broker absorbs this backpressure instead.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
