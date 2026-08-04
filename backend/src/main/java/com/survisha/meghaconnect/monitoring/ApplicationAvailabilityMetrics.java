package com.survisha.meghaconnect.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAvailabilityMetrics {
    private final ApplicationAvailability availability;

    public ApplicationAvailabilityMetrics(ApplicationAvailability availability, MeterRegistry registry) {
        this.availability = availability;
        Gauge.builder("meghaconnect.application.liveness", this, ApplicationAvailabilityMetrics::liveness)
                .description("1 when the Spring application is live").register(registry);
        Gauge.builder("meghaconnect.application.readiness", this, ApplicationAvailabilityMetrics::readiness)
                .description("1 when the Spring application is ready").register(registry);
    }

    double liveness() {
        return availability.getLivenessState() == LivenessState.CORRECT ? 1 : 0;
    }

    double readiness() {
        return availability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC ? 1 : 0;
    }
}
