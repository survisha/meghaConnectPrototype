package com.survisha.meghaconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class SpeechAsyncConfig {
    @Bean(name = "speechTaskExecutor")
    public Executor speechTaskExecutor(
            @Value("${speech.max-concurrent-transcriptions:1}") int concurrency,
            @Value("${speech.executor.queue-capacity:100}") int queueCapacity) {
        int boundedConcurrency = Math.max(1, concurrency);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(boundedConcurrency);
        executor.setMaxPoolSize(boundedConcurrency);
        executor.setQueueCapacity(Math.max(1, queueCapacity));
        executor.setThreadNamePrefix("speech-");
        // Rejection never affects upload: the DB PENDING row is recovered by the scheduler.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
