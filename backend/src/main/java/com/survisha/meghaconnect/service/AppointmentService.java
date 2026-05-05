package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AppointmentDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.exception.*;
import com.survisha.meghaconnect.util.ValidationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private static final List<Appointment.AppointmentStatus> DEO_VISIBLE_STATUSES = Arrays.asList(
        Appointment.AppointmentStatus.CREATED,
        Appointment.AppointmentStatus.SUBMITTED,
        Appointment.AppointmentStatus.DEO_PROCESSED,
        Appointment.AppointmentStatus.PENDING_APPROVER_REVIEW,
        Appointment.AppointmentStatus.CMO_REVIEW,
        Appointment.AppointmentStatus.APPROVER_REVIEW,
        Appointment.AppointmentStatus.HCM_PENDING,
        Appointment.AppointmentStatus.SELECTED_FOR_PUBLIC_DARBAR,
        Appointment.AppointmentStatus.APPROVED_WITH_DATE_TIME,
        Appointment.AppointmentStatus.SCHEDULED
    );

    private static final List<Appointment.AppointmentStatus> APPROVER_VISIBLE_STATUSES = Arrays.asList(
        Appointment.AppointmentStatus.CREATED,
        Appointment.AppointmentStatus.SUBMITTED,
        Appointment.AppointmentStatus.PENDING_APPROVER_REVIEW,
        Appointment.AppointmentStatus.CMO_REVIEW,
        Appointment.AppointmentStatus.APPROVER_REVIEW,
        Appointment.AppointmentStatus.HCM_PENDING,
        Appointment.AppointmentStatus.SELECTED_FOR_PUBLIC_DARBAR,
        Appointment.AppointmentStatus.APPROVED_WITH_DATE_TIME,
        Appointment.AppointmentStatus.SCHEDULED
    );

    private final AppointmentRepository appointmentRepository;
    private final VisitorRepository visitorRepository;
    private final AuditLogService auditLogService;
    private final RequestValidationService validationService;

    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    public Page<AppointmentDto> findAllDtos(Pageable pageable) {
        return appointmentRepository.findAll(pageable).map(this::toDto);
    }

    public Optional<Appointment> findById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Optional<Appointment> findByApplicationId(String appId) {
        return appointmentRepository.findByApplicationId(appId);
    }

    public List<AppointmentDto> findMyAppointments(Long visitorId) {
        if (visitorId == null) {
            return List.of();
        }
        return appointmentRepository.findByApplicant_IdOrderByCreatedAtDesc(visitorId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public Page<AppointmentDto> findForDeo(Pageable pageable) {
        return appointmentRepository.findByStatusIn(DEO_VISIBLE_STATUSES, pageable).map(this::toDto);
    }

    public Page<AppointmentDto> findForApprover(Pageable pageable) {
        return appointmentRepository.findByStatusIn(APPROVER_VISIBLE_STATUSES, pageable).map(this::toDto);
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
            .subject(dto.getSubject())
            .department(dto.getDepartment())
            .appointmentType(dto.getAppointmentType())
            .agendaType(dto.getAgendaType())
            .agendaBrief(dto.getAgendaBrief())
            .status(Appointment.AppointmentStatus.SUBMITTED)
            .requestedLocation(dto.getRequestedLocation())
            .mlaMdcApproved(dto.getMlaMdcApproved())
            .isWalkIn(Boolean.TRUE.equals(dto.getIsWalkIn()))
            .meetingCountLast6Months(meetingCount)
            .build();
        appt.setCreatedBy(createdBy);
        appt.setUpdatedBy(createdBy);

        Appointment saved = appointmentRepository.save(appt);
        auditLogService.log("Appointment", saved.getId(), "CREATED",
            "New appointment created: " + appId, createdBy);
        return saved;
    }

    public AppointmentDto toDto(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        Visitor applicant = appointment.getApplicant();
        com.survisha.meghaconnect.dto.VisitorDto applicantDto = applicant == null ? null
            : com.survisha.meghaconnect.dto.VisitorDto.builder()
                .id(applicant.getId())
                .fullName(applicant.getFullName())
                .phoneNumber(applicant.getPhoneNumber())
                .epicNumber(applicant.getEpicNumber())
                .aadhaarNumber(applicant.getAadhaarNumber())
                .kycType(applicant.getKycType())
                .kycVerified(applicant.getKycVerified())
                .designation(applicant.getDesignation())
                .addressLine(firstNonBlank(applicant.getAddressLine(), applicant.getAddress()))
                .district(applicant.getDistrict())
                .constituency(applicant.getConstituency())
                .booth(applicant.getBooth())
                .boothVillage(applicant.getBoothVillage())
                .village(applicant.getVillage())
                .outsideMeghalaya(applicant.getOutsideMeghalaya())
                .location(applicant.getLocation())
                .briefProfile(applicant.getBriefProfile())
                .photoStoragePath(applicant.getPhotoStoragePath())
                .livePhotoPath(applicant.getLivePhotoPath())
                .build();

        return AppointmentDto.builder()
            .id(appointment.getId())
            .applicationId(appointment.getApplicationId())
            .applicantId(applicant != null ? applicant.getId() : null)
            .applicant(applicantDto)
            .applicantName(applicant != null ? applicant.getFullName() : null)
            .applicantPhone(applicant != null ? applicant.getPhoneNumber() : null)
            .eventType(appointment.getEventType())
            .subject(appointment.getSubject())
            .department(appointment.getDepartment())
            .appointmentType(appointment.getAppointmentType())
            .agendaType(appointment.getAgendaType())
            .agendaBrief(appointment.getAgendaBrief())
            .status(appointment.getStatus())
            .requestedLocation(appointment.getRequestedLocation())
            .scheduledDateTime(appointment.getScheduledDateTime())
            .scheduledDurationMinutes(appointment.getScheduledDurationMinutes())
            .mlaMdcApproved(appointment.getMlaMdcApproved())
            .cmoRemarks(appointment.getCmoRemarks())
            .approverRemarks(appointment.getApproverRemarks())
            .hcmRemarks(appointment.getHcmRemarks())
            .shortNotes(appointment.getShortNotes())
            .isWalkIn(appointment.getIsWalkIn())
            .meetingCountLast6Months(appointment.getMeetingCountLast6Months())
            .createdAt(appointment.getCreatedAt())
            .updatedAt(appointment.getUpdatedAt())
            .build();
    }

    @Transactional
    public Appointment updateStatus(Long id, Map<String, String> body, String updatedBy) {
        Appointment.AppointmentStatus status = validationService.requireEnum(
                body != null ? body.get(ValidationConstants.FIELD_STATUS) : null,
                Appointment.AppointmentStatus.class,
                ValidationConstants.FIELD_STATUS
        );
        String remarks = body != null ? body.get("remarks") : null;
        return updateStatus(id, status, remarks, updatedBy);
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
    public Appointment schedule(Long id, Map<String, Object> body, String updatedBy) {
        LocalDateTime dateTime = validationService.requireDateTime(
                body != null ? body.get(ValidationConstants.FIELD_SCHEDULED_DATE_TIME) : null,
                ValidationConstants.FIELD_SCHEDULED_DATE_TIME
        );
        int duration = validationService.requireInteger(
                body != null ? body.get(ValidationConstants.FIELD_DURATION_MINUTES) : null,
                ValidationConstants.FIELD_DURATION_MINUTES
        );
        return schedule(id, dateTime, duration, updatedBy);
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

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.trim().isEmpty() ? primary : fallback;
    }
}
