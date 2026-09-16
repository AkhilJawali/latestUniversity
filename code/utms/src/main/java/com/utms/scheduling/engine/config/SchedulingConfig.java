package com.utms.scheduling.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Thread pool configuration for the scheduling engine (KD-46).
 * Dedicated pool isolates CPU-intensive generation from web request threads.
 * AbortPolicy rejects when full — controller catches and returns 503 (Fix #5).
 */
@Configuration
public class SchedulingConfig {

    @Bean("schedulingEngineExecutor")
    public Executor schedulingEngineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("sched-engine-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
