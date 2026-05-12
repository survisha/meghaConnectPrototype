package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.PublicDarbar;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AppointmentWorkflowService {

    private static final int DEFAULT_DURATION_MINUTES = 30;

    private static final List<Appointment.AppointmentStatus> REVIEWABLE_STATUSES = Arrays.asList(
            Appointment.AppointmentStatus.CREATED,
            Appointment.AppointmentStatus.SUBMITTED,
            Appointment.AppointmentStatus.PENDING_APPROVER_REVIEW,
            Appointment.AppointmentStatus.CMO_REVIEW,
            Appointment.AppointmentStatus.APPROVER_REVIEW
    );

    private static final List<Appointment.AppointmentStatus> FOLLOWUP_STATUSES = Arrays.asList(
            Appointment.AppointmentStatus.FOLLOWUP,
            Appointment.AppointmentStatus.SELECTED_FOR_PUBLIC_DARBAR
    );

    private static final List<Appointment.AppointmentStatus> SCHEDULED_STATUSES = Arrays.asList(
            Appointment.AppointmentStatus.SCHEDULED,
            Appointment.AppointmentStatus.SCHEDULED_FOR_PUBLIC_DARBAR,
            Appointment.AppointmentStatus.APPROVED_WITH_DATE_TIME
    );

    private static final List<Appointment.AppointmentStatus> TERMINAL_STATUSES = Arrays.asList(
            Appointment.AppointmentStatus.COMPLETED,
            Appointment.AppointmentStatus.CANCELLED,
            Appointment.AppointmentStatus.REJECTED,
            Appointment.AppointmentStatus.HCM_REJECTED
    );

    private final AppointmentRepository appointmentRepository;
    private final VisitorRepository visitorRepository;
    private final AppointmentAuditService appointmentAuditService;
    private final AppointmentNotificationService notificationService;

    @Transactional
    public AppointmentWorkflowResponse createDraft(CitizenAppointmentRequest request,
                                                   Long authenticatedVisitorId,
                                                   String actor,
                                                   String actorRole) {
        ensureCitizen(actorRole);
        Long applicantId = resolveApplicantId(request.getApplicantId(), authenticatedVisitorId);
        Visitor applicant = visitorRepository.findById(applicantId)
                .orElseThrow(() -> workflowException(
                        ErrorCodeConstants.VISITOR_NOT_FOUND,
                        ErrorCodeConstants.format(ErrorCodeConstants.VISITOR_NOT_FOUND_MSG, applicantId),
                        HttpStatus.NOT_FOUND
                ));

        Appointment appointment = Appointment.builder()
                .applicationId(generateApplicationId())
                .applicant(applicant)
                .eventType(request.getEventType())
                .subject(trimToNull(request.getSubject()))
                .department(trimToNull(request.getDepartment()))
                .appointmentType(trimToNull(request.getAppointmentType()))
                .agendaType(trimToNull(firstNonBlank(request.getAgendaType(), request.getAppointmentType())))
                .agendaBrief(trimToNull(request.getDescription()))
                .status(Appointment.AppointmentStatus.CREATED)
                .requestedLocation(request.getRequestedLocation())
                .mlaMdcApproved(Boolean.TRUE.equals(request.getMlaMdcApproved()))
                .isWalkIn(Boolean.TRUE.equals(request.getWalkIn()))
                .build();
        appointment.setCreatedBy(actor);
        appointment.setUpdatedBy(actor);

        Appointment saved = appointmentRepository.save(appointment);
        appointmentAuditService.recordStatusChange(
                saved,
                null,
                saved.getStatus(),
                "CREATED",
                "Citizen created appointment draft",
                actor,
                actorRole
        );
        log.info("Citizen appointment draft created appointmentId={} applicationId={}",
                saved.getId(), saved.getApplicationId());
        return toResponse(saved);
    }

    @Transactional
    public AppointmentWorkflowResponse updateDraft(Long id,
                                                   CitizenAppointmentRequest request,
                                                   Long authenticatedVisitorId,
                                                   String actor,
                                                   String actorRole) {
        ensureCitizen(actorRole);
        Appointment appointment = findOwnedAppointment(id, authenticatedVisitorId, request.getApplicantId());
        ensureStatus(appointment, "UPDATE_DRAFT", Appointment.AppointmentStatus.CREATED);

        appointment.setEventType(request.getEventType());
        appointment.setSubject(trimToNull(request.getSubject()));
        appointment.setDepartment(trimToNull(request.getDepartment()));
        appointment.setAppointmentType(trimToNull(request.getAppointmentType()));
        appointment.setAgendaType(trimToNull(firstNonBlank(request.getAgendaType(), request.getAppointmentType())));
        appointment.setAgendaBrief(trimToNull(request.getDescription()));
        appointment.setRequestedLocation(request.getRequestedLocation());
        appointment.setMlaMdcApproved(Boolean.TRUE.equals(request.getMlaMdcApproved()));
        appointment.setIsWalkIn(Boolean.TRUE.equals(request.getWalkIn()));
        appointment.setUpdatedBy(actor);

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Citizen appointment draft updated appointmentId={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public AppointmentWorkflowResponse submit(Long id,
                                              Long authenticatedVisitorId,
                                              String actor,
                                              String actorRole) {
        ensureCitizen(actorRole);
        Appointment appointment = findOwnedAppointment(id, authenticatedVisitorId, null);
        ensureStatus(appointment, "SUBMIT", Appointment.AppointmentStatus.CREATED, Appointment.AppointmentStatus.SUBMITTED);

        Appointment.AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setStatus(Appointment.AppointmentStatus.PENDING_APPROVER_REVIEW);
        appointment.setUpdatedBy(actor);
        Appointment saved = appointmentRepository.save(appointment);
        appointmentAuditService.recordStatusChange(
                saved,
                oldStatus,
                saved.getStatus(),
                "SUBMITTED",
                "Citizen submitted appointment for approver review",
                actor,
                actorRole
        );
        notificationService.appointmentSubmitted(saved);
        log.info("Citizen appointment submitted appointmentId={} oldStatus={} newStatus={}",
                saved.getId(), oldStatus, saved.getStatus());
        return toResponse(saved);
    }

    public List<AppointmentWorkflowResponse> getCitizenAppointments(Long authenticatedVisitorId,
                                                                    Long requestedApplicantId,
                                                                    String actorRole) {
        ensureCitizen(actorRole);
        Long applicantId = resolveApplicantId(requestedApplicantId, authenticatedVisitorId);
        return appointmentRepository.findByApplicant_IdOrderByCreatedAtDesc(applicantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AppointmentWorkflowResponse getCitizenAppointment(Long id,
                                                            Long authenticatedVisitorId,
                                                            String actorRole) {
        ensureCitizen(actorRole);
        return toResponse(findOwnedAppointment(id, authenticatedVisitorId, null));
    }

    public List<AppointmentWorkflowResponse> findPendingForApprover(String district,
                                                                    String department,
                                                                    Appointment.AppointmentStatus status,
                                                                    String appointmentType,
                                                                    LocalDate fromDate,
                                                                    LocalDate toDate,
                                                                    String search,
                                                                    String actorRole) {
        ensureApprover(actorRole);
        Specification<Appointment> spec = workflowFilter(district, department, status, appointmentType, fromDate, toDate, search);
        List<Appointment> appointments = appointmentRepository.findAll(spec);
        log.info("Approver appointments fetched count={} statusFilter={}",
                appointments.size(), status != null ? status : "DEFAULT_PENDING");
        return appointments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentWorkflowResponse markForPublicDarbar(Long id,
                                                           MarkPublicDarbarRequest request,
                                                           String actor,
                                                           String actorRole) {
        return markFollowUp(id, request, actor, actorRole);
    }

    @Transactional
    public AppointmentWorkflowResponse markFollowUp(Long id,
                                                    MarkPublicDarbarRequest request,
                                                    String actor,
                                                    String actorRole) {
        ensureApprover(actorRole);
        Appointment appointment = findAppointment(id);
        ensureMutable(appointment, "MARK_FOLLOWUP");
        ensureNotScheduled(appointment);
        ensureB1Appointment(appointment, "Only B1 Public Durbar appointments can be marked for follow-up.");

        if (!FOLLOWUP_STATUSES.contains(appointment.getStatus())) {
            ensureStatusIn(appointment, "MARK_FOLLOWUP", REVIEWABLE_STATUSES);
        }

        Appointment.AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setStatus(Appointment.AppointmentStatus.FOLLOWUP);
        appointment.setSelectedForPublicDarbarBy(actor);
        appointment.setSelectedForPublicDarbarAt(LocalDateTime.now());
        appointment.setApproverRemarks(request != null ? RequestContextUtil.sanitizeForLog(request.getRemarks()) : null);
        appointment.setUpdatedBy(actor);

        Appointment saved = appointmentRepository.save(appointment);
        appointmentAuditService.recordStatusChange(
                saved,
                oldStatus,
                saved.getStatus(),
                "FOLLOWUP",
                request != null ? request.getRemarks() : null,
                actor,
                actorRole
        );
        notificationService.appointmentSelectedForPublicDarbar(saved);
        log.info("Appointment marked for follow-up appointmentId={} oldStatus={} newStatus={}",
                saved.getId(), oldStatus, saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public AppointmentWorkflowResponse approveWithDateTime(Long id,
                                                           ApproveAppointmentRequest request,
                                                           String actor,
                                                           String actorRole) {
        ensureApprover(actorRole);
        Appointment appointment = findAppointment(id);
        ensureMutable(appointment, "APPROVE_WITH_DATE_TIME");
        ensureStatusIn(appointment, "APPROVE_WITH_DATE_TIME", REVIEWABLE_STATUSES);
        ensureNotScheduled(appointment);
        validateScheduleDateTime(request.getScheduledDateTime());

        Appointment.AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setScheduledDateTime(request.getScheduledDateTime());
        appointment.setScheduledDurationMinutes(request.getDurationMinutes() != null
                ? request.getDurationMinutes()
                : DEFAULT_DURATION_MINUTES);
        appointment.setStatus(Appointment.AppointmentStatus.APPROVED_WITH_DATE_TIME);
        appointment.setApprovedBy(actor);
        appointment.setApproverRemarks(RequestContextUtil.sanitizeForLog(request.getRemarks()));
        appointment.setUpdatedBy(actor);

        Appointment saved = appointmentRepository.save(appointment);
        appointmentAuditService.recordStatusChange(
                saved,
                oldStatus,
                saved.getStatus(),
                "APPROVED_WITH_DATE_TIME",
                request.getRemarks(),
                actor,
                actorRole
        );
        notificationService.normalAppointmentApproved(saved);
        log.info("Appointment approved with date/time appointmentId={} scheduledDateTime={}",
                saved.getId(), saved.getScheduledDateTime());
        return toResponse(saved);
    }

    @Transactional
    public AppointmentWorkflowResponse reject(Long id,
                                              RejectAppointmentRequest request,
                                              String actor,
                                              String actorRole) {
        ensureApprover(actorRole);
        if (request == null || isBlank(request.getReason())) {
            throw workflowException(
                    ErrorCodeConstants.APPT_REJECTION_REASON_REQUIRED,
                    ErrorCodeConstants.APPT_REJECTION_REASON_REQUIRED_MSG,
                    HttpStatus.BAD_REQUEST
            );
        }

        Appointment appointment = findAppointment(id);
        ensureMutable(appointment, "REJECT");

        Appointment.AppointmentStatus oldStatus = appointment.getStatus();
        appointment.setStatus(Appointment.AppointmentStatus.REJECTED);
        appointment.setRejectedBy(actor);
        appointment.setRejectionReason(RequestContextUtil.sanitizeForLog(request.getReason()));
        appointment.setUpdatedBy(actor);

        Appointment saved = appointmentRepository.save(appointment);
        appointmentAuditService.recordStatusChange(
                saved,
                oldStatus,
                saved.getStatus(),
                "REJECTED",
                request.getReason(),
                actor,
                actorRole
        );
        notificationService.appointmentRejected(saved);
        log.info("Appointment rejected appointmentId={} oldStatus={} newStatus={}",
                saved.getId(), oldStatus, saved.getStatus());
        return toResponse(saved);
    }

    public List<AppointmentWorkflowResponse> getSelectedPublicDarbarAppointments(String actorRole) {
        ensureApprover(actorRole);
        return appointmentRepository.findByStatusIn(FOLLOWUP_STATUSES)
                .stream()
                .sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentWorkflowResponse> getPublicDarbarAppointments(Long publicDarbarId) {
        return appointmentRepository.findByPublicDarbar_IdOrderByPublicDarbarTokenNumberAsc(publicDarbarId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AppointmentWorkflowResponse toResponse(Appointment appointment) {
        Visitor applicant = appointment.getApplicant();
        PublicDarbar darbar = appointment.getPublicDarbar();
        return AppointmentWorkflowResponse.builder()
                .id(appointment.getId())
                .applicationId(appointment.getApplicationId())
                .applicantId(applicant != null ? applicant.getId() : null)
                .applicantName(applicant != null ? applicant.getFullName() : null)
                .applicantMobile(applicant != null ? RequestContextUtil.maskPhone(applicant.getPhoneNumber()) : null)
                .district(applicant != null ? applicant.getDistrict() : null)
                .department(appointment.getDepartment())
                .subject(firstNonBlank(appointment.getSubject(), appointment.getAgendaType()))
                .description(appointment.getAgendaBrief())
                .appointmentType(appointment.getAppointmentType())
                .agendaType(appointment.getAgendaType())
                .eventType(appointment.getEventType())
                .status(appointment.getStatus())
                .requestedLocation(appointment.getRequestedLocation())
                .scheduledDateTime(appointment.getScheduledDateTime())
                .scheduledDurationMinutes(appointment.getScheduledDurationMinutes())
                .publicDarbarId(darbar != null ? darbar.getId() : null)
                .publicDarbarDate(darbar != null ? darbar.getDarbarDate() : null)
                .publicDarbarLocation(darbar != null ? darbar.getLocation() : null)
                .publicDarbarTokenNumber(appointment.getPublicDarbarTokenNumber())
                .rejectionReason(appointment.getRejectionReason())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }

    private Specification<Appointment> workflowFilter(String district,
                                                      String department,
                                                      Appointment.AppointmentStatus status,
                                                      String appointmentType,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,
                                                      String search) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Appointment, Visitor> applicant = root.join("applicant", JoinType.LEFT);
            javax.persistence.criteria.Predicate predicate = cb.conjunction();

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            } else {
                predicate = cb.and(predicate, root.get("status").in(REVIEWABLE_STATUSES));
            }
            if (!isBlank(district)) {
                predicate = cb.and(predicate, cb.equal(cb.lower(applicant.get("district")), district.trim().toLowerCase(Locale.ROOT)));
            }
            if (!isBlank(department)) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("department")), department.trim().toLowerCase(Locale.ROOT)));
            }
            if (!isBlank(appointmentType)) {
                String value = appointmentType.trim().toLowerCase(Locale.ROOT);
                predicate = cb.and(predicate, cb.or(
                        cb.equal(cb.lower(root.get("appointmentType")), value),
                        cb.equal(cb.lower(root.get("agendaType")), value)
                ));
            }
            if (fromDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicate = cb.and(predicate, cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }
            if (!isBlank(search)) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(applicant.get("fullName")), pattern),
                        cb.like(cb.lower(applicant.get("phoneNumber")), pattern),
                        cb.like(cb.lower(root.get("applicationId")), pattern)
                ));
            }
            query.orderBy(cb.asc(root.get("createdAt")));
            return predicate;
        };
    }

    private Appointment findOwnedAppointment(Long id, Long authenticatedVisitorId, Long requestedApplicantId) {
        Long applicantId = resolveApplicantId(requestedApplicantId, authenticatedVisitorId);
        return appointmentRepository.findByIdAndApplicant_Id(id, applicantId)
                .orElseThrow(() -> workflowException(
                        ErrorCodeConstants.APPT_NOT_FOUND,
                        ErrorCodeConstants.APPT_NOT_FOUND_MSG,
                        HttpStatus.NOT_FOUND
                ));
    }

    private Appointment findAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> workflowException(
                        ErrorCodeConstants.APPT_NOT_FOUND,
                        ErrorCodeConstants.APPT_NOT_FOUND_MSG,
                        HttpStatus.NOT_FOUND
                ));
    }

    private Long resolveApplicantId(Long requestedApplicantId, Long authenticatedVisitorId) {
        if (authenticatedVisitorId != null && requestedApplicantId != null && !authenticatedVisitorId.equals(requestedApplicantId)) {
            throw workflowException(
                    ErrorCodeConstants.APPT_UNAUTHORIZED_ACTION,
                    ErrorCodeConstants.APPT_UNAUTHORIZED_ACTION_MSG,
                    HttpStatus.FORBIDDEN
            );
        }
        Long applicantId = authenticatedVisitorId != null ? authenticatedVisitorId : requestedApplicantId;
        if (applicantId == null) {
            throw workflowException(
                    ErrorCodeConstants.APPT_UNAUTHORIZED_ACTION,
                    "Citizen applicant could not be resolved",
                    HttpStatus.FORBIDDEN
            );
        }
        return applicantId;
    }

    private void ensureCitizen(String actorRole) {
        if (!hasRole(actorRole, "PUBLIC", "CITIZEN")) {
            throw workflowException(
                    ErrorCodeConstants.APPT_UNAUTHORIZED_ACTION,
                    ErrorCodeConstants.APPT_UNAUTHORIZED_ACTION_MSG,
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void ensureApprover(String actorRole) {
        if (!hasRole(actorRole, "CMO", "CMO_OFFICER", "OSD", "APPROVER")) {
            throw workflowException(
                    ErrorCodeConstants.APPT_UNAUTHORIZED_ACTION,
                    ErrorCodeConstants.APPT_UNAUTHORIZED_ACTION_MSG,
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private boolean hasRole(String actorRole, String... allowedRoles) {
        if (actorRole == null) {
            return false;
        }
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equalsIgnoreCase(actorRole)) {
                return true;
            }
        }
        return false;
    }

    private void ensureStatus(Appointment appointment, String action, Appointment.AppointmentStatus... allowedStatuses) {
        ensureStatusIn(appointment, action, Arrays.asList(allowedStatuses));
    }

    private void ensureStatusIn(Appointment appointment, String action, Collection<Appointment.AppointmentStatus> allowedStatuses) {
        if (!allowedStatuses.contains(appointment.getStatus())) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_STATUS,
                    ErrorCodeConstants.APPT_INVALID_STATUS_MSG + ": " + action,
                    HttpStatus.CONFLICT
            );
        }
    }

    private void ensureMutable(Appointment appointment, String action) {
        if (TERMINAL_STATUSES.contains(appointment.getStatus())) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_STATUS,
                    ErrorCodeConstants.APPT_INVALID_STATUS_MSG + ": " + action,
                    HttpStatus.CONFLICT
            );
        }
    }

    private void ensureNotScheduled(Appointment appointment) {
        if (appointment.getScheduledDateTime() != null || SCHEDULED_STATUSES.contains(appointment.getStatus())) {
            throw workflowException(
                    ErrorCodeConstants.APPT_ALREADY_SCHEDULED,
                    ErrorCodeConstants.APPT_ALREADY_SCHEDULED_MSG,
                    HttpStatus.CONFLICT
            );
        }
    }

    private void ensureB1Appointment(Appointment appointment, String message) {
        if (appointment.getEventType() != Appointment.EventType.B1) {
            throw workflowException(
                    ErrorCodeConstants.INVALID_FIELD_VALUE,
                    message,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateScheduleDateTime(LocalDateTime scheduledDateTime) {
        if (scheduledDateTime == null || scheduledDateTime.isBefore(LocalDateTime.now())) {
            throw workflowException(
                    ErrorCodeConstants.APPT_INVALID_SCHEDULE_DATE_TIME,
                    ErrorCodeConstants.APPT_INVALID_SCHEDULE_DATE_TIME_MSG,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String generateApplicationId() {
        long count = appointmentRepository.count() + 1;
        return "MC-" + LocalDate.now().getYear() + "-" + String.format("%06d", count);
    }

    private String firstNonBlank(String primary, String fallback) {
        return !isBlank(primary) ? primary : fallback;
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private MeghaConnectException workflowException(String code, String message, HttpStatus status) {
        return new MeghaConnectException(code, message, status.value());
    }
}
