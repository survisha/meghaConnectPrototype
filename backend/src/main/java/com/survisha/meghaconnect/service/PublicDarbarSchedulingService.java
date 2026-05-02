package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.BatchScheduleResult;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.PublicDarbar;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicDarbarSchedulingService {

    private final PublicDarbarService publicDarbarService;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentAuditService appointmentAuditService;
    private final AppointmentNotificationService notificationService;

    @Value("${meghaconnect.public-darbar.start-time:10:00}")
    private String publicDarbarStartTime;

    @Value("${meghaconnect.public-darbar.slot-duration-minutes:10}")
    private int publicDarbarSlotDurationMinutes;

    @Transactional
    public BatchScheduleResult scheduleSelectedForDarbar(Long publicDarbarId,
                                                         String actor,
                                                         String actorRole,
                                                         String jobId,
                                                         boolean failWhenNone) {
        String resolvedJobId = jobId != null ? jobId : "PD-" + UUID.randomUUID();
        PublicDarbar darbar = publicDarbarService.findActiveDarbar(publicDarbarId);
        List<Appointment> selectedAppointments = appointmentRepository.findByStatusOrderByCreatedAtAsc(
                Appointment.AppointmentStatus.SELECTED_FOR_PUBLIC_DARBAR
        );

        if (selectedAppointments.isEmpty() && failWhenNone) {
            throw workflowException(
                    ErrorCodeConstants.APPT_NO_APPOINTMENTS_FOR_SCHEDULING,
                    ErrorCodeConstants.APPT_NO_APPOINTMENTS_FOR_SCHEDULING_MSG,
                    HttpStatus.NOT_FOUND
            );
        }

        int existingScheduled = appointmentRepository.findByPublicDarbar_IdOrderByPublicDarbarTokenNumberAsc(publicDarbarId).size();
        int availableSlots = Math.max(0, darbar.getMaxSlots() - existingScheduled);
        int skippedCount = 0;
        List<Long> scheduledIds = new ArrayList<>();
        LocalTime startTime = LocalTime.parse(publicDarbarStartTime);
        int nextToken = nextTokenNumber(publicDarbarId);

        log.info("Public Darbar scheduling batch started jobId={} publicDarbarId={} selectedCount={} availableSlots={}",
                resolvedJobId, publicDarbarId, selectedAppointments.size(), availableSlots);

        for (Appointment appointment : selectedAppointments) {
            if (availableSlots <= 0) {
                skippedCount++;
                continue;
            }
            if (appointment.getScheduledDateTime() != null || appointment.getPublicDarbar() != null) {
                skippedCount++;
                continue;
            }

            Appointment.AppointmentStatus oldStatus = appointment.getStatus();
            int tokenNumber = nextToken++;
            LocalDateTime slotDateTime = darbar.getDarbarDate()
                    .atTime(startTime)
                    .plusMinutes((long) (tokenNumber - 1) * publicDarbarSlotDurationMinutes);

            appointment.setPublicDarbar(darbar);
            appointment.setPublicDarbarTokenNumber(tokenNumber);
            appointment.setScheduledDateTime(slotDateTime);
            appointment.setScheduledDurationMinutes(publicDarbarSlotDurationMinutes);
            appointment.setStatus(Appointment.AppointmentStatus.SCHEDULED_FOR_PUBLIC_DARBAR);
            appointment.setApprovedBy(actor);
            appointment.setUpdatedBy(actor);

            Appointment saved = appointmentRepository.save(appointment);
            appointmentAuditService.recordStatusChange(
                    saved,
                    oldStatus,
                    saved.getStatus(),
                    "SCHEDULED_FOR_PUBLIC_DARBAR",
                    "Scheduled by Public Darbar batch job " + resolvedJobId,
                    actor,
                    actorRole
            );
            notificationService.publicDarbarAppointmentScheduled(saved);
            scheduledIds.add(saved.getId());
            availableSlots--;
        }

        log.info("Public Darbar scheduling batch completed jobId={} publicDarbarId={} scheduledCount={} skippedCount={}",
                resolvedJobId, publicDarbarId, scheduledIds.size(), skippedCount);
        return BatchScheduleResult.builder()
                .jobId(resolvedJobId)
                .publicDarbarId(publicDarbarId)
                .selectedCount(selectedAppointments.size())
                .scheduledCount(scheduledIds.size())
                .skippedCount(skippedCount)
                .scheduledAppointmentIds(scheduledIds)
                .build();
    }

    private int nextTokenNumber(Long publicDarbarId) {
        return appointmentRepository.findByPublicDarbar_IdOrderByPublicDarbarTokenNumberAsc(publicDarbarId)
                .stream()
                .map(Appointment::getPublicDarbarTokenNumber)
                .filter(token -> token != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private MeghaConnectException workflowException(String code, String message, HttpStatus status) {
        return new MeghaConnectException(code, message, status.value());
    }
}
