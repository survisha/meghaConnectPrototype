package com.survisha.meghaconnect.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MonitoringEndpointAuthenticationFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingCredentialForPrometheus() throws Exception {
        var filter = new MonitoringEndpointAuthenticationFilter("test-monitoring-token");
        var request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(invoked).isFalse();
        assertThat(response.getContentAsString()).doesNotContain("test-monitoring-token");
    }

    @Test
    void acceptsBearerCredentialForPrometheus() throws Exception {
        var filter = new MonitoringEndpointAuthenticationFilter("test-monitoring-token");
        var request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.addHeader("Authorization", "Bearer test-monitoring-token");
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertThat(invoked).isTrue();
    }

    @Test
    void doesNotProtectHealthProbe() throws Exception {
        var filter = new MonitoringEndpointAuthenticationFilter("test-monitoring-token");
        var request = new MockHttpServletRequest("GET", "/actuator/health/readiness");
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertThat(invoked).isTrue();
    }

    @Test
    void recognizesActuatorEndpointsBehindApiContextPath() throws Exception {
        var filter = new MonitoringEndpointAuthenticationFilter("test-monitoring-token");
        var health = new MockHttpServletRequest("GET", "/api/actuator/health");
        health.setContextPath("/api");
        var healthInvoked = new AtomicBoolean();

        filter.doFilter(health, new MockHttpServletResponse(), (req, res) -> healthInvoked.set(true));

        assertThat(healthInvoked).isTrue();

        var metrics = new MockHttpServletRequest("GET", "/api/actuator/metrics");
        metrics.setContextPath("/api");
        var metricsResponse = new MockHttpServletResponse();
        var metricsInvoked = new AtomicBoolean();

        filter.doFilter(metrics, metricsResponse, (req, res) -> metricsInvoked.set(true));

        assertThat(metricsResponse.getStatus()).isEqualTo(401);
        assertThat(metricsInvoked).isFalse();
    }
}
