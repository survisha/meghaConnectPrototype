package com.survisha.meghaconnect.service;

import com.survisha.common.sms.SmsResponse;
import com.survisha.common.sms.SmsService;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsAppointmentNotificationService implements AppointmentNotificationService {

    private static final DateTimeFormatter SMS_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final SmsService smsService;

    @Override
    public void appointmentSubmitted(Appointment appointment) {
        send("APPOINTMENT_SUBMITTED", appointment,
                () -> smsService.sendAppointmentCreatedSms(mobileNumber(appointment), appointmentReference(appointment)));
    }

    @Override
    public void appointmentSelectedForPublicDarbar(Appointment appointment) {
        send("SELECTED_FOR_PUBLIC_DARBAR", appointment,
                () -> smsService.sendFollowupScheduledSms(
                        mobileNumber(appointment),
                        appointmentReference(appointment),
                        scheduledDate(appointment)));
    }

    @Override
    public void publicDarbarAppointmentScheduled(Appointment appointment) {
        send("PUBLIC_DARBAR_APPOINTMENT_SCHEDULED", appointment,
                () -> smsService.sendFollowupScheduledSms(
                        mobileNumber(appointment),
                        appointmentReference(appointment),
                        scheduledDate(appointment)));
    }

    @Override
    public void normalAppointmentApproved(Appointment appointment) {
        send("NORMAL_APPOINTMENT_APPROVED", appointment,
                () -> smsService.sendAppointmentApprovedSms(mobileNumber(appointment), appointmentReference(appointment)));
    }

    @Override
    public void appointmentRejected(Appointment appointment) {
        send("APPOINTMENT_REJECTED", appointment,
                () -> smsService.sendAppointmentRejectedSms(mobileNumber(appointment), appointmentReference(appointment)));
    }

    private void send(String event, Appointment appointment, Supplier<SmsResponse> sender) {
        try {
            SmsResponse response = sender.get();
            log.info("Notification SMS event={} appointmentId={} recipientPhone={} sent={}",
                    event,
                    appointment != null ? appointment.getId() : null,
                    RequestContextUtil.maskPhone(mobileNumber(appointment)),
                    response != null && response.isSuccess());
        } catch (Exception e) {
            log.error("Notification SMS failed event={} appointmentId={} recipientPhone={}",
                    event,
                    appointment != null ? appointment.getId() : null,
                    RequestContextUtil.maskPhone(mobileNumber(appointment)),
                    e);
        }
    }

    private String mobileNumber(Appointment appointment) {
        if (appointment == null) {
            return "";
        }
        Visitor applicant = appointment.getApplicant();
        if (applicant != null && applicant.getPhoneNumber() != null) {
            return applicant.getPhoneNumber();
        }
        return appointment.getGuestMobile() != null ? appointment.getGuestMobile() : "";
    }

    private String appointmentReference(Appointment appointment) {
        if (appointment == null) {
            return "N/A";
        }
        if (appointment.getApplicationId() != null && !appointment.getApplicationId().trim().isEmpty()) {
            return appointment.getApplicationId();
        }
        if (appointment.getGuestReferenceId() != null && !appointment.getGuestReferenceId().trim().isEmpty()) {
            return appointment.getGuestReferenceId();
        }
        return appointment.getId() != null ? "APT" + appointment.getId() : "N/A";
    }

    private String scheduledDate(Appointment appointment) {
        if (appointment == null || appointment.getScheduledDateTime() == null) {
            return "the scheduled date";
        }
        return appointment.getScheduledDateTime().format(SMS_DATE_FORMAT);
    }
}
