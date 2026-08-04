package com.survisha.meghaconnect.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

class MonitoredOperationAspectTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MonitoredOperationAspect aspect = new MonitoredOperationAspect(registry);

    @Test
    void recordsBoundedSuccessMetricsWithoutArguments() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("sensitive-value-that-must-not-be-a-tag");

        aspect.record(joinPoint, operation("citizen_lookup_by_phone", MonitoredOperation.Category.DATABASE));

        assertThat(registry.counter("meghaconnect.operation", "operation", "citizen_lookup_by_phone",
                "category", "database", "result", "success").count()).isEqualTo(1);
        assertThat(registry.timer("meghaconnect.db.operation.duration", "operation", "citizen_lookup_by_phone",
                "result", "success").count()).isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags().toString())
                .doesNotContain("sensitive-value"));
    }

    @Test
    void recordsFailureByCategoryWithoutExceptionMessage() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("citizen mobile 9999999999"));

        try {
            aspect.record(joinPoint, operation("visitor_registration", MonitoredOperation.Category.BUSINESS));
        } catch (IllegalStateException expected) {
            // Expected.
        }

        assertThat(registry.counter("meghaconnect.operation", "operation", "visitor_registration",
                "category", "business", "result", "failure").count()).isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags().toString())
                .doesNotContain("9999999999"));
    }

    @Test
    void recordsSmsProviderBusinessFailure() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(com.survisha.common.sms.SmsResponse.builder()
                .success(false).provider("private-provider-name").message("sensitive response").build());

        aspect.record(joinPoint, operation("sms_provider_call", MonitoredOperation.Category.BUSINESS));

        assertThat(registry.counter("meghaconnect.sms.provider", "provider", "sms-provider",
                "operation", "send", "result", "failure").count()).isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags().toString())
                .doesNotContain("private-provider-name", "sensitive response"));
    }

    private MonitoredOperation operation(String value, MonitoredOperation.Category category) {
        return new MonitoredOperation() {
            public String value() { return value; }
            public Category category() { return category; }
            public Class<MonitoredOperation> annotationType() { return MonitoredOperation.class; }
        };
    }
}
