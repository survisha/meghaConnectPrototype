package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NoopAppointmentNotificationService implements AppointmentNotificationService {

    @Override
    public void appointmentSubmitted(Appointment appointment) {
        logNotification("APPOINTMENT_SUBMITTED", appointment);
    }

    @Override
    public void appointmentSelectedForPublicDarbar(Appointment appointment) {
        logNotification("SELECTED_FOR_PUBLIC_DARBAR", appointment);
    }

    @Override
    public void publicDarbarAppointmentScheduled(Appointment appointment) {
        logNotification("PUBLIC_DARBAR_APPOINTMENT_SCHEDULED", appointment);
    }

    @Override
    public void normalAppointmentApproved(Appointment appointment) {
        logNotification("NORMAL_APPOINTMENT_APPROVED", appointment);
    }

    @Override
    public void appointmentRejected(Appointment appointment) {
        logNotification("APPOINTMENT_REJECTED", appointment);
    }

    private void logNotification(String event, Appointment appointment) {
        String maskedPhone = appointment.getApplicant() != null
                ? RequestContextUtil.maskPhone(appointment.getApplicant().getPhoneNumber())
                : "";
        log.info("Notification stub event={} appointmentId={} recipientPhone={}",
                event, appointment.getId(), maskedPhone);
        // TODO: integrate SMS, WhatsApp, and optional email provider here.
    }
}
