package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentReportStatusPolicyTest {
    private final AppointmentReportStatusPolicy policy = new AppointmentReportStatusPolicy();

    @Test void includesScheduledAndWalkInCompletion() {
        Appointment scheduled = Appointment.builder().appointmentCategory(Appointment.AppointmentCategory.SCHEDULED)
                .status(Appointment.AppointmentStatus.HCM_MET_COMPLETED).build();
        Appointment walkIn = Appointment.builder().appointmentCategory(Appointment.AppointmentCategory.WALK_IN)
                .status(Appointment.AppointmentStatus.COMPLETED).build();
        assertTrue(policy.isCompleted(scheduled));
        assertTrue(policy.isCompleted(walkIn));
    }

    @Test void excludesPendingScheduledAndRejectedAppointments() {
        Appointment pending = Appointment.builder().appointmentCategory(Appointment.AppointmentCategory.SCHEDULED)
                .status(Appointment.AppointmentStatus.SCHEDULED).build();
        Appointment rejected = Appointment.builder().appointmentCategory(Appointment.AppointmentCategory.SCHEDULED)
                .status(Appointment.AppointmentStatus.REJECTED).build();
        assertFalse(policy.isCompleted(pending));
        assertFalse(policy.isCompleted(rejected));
        assertTrue(policy.rejectedStatuses().contains(Appointment.AppointmentStatus.REJECTED));
    }
}
