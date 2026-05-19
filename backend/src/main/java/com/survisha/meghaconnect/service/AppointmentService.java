package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AppointmentDto;
import com.survisha.meghaconnect.dto.AppointmentDocumentDto;
import com.survisha.meghaconnect.dto.AppointmentMultipartRequest;
import com.survisha.meghaconnect.dto.AssociateVisitorDto;
import com.survisha.meghaconnect.dto.GuestAppointmentRequest;
import com.survisha.meghaconnect.dto.GuestAppointmentResponse;
import com.survisha.meghaconnect.entity.AssociateMapping;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.DocumentUpload;
import com.survisha.meghaconnect.entity.SchemeApplication;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.entity.WalkIn;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.AssociateMappingRepository;
import com.survisha.meghaconnect.repository.DocumentUploadRepository;
import com.survisha.meghaconnect.repository.ScheduleEventRepository;
import com.survisha.meghaconnect.repository.SchemeApplicationRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.repository.WalkInRepository;
import com.survisha.meghaconnect.exception.*;
import com.survisha.meghaconnect.util.DateTimeUtil;
import com.survisha.meghaconnect.util.ValidationConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AppointmentService {

    private static final List<Appointment.AppointmentStatus> DEO_VISIBLE_STATUSES = Arrays.asList(
        Appointment.AppointmentStatus.CREATED,
        Appointment.AppointmentStatus.SUBMITTED,
        Appointment.AppointmentStatus.DEO_PROCESSED,
        Appointment.AppointmentStatus.PENDING_APPROVER_REVIEW,
        Appointment.AppointmentStatus.CMO_REVIEW,
        Appointment.AppointmentStatus.APPROVER_REVIEW,
        Appointment.AppointmentStatus.APPROVED,
        Appointment.AppointmentStatus.HCM_PENDING,
        Appointment.AppointmentStatus.FOLLOWUP,
        Appointment.AppointmentStatus.SELECTED_FOR_PUBLIC_DARBAR,
        Appointment.AppointmentStatus.APPROVED_WITH_DATE_TIME,
        Appointment.AppointmentStatus.SCHEDULED
    );

    private static final List<Appointment.AppointmentStatus> APPROVER_VISIBLE_STATUSES = Arrays.asList(
        Appointment.AppointmentStatus.APPROVER_REVIEW,
        Appointment.AppointmentStatus.APPROVED,
        Appointment.AppointmentStatus.HCM_ACCEPTED,
        Appointment.AppointmentStatus.FORWARDED_TO_DEPARTMENT,
        Appointment.AppointmentStatus.FOLLOWUP,
        Appointment.AppointmentStatus.SCHEDULED,
        Appointment.AppointmentStatus.COMPLETED,
        Appointment.AppointmentStatus.HCM_REJECTED
    );

    private static final Set<String> DUPLICATE_ALLOWED_FINAL_SCHEME_STATUSES = Set.of(
        "REJECTED",
        "HCM_REJECTED",
        "CANCELLED",
        "CANCELED",
        "COMPLETED",
        "CLOSED"
    );

    private static final Set<Appointment.AppointmentStatus> QR_GENERATING_STATUSES = Set.of(
        Appointment.AppointmentStatus.APPROVED_WITH_DATE_TIME,
        Appointment.AppointmentStatus.SCHEDULED_FOR_PUBLIC_DARBAR,
        Appointment.AppointmentStatus.SCHEDULED
    );

    private final AppointmentRepository appointmentRepository;
    private final AssociateMappingRepository associateMappingRepository;
    private final VisitorRepository visitorRepository;
    private final DocumentUploadRepository documentUploadRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final SchemeApplicationRepository schemeApplicationRepository;
    private final AuditLogService auditLogService;
    private final RequestValidationService validationService;
    private final FileStorageService fileStorageService;
    private final AppointmentDocumentAiNotesService appointmentDocumentAiNotesService;
    private final QrTokenService qrTokenService;
    private final WalkInRepository walkInRepository;
    private final WalkInTokenService walkInTokenService;
    private final ObjectMapper objectMapper;

    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    public Page<AppointmentDto> findAllDtos(Pageable pageable) {
        return appointmentRepository.findAll(pageable).map(this::toDto);
    }

    public Page<AppointmentDto> findAllDtos(String status, Pageable pageable) {
        return findAllDtos(status, null, null, pageable);
    }

    public Page<AppointmentDto> findAllDtos(String status, String source, String referredOffice, Pageable pageable) {
        List<Appointment.AppointmentStatus> statuses = parseStatuses(status);
        String sourceValue = normalizeSource(source);
        String referredOfficeValue = trimToNull(referredOffice);
        if (statuses.isEmpty() && sourceValue == null && referredOfficeValue == null) {
            return findAllDtos(pageable);
        }
        Specification<Appointment> spec = (root, query, cb) -> {
            javax.persistence.criteria.Predicate predicate = cb.conjunction();
            if (!statuses.isEmpty()) {
                javax.persistence.criteria.Predicate statusPredicate = root.get("status").in(statuses);
                javax.persistence.criteria.Predicate walkInSourcePredicate =
                        cb.equal(cb.upper(root.get("appointmentSource")), "WALKIN");
                if ("WALKIN".equals(sourceValue)) {
                    statusPredicate = cb.conjunction();
                } else if (sourceValue == null) {
                    statusPredicate = cb.or(statusPredicate, walkInSourcePredicate);
                }
                predicate = cb.and(predicate, statusPredicate);
            }
            if (sourceValue != null) {
                predicate = cb.and(predicate, cb.equal(cb.upper(root.get("appointmentSource")), sourceValue));
            }
            if (referredOfficeValue != null) {
                predicate = cb.and(predicate, cb.equal(cb.upper(root.get("referredOffice")), referredOfficeValue.toUpperCase()));
            }
            return predicate;
        };
        return appointmentRepository.findAll(spec, pageable).map(this::toDto);
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
    public List<AppointmentDto> markFollowup(List<Long> appointmentIds, String remarks, String updatedBy) {
        List<Long> ids = uniqueIds(appointmentIds);
        if (ids.isEmpty()) {
            throw new MeghaConnectException(ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "appointmentIds"),
                HttpStatus.BAD_REQUEST.value());
        }

        List<Appointment> appointments = appointmentRepository.findAllById(ids);
        if (appointments.size() != ids.size()) {
            throw new AppointmentNotFoundException(ids.get(0));
        }

        for (Appointment appointment : appointments) {
            ensureTransition(appointment, Appointment.AppointmentStatus.APPROVED, "Only APPROVED applications can be marked as FOLLOWUP.");
            appointment.setStatus(Appointment.AppointmentStatus.FOLLOWUP);
            appointment.setApproverRemarks(firstNonBlank(remarks, "Follow-up"));
            appointment.setUpdatedBy(updatedBy);
        }

        return appointmentRepository.saveAll(appointments)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public List<AppointmentDocumentDto> findDocumentDtos(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new AppointmentNotFoundException(appointmentId);
        }
        return documentUploadRepository.findByAppointmentId(appointmentId)
            .stream()
            .map(this::toDocumentDto)
            .toList();
    }

    @Transactional
    public GuestAppointmentResponse createGuestAppointment(GuestAppointmentRequest request) {
        GuestAppointmentRequest safeRequest = request != null ? request : new GuestAppointmentRequest();
        String fullName = validationService.requireText(safeRequest.getFullName(), "fullName");
        String mobileNumber = validationService.requirePhone(safeRequest.getMobileNumber());
        String address = validationService.requireText(safeRequest.getAddress(), "address");
        String referredOffice = validationService.requireText(safeRequest.getReferredOffice(), "referredOffice");
        String reason = validationService.requireText(safeRequest.getReasonForAppointment(), "reasonForAppointment");
        if (reason.length() < 10) {
            throw new MeghaConnectException(ErrorCodeConstants.INVALID_FIELD_VALUE,
                "Reason for appointment must be at least 10 characters.", HttpStatus.BAD_REQUEST.value());
        }
        String guestPhotoPath = storeGuestPhotoIfPresent(safeRequest.getLivePhotoBase64());

        Visitor visitor = Visitor.builder()
            .fullName(fullName)
            .phoneNumber(mobileNumber)
            .email(trimToNull(safeRequest.getEmail()))
            .designation(trimToNull(safeRequest.getDesignation()))
            .address(address)
            .addressLine(address)
            .fullAddress(address)
            .kycType("NONE")
            .kycVerified(false)
            .kycStatus("NOT_VERIFIED")
            .photoStoragePath(guestPhotoPath)
            .livePhotoPath(guestPhotoPath)
            .build();
        Visitor savedVisitor = visitorRepository.save(visitor);

        String referenceId = generateGuestReferenceId();
        Appointment appointment = Appointment.builder()
            .applicationId(referenceId)
            .applicant(savedVisitor)
            .eventType(Appointment.EventType.A4)
            .agendaType("Guest Appointment")
            .subject("Guest Appointment - " + fullName)
            .appointmentType("Guest Appointment")
            .agendaBrief(reason)
            .status(Appointment.AppointmentStatus.SUBMITTED)
            .requestedLocation(Appointment.MeetingLocation.OTHERS)
            .mlaMdcApproved(false)
            .isWalkIn(false)
            .aiDuplicateFlag(false)
            .meetingCountLast6Months(0)
            .appointmentSource("GUEST")
            .guestReferenceId(referenceId)
            .guestName(fullName)
            .guestMobile(mobileNumber)
            .guestAddress(address)
            .guestEmail(trimToNull(safeRequest.getEmail()))
            .organizationName(trimToNull(safeRequest.getOrganizationName()))
            .guestDesignation(trimToNull(safeRequest.getDesignation()))
            .visitorCategory(trimToNull(safeRequest.getVisitorCategory()))
            .referredOffice(referredOffice)
            .referredByName(trimToNull(safeRequest.getReferredByName()))
            .reasonForAppointment(reason)
            .preferredDate(safeRequest.getPreferredDate())
            .shortNotes(trimToNull(safeRequest.getRemarks()))
            .build();
        appointment.setCreatedBy("guest");
        appointment.setUpdatedBy("guest");
        Appointment saved = appointmentRepository.save(appointment);

        if (safeRequest.getSupportingDocument() != null && !safeRequest.getSupportingDocument().isEmpty()) {
            uploadSupportingDocument(saved.getId(), safeRequest.getSupportingDocument(), "guest");
        }

        auditLogService.log("Appointment", saved.getId(), "GUEST_SUBMITTED",
            "Guest appointment submitted: " + referenceId, "guest");
        return GuestAppointmentResponse.builder()
            .referenceId(referenceId)
            .status(saved.getStatus().name())
            .message("Guest appointment submitted successfully")
            .build();
    }

    @Transactional
    public Appointment reschedule(Long id, Map<String, Object> body, String updatedBy) {
        Appointment appt = appointmentRepository.findById(id)
            .orElseThrow(() -> new AppointmentNotFoundException(id));
        LocalDateTime scheduledDateTime = parseScheduleDateTime(body);
        if (scheduledDateTime.isBefore(DateTimeUtil.nowIST())) {
            throw new MeghaConnectException(ErrorCodeConstants.APPT_INVALID_SCHEDULE_DATE_TIME,
                "Appointments cannot be scheduled or rescheduled to a past date/time.", HttpStatus.BAD_REQUEST.value());
        }
        Long eventId = longValue(body != null ? body.get("eventId") : null);
        if (eventId != null) {
            scheduleEventRepository.findById(eventId).ifPresent(appt::setScheduleEvent);
        }
        appt.setScheduledDateTime(scheduledDateTime);
        if (appt.getScheduledDurationMinutes() == null) {
            appt.setScheduledDurationMinutes(30);
        }
        appt.setStatus(Appointment.AppointmentStatus.SCHEDULED);
        appt.setUpdatedBy(updatedBy);
        Appointment saved = appointmentRepository.save(appt);
        generateQrIfApproved(saved, updatedBy);
        auditLogService.log("Appointment", saved.getId(), "RESCHEDULED",
            "Rescheduled for: " + scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), updatedBy);
        return saved;
    }

    @Transactional
    public AppointmentDocumentDto uploadSupportingDocument(Long appointmentId, MultipartFile file, String actor) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        if (file == null || file.isEmpty()) {
            throw new MeghaConnectException(ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "file"),
                HttpStatus.BAD_REQUEST.value());
        }
        try {
            FileStorageService.StoredFileMetadata storedFile =
                fileStorageService.storeFileSecure(file, appointment.getApplicant().getId(), appointment.getApplicationId());
            LocalDateTime now = DateTimeUtil.nowIST();
            DocumentUpload document = DocumentUpload.builder()
                .appointment(appointment)
                .visitor(appointment.getApplicant())
                .documentType("SUPPORTING_DOCUMENT")
                .originalFilename(storedFile.getOriginalFileName())
                .storedFileName(storedFile.getStoredFileName())
                .filePath(storedFile.getEncryptedFilePath())
                .encryptedFilePath(storedFile.getEncryptedFilePath())
                .secureHash(storedFile.getSecureHash())
                .fileSizeBytes(storedFile.getFileSize())
                .mimeType(storedFile.getContentType())
                .contentType(storedFile.getContentType())
                .uploadedBy(actor)
                .uploadedDate(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
            DocumentUpload saved = documentUploadRepository.save(document);
            queueAiNotes(saved, actor);
            return toDocumentDto(saved);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to store supporting document.", e);
        }
    }

    @Transactional
    public Appointment create(AppointmentDto dto, String createdBy) {
        Visitor applicant = visitorRepository.findById(dto.getApplicantId())
            .orElseThrow(() -> new VisitorNotFoundException(dto.getApplicantId()));

        int meetingCount = appointmentRepository.countMeetingsLast6Months(
            applicant.getId(), DateTimeUtil.nowIST().minusMonths(6)
        );

        String appId = generateApplicationId();

        Appointment appt = Appointment.builder()
            .applicationId(appId)
            .applicant(applicant)
            .eventType(dto.getEventType())
            .subject(dto.getSubject())
            .department(dto.getDepartment())
            .appointmentType(dto.getAppointmentType())
            .appointmentSource("CITIZEN")
            .agendaType(dto.getAgendaType())
            .agendaBrief(dto.getAgendaBrief())
            .status(Appointment.AppointmentStatus.SUBMITTED)
            .requestedLocation(dto.getRequestedLocation())
            .mlaMdcApproved(dto.getMlaMdcApproved())
            .isWalkIn(Boolean.TRUE.equals(dto.getIsWalkIn()))
            .aiDuplicateFlag(false)
            .meetingCountLast6Months(meetingCount)
            .build();
        appt.setCreatedBy(createdBy);
        appt.setUpdatedBy(createdBy);

        Appointment saved = appointmentRepository.save(appt);
        saveAppointmentAssociates(toAssociatesJson(dto.getAssociates()), saved, applicant, createdBy);
        auditLogService.log("Appointment", saved.getId(), "CREATED",
            "New appointment created: " + appId, createdBy);
        return saved;
    }

    @Transactional
    public Appointment createPilotImportedAppointment(Visitor applicant, String agendaBrief, String createdBy) {
        if (applicant == null || applicant.getId() == null) {
            throw new IllegalArgumentException("Imported visitor must be saved before creating an appointment.");
        }

        String actor = firstNonBlank(createdBy, "pilot-import");
        AppointmentDto dto = AppointmentDto.builder()
                .applicantId(applicant.getId())
                .eventType(Appointment.EventType.B1)
                .agendaType("Public Darbar")
                .agendaBrief(trimToNull(agendaBrief))
                .requestedLocation(Appointment.MeetingLocation.OTHERS)
                .mlaMdcApproved(false)
                .isWalkIn(true)
                .build();
        return create(dto, actor);
    }

    @Transactional
    public Map<String, Object> createMultipart(
            AppointmentMultipartRequest form,
            HttpServletRequest request,
            String createdBy) {

        AppointmentMultipartRequest safeForm = form != null ? form : new AppointmentMultipartRequest();
        Visitor applicant = resolveAppointmentApplicant(
                safeForm.getApplicantId(),
                safeForm.getApplicantName(),
                safeForm.getApplicantPhone(),
                safeForm.getEpicNumber());
        String actor = resolveAppointmentActor(createdBy, applicant);
        String agendaTypeValue = validationService.requireText(safeForm.getAgendaType(), "agendaType");
        Appointment.MeetingLocation location = parseMeetingLocation(safeForm.getRequestedLocation());
        boolean walkIn = Boolean.TRUE.equals(safeForm.getIsWalkIn());
        Appointment.EventType parsedEventType = walkIn ? Appointment.EventType.B2 : parseEventType(safeForm.getEventType());
        SchemeApplication.SchemeType schemeType = parseSchemeType(safeForm.getSchemeType());
        ensureSchemeApplicationIsNotDuplicate(applicant, schemeType);
        String appId = generatePublicApplicationId();

        int meetingCount = appointmentRepository.countMeetingsLast6Months(
                applicant.getId(), DateTimeUtil.nowIST().minusMonths(6));

        Appointment appt = Appointment.builder()
            .applicationId(appId)
            .applicant(applicant)
            .eventType(parsedEventType)
            .agendaType(agendaTypeValue)
            .agendaBrief(safeForm.getAgendaBrief())
            .appointmentSource(walkIn ? "WALKIN" : "CITIZEN")
            .appointmentType(walkIn ? "B2 Walk-in" : null)
            .status(walkIn ? Appointment.AppointmentStatus.SCHEDULED : Appointment.AppointmentStatus.SUBMITTED)
            .requestedLocation(location)
            .mlaMdcApproved(safeForm.getMlaMdcApproved() != null && safeForm.getMlaMdcApproved())
            .isWalkIn(walkIn)
            .scheduledDateTime(walkIn ? DateTimeUtil.nowIST() : null)
            .scheduledDurationMinutes(walkIn ? 15 : null)
            .aiDuplicateFlag(false)
            .meetingCountLast6Months(meetingCount)
            .build();
        appt.setCreatedBy(actor);
        appt.setUpdatedBy(actor);

        if (safeForm.getAiSummary() != null && !safeForm.getAiSummary().trim().isEmpty()) {
            appt.setAiSummary(safeForm.getAiSummary().trim());
        }
        if (safeForm.getAiPriorityLevel() != null && !safeForm.getAiPriorityLevel().trim().isEmpty()) {
            appt.setAiPriorityLevel(safeForm.getAiPriorityLevel().trim());
        }

        Appointment saved = appointmentRepository.save(appt);
        saveSchemeApplicationIfRequired(safeForm, saved, applicant, schemeType, actor);
        saveAppointmentDocuments(request, saved, applicant, appId, actor);
        saveAppointmentAssociates(safeForm.getAssociates(), saved, applicant, actor);
        WalkIn walkInRow = walkIn ? saveWalkIn(saved, applicant, safeForm, actor) : null;

        auditLogService.log("Appointment", saved.getId(), "SUBMITTED",
            "Appointment submitted: " + appId, actor);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", saved.getId());
        response.put("applicationId", appId);
        if (walkInRow != null) {
            response.put("tokenNumber", walkInRow.getTokenNumber());
            response.put("walkInTokenNumber", walkInRow.getTokenNumber());
        }
        response.put("status", saved.getStatus().name());
        response.put("message", "Appointment request submitted successfully.");
        return response;
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
                .aadhaarNumber(maskAadhaarForResponse(applicant.getAadhaarNumber()))
                .kycType(applicant.getKycType())
                .kycVerified(applicant.getKycVerified())
                .kycStatus(applicant.getKycStatus())
                .dateOfBirth(applicant.getDateOfBirth())
                .gender(applicant.getGender())
                .designation(applicant.getDesignation())
                .address(applicant.getAddress())
                .fullAddress(applicant.getFullAddress())
                .address1(applicant.getAddress1())
                .addressLine(firstNonBlank(applicant.getAddressLine(), applicant.getAddress()))
                .city(applicant.getCity())
                .state(applicant.getState())
                .pincode(applicant.getPincode())
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
                .photoPath(applicant.getPhotoPath())
                .createdAt(applicant.getCreatedAt())
                .updatedAt(applicant.getUpdatedAt())
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
            .appointmentSource(appointment.getAppointmentSource())
            .guestReferenceId(appointment.getGuestReferenceId())
            .guestName(appointment.getGuestName())
            .guestMobile(appointment.getGuestMobile())
            .guestAddress(appointment.getGuestAddress())
            .guestEmail(appointment.getGuestEmail())
            .organizationName(appointment.getOrganizationName())
            .guestDesignation(appointment.getGuestDesignation())
            .visitorCategory(appointment.getVisitorCategory())
            .referredOffice(appointment.getReferredOffice())
            .referredByName(appointment.getReferredByName())
            .reasonForAppointment(appointment.getReasonForAppointment())
            .preferredDate(appointment.getPreferredDate())
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
            .walkInTokenNumber(walkInRepository.findByAppointment_Id(appointment.getId())
                    .map(WalkIn::getTokenNumber)
                    .orElse(null))
            .meetingCountLast6Months(appointment.getMeetingCountLast6Months())
            .associates(toAssociateDtos(appointment))
            .createdAt(appointment.getCreatedAt())
            .updatedAt(appointment.getUpdatedAt())
            .build();
    }

    public List<AssociateVisitorDto> toAssociateDtos(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            return List.of();
        }
        return associateMappingRepository.findByAppointment_IdOrderByCreatedAtAsc(appointment.getId())
                .stream()
                .map(mapping -> toAssociateDto(mapping, "ASSOCIATE"))
                .toList();
    }

    private AssociateVisitorDto toAssociateDto(AssociateMapping mapping, String role) {
        if (mapping == null) {
            return null;
        }
        Visitor citizen = mapping.getPerson();
        return AssociateVisitorDto.builder()
                .id(mapping.getId())
                .citizenId(citizen != null ? citizen.getId() : null)
                .fullName(citizen != null ? citizen.getFullName() : mapping.getAssociateName())
                .mobileNumber(citizen != null ? citizen.getPhoneNumber() : mapping.getAssociatePhone())
                .epicReference(maskReference(citizen != null ? citizen.getEpicNumber() : mapping.getAssociateEpic()))
                .aadhaarReference(citizen != null ? firstNonBlank(citizen.getMaskedIdentityNumber(), maskAadhaarForResponse(citizen.getAadhaarNumber())) : null)
                .addressSummary(citizen != null
                        ? firstNonBlank(citizen.getFullAddress(), citizen.getAddress(), citizen.getAddressLine(), citizen.getLocation())
                        : mapping.getAssociateAddress())
                .photoUrl(citizen != null ? firstNonBlank(citizen.getLivePhotoPath(), citizen.getPhotoStoragePath(), citizen.getPhotoPath()) : null)
                .kycStatus(citizen != null ? firstNonBlank(citizen.getKycStatus(), "PENDING") : null)
                .status("ACTIVE")
                .relationship(mapping.getRelationship())
                .remarks(mapping.getRelationship())
                .role(role)
                .createdAt(mapping.getCreatedAt())
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
        validateStatusTransition(appt, newStatus);
        appt.setStatus(newStatus);
        if (remarks != null) {
            if (newStatus == Appointment.AppointmentStatus.APPROVER_REVIEW || newStatus == Appointment.AppointmentStatus.CMO_REVIEW) {
                appt.setCmoRemarks(remarks);
            } else {
                appt.setApproverRemarks(remarks);
            }
        }
        appt.setUpdatedBy(updatedBy);

        Appointment saved = appointmentRepository.save(appt);
        generateQrIfApproved(saved, updatedBy);
        auditLogService.log("Appointment", saved.getId(), "STATUS_CHANGE",
            "Status: " + oldStatus + " → " + newStatus, updatedBy);
        return saved;
    }

    @Transactional
    public Appointment submitCmoReview(Long id, Map<String, Object> body, String updatedBy) {
        Appointment appt = appointmentRepository.findById(id)
            .orElseThrow(() -> new AppointmentNotFoundException(id));

        Appointment.AppointmentStatus oldStatus = appt.getStatus();
        Appointment.AppointmentStatus newStatus = validationService.requireEnum(
                firstNonBlank(
                        bodyValue(body, ValidationConstants.FIELD_STATUS),
                        Appointment.AppointmentStatus.CMO_REVIEW.name()
                ),
                Appointment.AppointmentStatus.class,
                ValidationConstants.FIELD_STATUS
        );
        String eventType = bodyValue(body, "eventType");
        String requestedLocation = bodyValue(body, "requestedLocation");
        String remarks = firstNonBlank(
                bodyValue(body, "cmoRemarks"),
                bodyValue(body, "remarks"),
                bodyValue(body, "pendingInformation")
        );

        if (eventType != null && !eventType.trim().isEmpty()) {
            appt.setEventType(parseEventType(eventType));
        }
        if (requestedLocation != null && !requestedLocation.trim().isEmpty()) {
            appt.setRequestedLocation(parseMeetingLocation(requestedLocation));
        }
        if (remarks != null) {
            appt.setCmoRemarks(remarks);
        }
        validateStatusTransition(appt, newStatus);
        appt.setStatus(newStatus);
        appt.setUpdatedBy(updatedBy);

        Appointment saved = appointmentRepository.save(appt);
        String action = newStatus == Appointment.AppointmentStatus.CMO_REVIEW
                ? "MISSING_INFORMATION_REQUESTED"
                : "CMO_REVIEW_SUBMITTED";
        auditLogService.log("Appointment", saved.getId(), action,
            "CMO review status: " + oldStatus + " -> " + newStatus, updatedBy);
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
        if (dateTime == null || dateTime.isBefore(DateTimeUtil.nowIST())) {
            throw new MeghaConnectException(ErrorCodeConstants.APPT_INVALID_SCHEDULE_DATE_TIME,
                "Appointments cannot be scheduled in the past.", HttpStatus.BAD_REQUEST.value());
        }
        if (appt.getStatus() != Appointment.AppointmentStatus.APPROVED
                && appt.getStatus() != Appointment.AppointmentStatus.FOLLOWUP
                && appt.getStatus() != Appointment.AppointmentStatus.SCHEDULED
                && !(isGuestAppointment(appt) && appt.getStatus() == Appointment.AppointmentStatus.SUBMITTED)) {
            invalidTransition("Only APPROVED, FOLLOWUP, or SCHEDULED applications can be scheduled.");
        }

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
        generateQrIfApproved(saved, updatedBy);
        auditLogService.log("Appointment", saved.getId(), "SCHEDULED",
            "Scheduled for: " + dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), updatedBy);
        return saved;
    }

    private void generateQrIfApproved(Appointment appointment, String actor) {
        if (appointment != null
                && appointment.getScheduledDateTime() != null
                && QR_GENERATING_STATUSES.contains(appointment.getStatus())) {
            qrTokenService.generateForApprovedAppointment(appointment, actor);
        }
    }

    private String generateApplicationId() {
        long count = appointmentRepository.count() + 1;
        return "MC-" + DateTimeUtil.nowIST().getYear() + "-" + String.format("%05d", count);
    }

    private String generateGuestReferenceId() {
        return "GA-" + DateTimeUtil.nowIST().getYear() + "-" + String.format("%05d", appointmentRepository.count() + 1);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String bodyValue(Map<String, Object> body, String key) {
        if (body == null || key == null || !body.containsKey(key) || body.get(key) == null) {
            return null;
        }
        String value = body.get(key).toString().trim();
        return value.isEmpty() ? null : value;
    }

    private LocalDateTime parseScheduleDateTime(Map<String, Object> body) {
        Object scheduledDateTime = body != null ? body.get(ValidationConstants.FIELD_SCHEDULED_DATE_TIME) : null;
        if (scheduledDateTime != null && !scheduledDateTime.toString().trim().isEmpty()) {
            return validationService.requireDateTime(scheduledDateTime, ValidationConstants.FIELD_SCHEDULED_DATE_TIME);
        }
        String date = bodyValue(body, "scheduledDate");
        String time = bodyValue(body, "scheduledTime");
        if (date == null || time == null) {
            throw new MeghaConnectException(ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                "scheduledDate and scheduledTime are required.", HttpStatus.BAD_REQUEST.value());
        }
        return LocalDateTime.parse(date + "T" + time);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeSource(String source) {
        String value = trimToNull(source);
        if (value == null) {
            return null;
        }
        return value.toUpperCase();
    }

    private boolean isGuestAppointment(Appointment appointment) {
        return appointment != null && "GUEST".equalsIgnoreCase(appointment.getAppointmentSource());
    }

    private List<Appointment.AppointmentStatus> parseStatuses(String status) {
        if (status == null || status.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(status.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(value -> validationService.requireEnum(value, Appointment.AppointmentStatus.class, "status"))
            .distinct()
            .toList();
    }

    private List<Long> uniqueIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
    }

    private void validateStatusTransition(Appointment appointment, Appointment.AppointmentStatus newStatus) {
        if (newStatus == Appointment.AppointmentStatus.APPROVED) {
            if (appointment.getStatus() != Appointment.AppointmentStatus.APPROVER_REVIEW
                    && appointment.getStatus() != Appointment.AppointmentStatus.APPROVED) {
                invalidTransition("Only APPROVER_REVIEW applications can be approved by Approver.");
            }
            return;
        }
        if (newStatus == Appointment.AppointmentStatus.APPROVER_REVIEW) {
            if (appointment.getStatus() != Appointment.AppointmentStatus.SUBMITTED
                    && appointment.getStatus() != Appointment.AppointmentStatus.CMO_REVIEW
                    && appointment.getStatus() != Appointment.AppointmentStatus.APPROVER_REVIEW) {
                invalidTransition("Only SUBMITTED or CMO_REVIEW applications can be forwarded to Approver.");
            }
            return;
        }
        if (newStatus == Appointment.AppointmentStatus.FOLLOWUP) {
            ensureTransition(appointment, Appointment.AppointmentStatus.APPROVED,
                "Only APPROVED applications can be marked as FOLLOWUP.");
            return;
        }
        if (newStatus == Appointment.AppointmentStatus.SCHEDULED) {
            ensureTransition(appointment, Appointment.AppointmentStatus.FOLLOWUP,
                "Only FOLLOWUP applications can be assigned to an event.");
        }
    }

    private void ensureTransition(Appointment appointment, Appointment.AppointmentStatus expectedStatus, String message) {
        if (appointment.getStatus() != expectedStatus) {
            invalidTransition(message);
        }
    }

    private void invalidTransition(String message) {
        throw new MeghaConnectException(ErrorCodeConstants.APPT_INVALID_STATUS, message, HttpStatus.CONFLICT.value());
    }

    private String maskAadhaarForResponse(String aadhaarNumber) {
        String normalized = trimToNull(aadhaarNumber);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= 4) {
            return "XXXX-XXXX-" + normalized;
        }
        return "XXXX-XXXX-" + normalized.substring(normalized.length() - 4);
    }

    private String maskReference(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() <= 4) {
            return normalized;
        }
        return "XXXX" + normalized.substring(normalized.length() - 4);
    }

    private AppointmentDocumentDto toDocumentDto(DocumentUpload document) {
        Appointment appointment = document.getAppointment();
        return AppointmentDocumentDto.builder()
            .id(document.getId())
            .appointmentId(appointment != null ? appointment.getId() : null)
            .documentType(document.getDocumentType())
            .fileName(firstNonBlank(document.getOriginalFilename(), document.getDocumentType()))
            .filePath(null)
            .fileSize(document.getFileSizeBytes())
            .mimeType(firstNonBlank(document.getContentType(), document.getMimeType()))
            .uploadedBy(document.getUploadedBy())
            .uploadedAt(document.getUploadedDate() != null ? document.getUploadedDate() : document.getCreatedAt())
            .isRequired(false)
            .status("UPLOADED")
            .build();
    }

    private Visitor resolveAppointmentApplicant(Long applicantId, String applicantName,
                                                String applicantPhone, String epicNumber) {
        if (applicantId != null && applicantId > 0) {
            Optional<Visitor> visitor = visitorRepository.findById(applicantId);
            if (visitor.isPresent()) {
                return visitor.get();
            }
        }

        String applicantNameValue = validationService.requireText(applicantName, "applicantName");
        String applicantPhoneValue = validationService.requirePhone(applicantPhone);
        String epicFinal = epicNumber != null ? epicNumber.trim().toUpperCase() : null;

        if (epicFinal != null && !epicFinal.isEmpty() && !epicFinal.matches(ValidationConstants.REGEX_EPIC)) {
            epicFinal = null;
        }

        List<Visitor> existingVisitors = epicFinal != null
                ? visitorRepository.findByPhoneNumberAndEpicNumber(applicantPhoneValue, epicFinal)
                : visitorRepository.findByPhoneNumber(applicantPhoneValue);

        if (!existingVisitors.isEmpty()) {
            return existingVisitors.get(0);
        }
        if (epicFinal != null && visitorRepository.findByEpicNumber(epicFinal).isPresent()) {
            return visitorRepository.findByEpicNumber(epicFinal).get();
        }

        Visitor visitor = Visitor.builder()
                .fullName(applicantNameValue)
                .phoneNumber(applicantPhoneValue)
                .epicNumber(epicFinal)
                .kycType("NONE")
                .kycVerified(false)
                .kycStatus("PENDING")
                .build();
        return visitorRepository.save(visitor);
    }

    private void saveAppointmentDocuments(HttpServletRequest request, Appointment appointment,
                                          Visitor applicant, String applicationId, String uploadedBy) {
        try {
            for (Part part : request.getParts()) {
                String paramName = part.getName();
                if (!paramName.startsWith("documents_")) {
                    continue;
                }

                MultipartFile file = convertPartToMultipartFile(part);
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String documentType = paramName.replace("documents_", "");
                FileStorageService.StoredFileMetadata storedFile =
                        fileStorageService.storeFileSecure(file, applicant.getId(), applicationId);
                LocalDateTime uploadedAt = DateTimeUtil.nowIST();

                DocumentUpload docUpload = DocumentUpload.builder()
                        .appointment(appointment)
                        .visitor(applicant)
                        .documentType(documentType)
                        .originalFilename(storedFile.getOriginalFileName())
                        .storedFileName(storedFile.getStoredFileName())
                        .filePath(storedFile.getEncryptedFilePath())
                        .encryptedFilePath(storedFile.getEncryptedFilePath())
                        .secureHash(storedFile.getSecureHash())
                        .fileSizeBytes(storedFile.getFileSize())
                        .mimeType(storedFile.getContentType())
                        .contentType(storedFile.getContentType())
                        .uploadedBy(uploadedBy)
                        .uploadedDate(uploadedAt)
                        .createdAt(uploadedAt)
                        .updatedAt(uploadedAt)
                        .build();
                DocumentUpload savedDocument = documentUploadRepository.save(docUpload);
                queueAiNotes(savedDocument, uploadedBy);
            }
        } catch (ServletException | IOException e) {
            auditLogService.log("Appointment", appointment.getId(), "DOCUMENT_UPLOAD_ERROR",
                    "Failed to process appointment documents: " + e.getMessage(), uploadedBy);
            throw new IllegalArgumentException("Failed to process uploaded documents.", e);
        }
    }

    private void queueAiNotes(DocumentUpload document, String actor) {
        try {
            appointmentDocumentAiNotesService.queueGeneration(document);
        } catch (Exception e) {
            log.warn("Unable to queue AI notes requestId={} appointmentId={} documentId={}",
                    com.survisha.meghaconnect.util.RequestContextUtil.getRequestId(),
                    document.getAppointment() != null ? document.getAppointment().getId() : null,
                    document.getId(),
                    e);
            auditLogService.log("DocumentUpload", document.getId(), "AI_NOTES_QUEUE_FAILED",
                    "AI notes queue failed: " + e.getMessage(), actor);
        }
    }

    private void saveSchemeApplicationIfRequired(AppointmentMultipartRequest form, Appointment appointment,
                                                 Visitor applicant, SchemeApplication.SchemeType schemeType,
                                                 String actor) {
        if (schemeType == null) {
            return;
        }

        SchemeApplication schemeApplication = SchemeApplication.builder()
                .applicant(applicant)
                .appointment(appointment)
                .schemeType(schemeType)
                .projectName(firstNonBlank(
                        form.getProjectName(),
                        form.getAgendaBrief(),
                        formatSchemeType(schemeType) + " application"))
                .projectCategory(trimToNull(form.getProjectCategory()))
                .beneficiaryType(trimToNull(form.getBeneficiaryType()))
                .beneficiaryCount(trimToNull(form.getBeneficiaryCount()))
                .estimatedCost(parseAmount(form.getEstimatedCost()))
                .communityContribution(parseAmount(form.getCommunityContribution()))
                .justification(trimToNull(firstNonBlank(form.getJustification(), form.getAgendaBrief())))
                .status(Appointment.AppointmentStatus.SUBMITTED.name())
                .build();
        schemeApplication.setCreatedBy(actor);
        schemeApplication.setUpdatedBy(actor);
        schemeApplicationRepository.save(schemeApplication);
    }

    private WalkIn saveWalkIn(Appointment appointment, Visitor applicant, AppointmentMultipartRequest form, String actor) {
        java.time.LocalDate tokenDate = DateTimeUtil.nowIST().toLocalDate();
        String tokenNumber = walkInTokenService.nextToken(tokenDate);
        WalkIn walkIn = WalkIn.builder()
                .visitor(applicant)
                .appointment(appointment)
                .tokenNumber(tokenNumber)
                .tokenDate(tokenDate)
                .name(firstNonBlank(applicant.getFullName(), "Walk-in Visitor"))
                .mobile(applicant.getPhoneNumber())
                .idType(firstNonBlank(applicant.getKycType(), "NONE"))
                .agendaType(firstNonBlank(form.getAgendaType(), form.getRegistrationAgendaType()))
                .briefDescription(firstNonBlank(form.getAgendaBrief(), form.getRegistrationBriefDescription()))
                .createdByDeoId(actor)
                .status("CREATED")
                .createdAt(DateTimeUtil.nowIST())
                .updatedAt(DateTimeUtil.nowIST())
                .build();
        return walkInRepository.save(walkIn);
    }

    private void ensureSchemeApplicationIsNotDuplicate(Visitor applicant, SchemeApplication.SchemeType schemeType) {
        if (schemeType == null) {
            return;
        }

        schemeApplicationRepository.findByApplicant_IdAndSchemeTypeOrderByCreatedAtDesc(applicant.getId(), schemeType)
                .stream()
                .filter(this::isActiveSchemeApplication)
                .findFirst()
                .ifPresent(existing -> {
                    Appointment existingAppointment = existing.getAppointment();
                    String applicationRef = existingAppointment != null && existingAppointment.getApplicationId() != null
                            ? existingAppointment.getApplicationId()
                            : "scheme application #" + existing.getId();
                    String status = firstNonBlank(existing.getStatus(), "SUBMITTED");
                    String message = "Multiple applications for " + formatSchemeType(schemeType)
                            + " are not allowed. Existing application " + applicationRef
                            + " is currently " + status + ".";
                    throw new MeghaConnectException(ErrorCodeConstants.DUPLICATE_ENTRY, message, 409);
                });
    }

    private boolean isActiveSchemeApplication(SchemeApplication application) {
        String status = application.getStatus();
        if (status == null || status.trim().isEmpty()) {
            return true;
        }
        return !DUPLICATE_ALLOWED_FINAL_SCHEME_STATUSES.contains(status.trim().toUpperCase());
    }

    private SchemeApplication.SchemeType parseSchemeType(String schemeType) {
        String normalized = normalizeSchemeType(schemeType);
        if (normalized == null) {
            return null;
        }
        try {
            return SchemeApplication.SchemeType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.GENERAL_ERROR,
                    "Invalid scheme type: " + schemeType,
                    400
            );
        }
    }

    private String normalizeSchemeType(String schemeType) {
        if (schemeType == null || schemeType.trim().isEmpty()) {
            return null;
        }

        String normalized = schemeType.trim()
                .toUpperCase()
                .replace("&", "AND")
                .replace("+", "_PLUS")
                .replaceAll("[\\s-]+", "_")
                .replaceAll("_+", "_");

        if ("CMCARE".equals(normalized)) {
            return "CM_CARE";
        }
        if ("CMCONNECT".equals(normalized)) {
            return "CM_CONNECT";
        }
        if ("CMELEVATE".equals(normalized)) {
            return "CM_ELEVATE";
        }
        if ("FOCUSPLUS".equals(normalized) || "FOCUS_PLUS".equals(normalized)) {
            return "FOCUS_PLUS";
        }
        if ("OTHER".equals(normalized)) {
            return "OTHERS";
        }
        return normalized;
    }

    private String formatSchemeType(SchemeApplication.SchemeType schemeType) {
        if (schemeType == null) {
            return "this scheme";
        }
        return switch (schemeType) {
            case CM_CARE -> "CM Care";
            case CM_CONNECT -> "CM Connect";
            case CM_ELEVATE -> "CM Elevate";
            case FOCUS_PLUS -> "Focus+";
            case OTHERS -> "Others";
            default -> schemeType.name();
        };
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().replaceAll("[^0-9.]", "");
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String storeGuestPhotoIfPresent(String livePhotoBase64) {
        if (trimToNull(livePhotoBase64) == null) {
            return null;
        }
        try {
            return fileStorageService.storeVisitorPhotoBase64(livePhotoBase64);
        } catch (VisitorRegistrationValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.FILE_UPLOAD_FAILED,
                    "Guest photo storage failed.",
                    500,
                    e
            );
        }
    }

    private void saveAppointmentAssociates(String associates, Appointment appointment, Visitor applicant, String actor) {
        if (associates == null || associates.trim().isEmpty() || "[]".equals(associates.trim())) {
            return;
        }
        List<Map<String, Object>> associateRequests;
        try {
            associateRequests = objectMapper.readValue(associates, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            auditLogService.log("Appointment", appointment.getId(), "ASSOCIATES_PARSE_ERROR",
                    "Failed to parse associates JSON: " + e.getMessage(), actor);
            throw new MeghaConnectException(ErrorCodeConstants.INVALID_FIELD_VALUE,
                    "Associate visitors payload is invalid.", HttpStatus.BAD_REQUEST.value());
        }
        if (associateRequests == null || associateRequests.isEmpty()) {
            return;
        }

        LinkedHashSet<Long> associateCitizenIds = new LinkedHashSet<>();
        Map<Long, String> remarksByCitizenId = new HashMap<>();
        for (Map<String, Object> item : associateRequests) {
            Long citizenId = longValue(item != null ? firstNonNull(item.get("citizenId"), item.get("id")) : null);
            if (citizenId == null || citizenId <= 0) {
                throw new MeghaConnectException(ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                        "Every associate visitor must be selected from registered citizens.", HttpStatus.BAD_REQUEST.value());
            }
            if (!associateCitizenIds.add(citizenId)) {
                throw new MeghaConnectException(ErrorCodeConstants.DUPLICATE_ENTRY,
                        "Duplicate associate visitors are not allowed.", HttpStatus.CONFLICT.value());
            }
            Object remarksValue = firstNonNull(item.get("remarks"), item.get("relationship"));
            remarksByCitizenId.put(citizenId, remarksValue == null ? null : trimToNull(remarksValue.toString()));
        }

        if (applicant != null && associateCitizenIds.contains(applicant.getId())) {
            throw new MeghaConnectException(ErrorCodeConstants.INVALID_FIELD_VALUE,
                    "Primary citizen cannot be added again as an associate visitor.", HttpStatus.BAD_REQUEST.value());
        }

        List<Visitor> associateCitizens = visitorRepository.findAllById(associateCitizenIds);
        if (associateCitizens.size() != associateCitizenIds.size()) {
            throw new MeghaConnectException(ErrorCodeConstants.CONTENT_NOT_FOUND,
                    "Citizen must register in the portal before being added as an associate visitor.", HttpStatus.BAD_REQUEST.value());
        }

        for (Visitor citizen : associateCitizens) {
            if (associateMappingRepository.existsByAppointment_IdAndPerson_Id(appointment.getId(), citizen.getId())) {
                throw new MeghaConnectException(ErrorCodeConstants.DUPLICATE_ENTRY,
                        "Duplicate associate visitors are not allowed.", HttpStatus.CONFLICT.value());
            }
            String remarks = remarksByCitizenId.get(citizen.getId());
            AssociateMapping mapping = AssociateMapping.builder()
                    .appointment(appointment)
                    .person(citizen)
                    .relationship(remarks)
                    .associateName(citizen.getFullName())
                    .associatePhone(citizen.getPhoneNumber())
                    .associateEpic(citizen.getEpicNumber())
                    .associateDesignation(citizen.getDesignation())
                    .associateAddress(firstNonBlank(citizen.getFullAddress(), citizen.getAddress(), citizen.getAddressLine(), citizen.getLocation()))
                    .build();
            mapping.setCreatedBy(actor);
            mapping.setUpdatedBy(actor);
            AssociateMapping savedMapping = associateMappingRepository.save(mapping);
            auditLogService.log("Appointment", appointment.getId(), "ASSOCIATE_CITIZEN_ADDED",
                    "Associate citizen added: citizenId=" + citizen.getId() + ", mappingId=" + savedMapping.getId(), actor);
        }
        auditLogService.log("Appointment", appointment.getId(), "ASSOCIATES_SAVED",
                "Appointment created with " + associateCitizens.size() + " associate visitor(s).", actor);
    }

    private String toAssociatesJson(List<AssociateVisitorDto> associates) {
        if (associates == null || associates.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(associates);
        } catch (Exception e) {
            throw new MeghaConnectException(ErrorCodeConstants.INVALID_FIELD_VALUE,
                    "Associate visitors payload is invalid.", HttpStatus.BAD_REQUEST.value());
        }
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private MultipartFile convertPartToMultipartFile(Part part) throws IOException {
        byte[] fileContent = part.getInputStream().readAllBytes();

        return new MultipartFile() {
            @Override
            public String getName() {
                return part.getName();
            }

            @Override
            public String getOriginalFilename() {
                return part.getSubmittedFileName();
            }

            @Override
            public String getContentType() {
                return part.getContentType();
            }

            @Override
            public boolean isEmpty() {
                return fileContent.length == 0;
            }

            @Override
            public long getSize() {
                return fileContent.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return fileContent;
            }

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                return new java.io.ByteArrayInputStream(fileContent);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                java.nio.file.Files.write(dest.toPath(), fileContent);
            }

            @Override
            public void transferTo(java.nio.file.Path dest) throws IOException, IllegalStateException {
                java.nio.file.Files.write(dest, fileContent);
            }
        };
    }

    private Appointment.MeetingLocation parseMeetingLocation(String requestedLocation) {
        if (requestedLocation == null || requestedLocation.trim().isEmpty()) {
            return Appointment.MeetingLocation.OTHERS;
        }
        try {
            return Appointment.MeetingLocation.valueOf(requestedLocation.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Appointment.MeetingLocation.OTHERS;
        }
    }

    private Appointment.EventType parseEventType(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            return Appointment.EventType.A1;
        }
        try {
            return Appointment.EventType.valueOf(eventType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Appointment.EventType.A1;
        }
    }

    private String resolveAppointmentActor(String createdBy, Visitor applicant) {
        String applicantName = applicant != null ? trimToNull(applicant.getFullName()) : null;
        if (createdBy != null && createdBy.trim().startsWith("visitor_") && applicantName != null) {
            return applicantName;
        }
        if (createdBy != null && !createdBy.trim().isEmpty() && !"anonymous".equalsIgnoreCase(createdBy.trim())) {
            return createdBy.trim();
        }
        return applicantName != null ? applicantName : "visitor_" + applicant.getId();
    }

    private String generatePublicApplicationId() {
        return "MC-" + DateTimeUtil.nowIST().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
