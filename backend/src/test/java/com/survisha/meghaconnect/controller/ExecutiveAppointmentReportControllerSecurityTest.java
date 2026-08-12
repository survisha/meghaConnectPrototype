package com.survisha.meghaconnect.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.*;

class ExecutiveAppointmentReportControllerSecurityTest {
    @Test void executiveReportControllerHasStrictRolePolicy() {
        PreAuthorize policy = ExecutiveAppointmentReportController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(policy);
        assertTrue(policy.value().contains("SUPER_ADMIN"));
        assertTrue(policy.value().contains("APPROVER"));
        assertTrue(policy.value().contains("HCM"));
        assertFalse(policy.value().contains("DEO"));
        assertFalse(policy.value().contains("DEPARTMENT_ADMIN"));
        assertFalse(policy.value().contains("DEPARTMENT_PA"));
    }
}
