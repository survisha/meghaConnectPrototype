package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    private final MeterRegistry meterRegistry;

    public AsyncConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("meghaconnect-async-");
        executor.setRejectedExecutionHandler((task, pool) -> {
            meterRegistry.counter("meghaconnect.executor.rejected", "executor", "application").increment();
            throw new java.util.concurrent.RejectedExecutionException("Application executor queue is full");
        });
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        ExecutorServiceMetrics.monitor(meterRegistry, executor.getThreadPoolExecutor(),
                "meghaconnect.executor", java.util.List.of(Tag.of("executor", "application")));
        return executor;
    }

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> parentContext = RequestContextUtil.copyMdcContext();
            return () -> {
                Map<String, String> previousContext = RequestContextUtil.copyMdcContext();
                long startedAt = System.nanoTime();
                String result = "success";
                try {
                    RequestContextUtil.restoreMdcContext(parentContext);
                    runnable.run();
                } catch (RuntimeException error) {
                    result = "failure";
                    throw error;
                } finally {
                    meterRegistry.timer("meghaconnect.executor.task.duration", "executor", "application",
                            "result", result).record(System.nanoTime() - startedAt,
                                    java.util.concurrent.TimeUnit.NANOSECONDS);
                    RequestContextUtil.restoreMdcContext(previousContext);
                }
            };
        };
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("Unhandled async exception in {}", method.getName(), ex);
    }
}
