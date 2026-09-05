package com.survisha.meghaconnect.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class VoiceRemarkControllerSecurityTest {
    @Test
    void restrictsEveryEndpointToApproverAndHcm() {
        PreAuthorize policy = VoiceRemarkController.class.getAnnotation(PreAuthorize.class);
        assertThat(policy).isNotNull();
        assertThat(policy.value()).contains("APPROVER", "HCM").doesNotContain("ADMIN", "DEO", "PUBLIC");
    }
}
