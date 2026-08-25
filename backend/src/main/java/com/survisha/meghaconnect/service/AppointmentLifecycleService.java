package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single authority for the frozen scheduled-appointment and walk-in lifecycles.
 * Legacy and Public Darbar states remain readable but are not valid transition targets.
 */
@Service
public class AppointmentLifecycleService {

    private static final Map<Appointment.AppointmentStatus, Set<Appointment.AppointmentStatus>> SCHEDULED_TRANSITIONS =
            transitions(
                    transition(Appointment.AppointmentStatus.PENDING,
                            Appointment.AppointmentStatus.SCHEDULED,
                            Appointment.AppointmentStatus.PENDING_REQUEST,
                            Appointment.AppointmentStatus.COMPLETED,
                            Appointment.AppointmentStatus.REJECTED,
                            Appointment.AppointmentStatus.ROUTED_TO_OFFICIAL),
                    transition(Appointment.AppointmentStatus.PENDING_REQUEST,
                            Appointment.AppointmentStatus.PENDING,
                            Appointment.AppointmentStatus.REJECTED),
                    transition(Appointment.AppointmentStatus.SCHEDULED,
                            Appointment.AppointmentStatus.RESCHEDULED,
                            Appointment.AppointmentStatus.PENDING_REQUEST,
                            Appointment.AppointmentStatus.COMPLETED,
                            Appointment.AppointmentStatus.REJECTED),
                    transition(Appointment.AppointmentStatus.RESCHEDULED,
                            Appointment.AppointmentStatus.RESCHEDULED,
                            Appointment.AppointmentStatus.PENDING_REQUEST,
                            Appointment.AppointmentStatus.COMPLETED,
                            Appointment.AppointmentStatus.REJECTED),
                    transition(Appointment.AppointmentStatus.COMPLETED,
                            Appointment.AppointmentStatus.CLOSED)
            );

    private static final Map<Appointment.AppointmentStatus, Set<Appointment.AppointmentStatus>> WALK_IN_TRANSITIONS =
            transitions(
                    transition(Appointment.AppointmentStatus.PENDING,
                            Appointment.AppointmentStatus.COMPLETED,
                            Appointment.AppointmentStatus.PENDING_REQUEST,
                            Appointment.AppointmentStatus.REJECTED),
                    transition(Appointment.AppointmentStatus.PENDING_REQUEST,
                            Appointment.AppointmentStatus.PENDING,
                            Appointment.AppointmentStatus.REJECTED),
                    transition(Appointment.AppointmentStatus.COMPLETED,
                            Appointment.AppointmentStatus.CLOSED)
            );

    public void transition(Appointment appointment, Appointment.AppointmentStatus targetStatus) {
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment is required.");
        }
        validateTransition(appointment.getAppointmentCategory(), appointment.getStatus(), targetStatus);
        appointment.setStatus(targetStatus);
    }

    public void validateTransition(Appointment.AppointmentCategory category,
                                   Appointment.AppointmentStatus currentStatus,
                                   Appointment.AppointmentStatus targetStatus) {
        if (category == null || currentStatus == null || targetStatus == null) {
            throw new IllegalArgumentException("Appointment category, current status and target status are required.");
        }
        if (category == Appointment.AppointmentCategory.PUBLIC_DARBAR) {
            throw new IllegalStateException("Public Darbar transitions remain in the existing Public Darbar workflow.");
        }

        Map<Appointment.AppointmentStatus, Set<Appointment.AppointmentStatus>> rules =
                category == Appointment.AppointmentCategory.WALK_IN
                        ? WALK_IN_TRANSITIONS
                        : SCHEDULED_TRANSITIONS;
        if (!rules.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new IllegalStateException("Invalid " + category + " appointment transition: "
                    + currentStatus + " -> " + targetStatus);
        }
    }

    public Appointment.AppointmentStatus initialStatus(Appointment.AppointmentCategory category) {
        if (category == null || category == Appointment.AppointmentCategory.PUBLIC_DARBAR) {
            throw new IllegalArgumentException("A scheduled or walk-in category is required.");
        }
        return Appointment.AppointmentStatus.PENDING;
    }

    @SafeVarargs
    private static Map<Appointment.AppointmentStatus, Set<Appointment.AppointmentStatus>> transitions(
            Map.Entry<Appointment.AppointmentStatus, Set<Appointment.AppointmentStatus>>... entries) {
        Map<Appointment.AppointmentStatus, Set<Appointment.AppointmentStatus>> result =
                new EnumMap<>(Appointment.AppointmentStatus.class);
        for (Map.Entry<Appointment.AppointmentStatus, Set<Appointment.AppointmentStatus>> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(result);
    }

    private static Map.Entry<Appointment.AppointmentStatus, Set<Appointment.AppointmentStatus>> transition(
            Appointment.AppointmentStatus source,
            Appointment.AppointmentStatus firstTarget,
            Appointment.AppointmentStatus... additionalTargets) {
        EnumSet<Appointment.AppointmentStatus> targets = EnumSet.of(firstTarget, additionalTargets);
        return Map.entry(source, Set.copyOf(targets));
    }
}
