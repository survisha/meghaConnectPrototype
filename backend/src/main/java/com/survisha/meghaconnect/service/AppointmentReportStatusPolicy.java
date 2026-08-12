package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AppointmentReportStatusPolicy {
    private static final Set<Appointment.AppointmentStatus> COMPLETED = Set.of(
            Appointment.AppointmentStatus.HCM_MET_COMPLETED,
            Appointment.AppointmentStatus.COMPLETED);
    private static final Set<Appointment.AppointmentStatus> REJECTED = Set.of(
            Appointment.AppointmentStatus.REJECTED);

    public Set<Appointment.AppointmentStatus> completedStatuses() { return COMPLETED; }
    public Set<Appointment.AppointmentStatus> rejectedStatuses() { return REJECTED; }

    public boolean isCompleted(Appointment appointment) {
        if (appointment == null || !COMPLETED.contains(appointment.getStatus())) return false;
        if (appointment.getAppointmentCategory() == Appointment.AppointmentCategory.SCHEDULED) {
            return appointment.getStatus() == Appointment.AppointmentStatus.HCM_MET_COMPLETED
                    || appointment.getStatus() == Appointment.AppointmentStatus.COMPLETED;
        }
        return appointment.getStatus() == Appointment.AppointmentStatus.COMPLETED;
    }
}
