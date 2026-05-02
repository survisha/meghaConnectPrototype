package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;

public interface AppointmentNotificationService {

    void appointmentSubmitted(Appointment appointment);

    void appointmentSelectedForPublicDarbar(Appointment appointment);

    void publicDarbarAppointmentScheduled(Appointment appointment);

    void normalAppointmentApproved(Appointment appointment);

    void appointmentRejected(Appointment appointment);
}
