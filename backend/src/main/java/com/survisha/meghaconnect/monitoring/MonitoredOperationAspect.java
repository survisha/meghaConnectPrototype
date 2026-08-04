package com.survisha.meghaconnect.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class MonitoredOperationAspect {
    private final MeterRegistry meterRegistry;

    @Around("@annotation(operation)")
    public Object record(ProceedingJoinPoint joinPoint, MonitoredOperation operation) throws Throwable {
        long startedAt = System.nanoTime();
        String result = "success";
        try {
            Object value = joinPoint.proceed();
            if (value instanceof java.util.Optional && ((java.util.Optional<?>) value).isEmpty()) result = "not_found";
            if (Boolean.FALSE.equals(value)) result = "failure";
            if (value instanceof com.survisha.common.sms.SmsResponse
                    && !((com.survisha.common.sms.SmsResponse) value).isSuccess()) result = "failure";
            if (value instanceof com.survisha.meghaconnect.formextraction.dto.VisitorFormExtractionResponse
                    && !((com.survisha.meghaconnect.formextraction.dto.VisitorFormExtractionResponse) value).isSuccess()) {
                result = "failure";
            }
            return value;
        } catch (Throwable error) {
            result = isTimeout(error) ? "timeout" : "failure";
            throw error;
        } finally {
            String category = operation.category().name().toLowerCase(java.util.Locale.ROOT);
            meterRegistry.counter("meghaconnect.operation", "operation", operation.value(),
                    "category", category, "result", result).increment();
            long elapsed = System.nanoTime() - startedAt;
            meterRegistry.timer("meghaconnect.operation.duration", "operation", operation.value(),
                    "category", category, "result", result)
                    .record(elapsed, java.util.concurrent.TimeUnit.NANOSECONDS);
            if (operation.category() == MonitoredOperation.Category.DATABASE) {
                meterRegistry.counter("meghaconnect.db.operation", "operation", operation.value(),
                        "result", result).increment();
                meterRegistry.timer("meghaconnect.db.operation.duration", "operation", operation.value(),
                        "result", result).record(elapsed, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
            if ("sms_provider_call".equals(operation.value())) {
                meterRegistry.counter("meghaconnect.sms.provider", "provider", "sms-provider",
                        "operation", "send", "result", result).increment();
                meterRegistry.timer("meghaconnect.sms.provider.duration", "provider", "sms-provider",
                        "operation", "send", "result", result)
                        .record(elapsed, java.util.concurrent.TimeUnit.NANOSECONDS);
                if (!"success".equals(result)) {
                    meterRegistry.counter("meghaconnect.external.api.errors", "provider", "sms-provider",
                            "operation", "send", "result", result).increment();
                }
            }
        }
    }

    private boolean isTimeout(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.util.concurrent.TimeoutException) return true;
        }
        return false;
    }
}
