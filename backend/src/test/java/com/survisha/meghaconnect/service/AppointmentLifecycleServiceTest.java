package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppointmentLifecycleServiceTest {

    private final AppointmentLifecycleService service = new AppointmentLifecycleService();

    @Test
    void scheduledAppointmentsUseOnlyFrozenTransitions() {
        assertAllowed(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.PENDING,
                Appointment.AppointmentStatus.SCHEDULED);
        assertAllowed(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.PENDING,
                Appointment.AppointmentStatus.REJECTED);
        assertAllowed(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.PENDING,
                Appointment.AppointmentStatus.ROUTED_TO_OFFICIAL);
        assertAllowed(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.SCHEDULED,
                Appointment.AppointmentStatus.HCM_MET_COMPLETED);
    }

    @Test
    void scheduledTerminalAndReverseTransitionsAreRejected() {
        assertRejected(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.REJECTED,
                Appointment.AppointmentStatus.SCHEDULED);
        assertRejected(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.HCM_MET_COMPLETED,
                Appointment.AppointmentStatus.PENDING);
        assertRejected(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.PENDING,
                Appointment.AppointmentStatus.COMPLETED);
    }

    @Test
    void walkInAllowsOnlyPendingToCompleted() {
        assertAllowed(Appointment.AppointmentCategory.WALK_IN, Appointment.AppointmentStatus.PENDING,
                Appointment.AppointmentStatus.COMPLETED);
        assertRejected(Appointment.AppointmentCategory.WALK_IN, Appointment.AppointmentStatus.PENDING,
                Appointment.AppointmentStatus.SCHEDULED);
        assertRejected(Appointment.AppointmentCategory.WALK_IN, Appointment.AppointmentStatus.PENDING,
                Appointment.AppointmentStatus.REJECTED);
        assertRejected(Appointment.AppointmentCategory.WALK_IN, Appointment.AppointmentStatus.COMPLETED,
                Appointment.AppointmentStatus.COMPLETED);
    }

    @Test
    void newScheduledAndWalkInAppointmentsStartPending() {
        assertEquals(Appointment.AppointmentStatus.PENDING,
                service.initialStatus(Appointment.AppointmentCategory.SCHEDULED));
        assertEquals(Appointment.AppointmentStatus.PENDING,
                service.initialStatus(Appointment.AppointmentCategory.WALK_IN));
    }

    private void assertAllowed(Appointment.AppointmentCategory category,
                               Appointment.AppointmentStatus current,
                               Appointment.AppointmentStatus target) {
        Appointment appointment = Appointment.builder()
                .appointmentCategory(category)
                .status(current)
                .build();
        service.transition(appointment, target);
        assertEquals(target, appointment.getStatus());
    }

    private void assertRejected(Appointment.AppointmentCategory category,
                                Appointment.AppointmentStatus current,
                                Appointment.AppointmentStatus target) {
        assertThrows(IllegalStateException.class,
                () -> service.validateTransition(category, current, target));
    }
}
