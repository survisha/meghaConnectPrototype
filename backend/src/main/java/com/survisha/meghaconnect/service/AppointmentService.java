package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AppointmentDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final VisitorRepository visitorRepository;
    private final AuditLogService auditLogService;

    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    public Optional<Appointment> findById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Optional<Appointment> findByApplicationId(String appId) {
        return appointmentRepository.findByApplicationId(appId);
    }

    @Transactional
    public Appointment create(AppointmentDto dto, String createdBy) {
        Visitor applicant = visitorRepository.findById(dto.getApplicantId())
            .orElseThrow(() -> new VisitorNotFoundException(dto.getApplicantId()));

        int meetingCount = appointmentRepository.countMeetingsLast6Months(
            applicant.getId(), LocalDateTime.now().minusMonths(6)
        );

        String appId = generateApplicationId();

        Appointment appt = Appointment.builder()
            .applicationId(appId)
            .applicant(applicant)
            .eventType(dto.getEventType())
            .agendaType(dto.getAgendaType())
            .agendaBrief(dto.getAgendaBrief())
            .status(Appointment.AppointmentStatus.SUBMITTED)
            .requestedLocation(dto.getRequestedLocation())
            .mlaMdcApproved(dto.getMlaMdcApproved())
            .isWalkIn(Boolean.TRUE.equals(dto.getIsWalkIn()))
            .meetingCountLast6Months(meetingCount)
            .build();

        Appointment saved = appointmentRepository.save(appt);
        auditLogService.log("Appointment", saved.getId(), "CREATED",
            "New appointment created: " + appId, createdBy);
        return saved;
    }

    @Transactional
    public Appointment updateStatus(Long id, Appointment.AppointmentStatus newStatus,
                                    String remarks, String updatedBy) {
        Appointment appt = appointmentRepository.findById(id)
            .orElseThrow(() -> new AppointmentNotFoundException(id));

        Appointment.AppointmentStatus oldStatus = appt.getStatus();
        appt.setStatus(newStatus);
        if (remarks != null) appt.setCmoRemarks(remarks);

        Appointment saved = appointmentRepository.save(appt);
        auditLogService.log("Appointment", saved.getId(), "STATUS_CHANGE",
            "Status: " + oldStatus + " → " + newStatus, updatedBy);
        return saved;
    }

    @Transactional
    public Appointment schedule(Long id, LocalDateTime dateTime, int durationMinutes,
                                String updatedBy) {
        Appointment appt = appointmentRepository.findById(id)
            .orElseThrow(() -> new AppointmentNotFoundException(id));

        // Conflict check
        List<Appointment> conflicts = appointmentRepository.findByStatus(
            Appointment.AppointmentStatus.SCHEDULED
        );
        boolean hasConflict = conflicts.stream().anyMatch(a ->
            a.getScheduledDateTime() != null && !a.getId().equals(id) &&
            a.getScheduledDateTime().isBefore(dateTime.plusMinutes(durationMinutes)) &&
            dateTime.isBefore(a.getScheduledDateTime().plusMinutes(
                a.getScheduledDurationMinutes() != null ? a.getScheduledDurationMinutes() : 30
            ))
        );
        if (hasConflict) {
            throw new SchedulingConflictException(dateTime);
        }

        appt.setScheduledDateTime(dateTime);
        appt.setScheduledDurationMinutes(durationMinutes);
        appt.setStatus(Appointment.AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepository.save(appt);
        auditLogService.log("Appointment", saved.getId(), "SCHEDULED",
            "Scheduled for: " + dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), updatedBy);
        return saved;
    }

    private String generateApplicationId() {
        long count = appointmentRepository.count() + 1;
        return "MC-" + LocalDateTime.now().getYear() + "-" + String.format("%05d", count);
    }
}
