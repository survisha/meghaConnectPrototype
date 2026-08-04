package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.PublicIdentificationHistoryDto;
import com.survisha.meghaconnect.entity.AssociateMapping;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.SchemeApplication;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ResourceNotFoundException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.AssociateMappingRepository;
import com.survisha.meghaconnect.repository.SchemeApplicationRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.survisha.meghaconnect.monitoring.MonitoredOperation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublicIdentificationService {

    private final VisitorRepository visitorRepository;
    private final SchemeApplicationRepository schemeApplicationRepository;
    private final AppointmentRepository appointmentRepository;
    private final AssociateMappingRepository associateMappingRepository;
    private final AuditLogService auditLogService;
    private final AppointmentService appointmentService;

    @Transactional
    @MonitoredOperation(value = "public_identification", category = MonitoredOperation.Category.DATABASE)
    public PublicIdentificationHistoryDto getCitizenFullHistory(Long citizenId, String actor) {
        Visitor citizen = visitorRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found"));

        List<SchemeApplication> schemeApplications = schemeApplicationRepository.findByApplicant_IdOrderByCreatedAtDesc(citizenId);
        List<Appointment> primaryAppointments = appointmentRepository.findByApplicant_IdOrderByCreatedAtDesc(citizenId);
        List<AssociateMapping> associateMappings = associateMappingRepository.findByPerson_IdOrderByCreatedAtDesc(citizenId);
        List<Appointment> appointments = mergeAppointments(primaryAppointments, associateMappings);

        LocalDateTime lastVisitedAt = appointments.stream()
                .filter(this::isPastVisit)
                .map(this::latestAppointmentTime)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        auditLogService.log(
                "PublicIdentification",
                citizenId,
                "PROFILE_HISTORY_VIEWED",
                "Public identification history viewed for citizen " + firstNonBlank(citizen.getFullName(), citizen.getPhoneNumber(), String.valueOf(citizenId)),
                firstNonBlank(actor, "public-identification")
        );

        return PublicIdentificationHistoryDto.builder()
                .citizenId(citizen.getId())
                .citizenName(citizen.getFullName())
                .photoUrl(toUploadUrl(firstNonBlank(citizen.getPhotoStoragePath(), citizen.getPhotoPath(), citizen.getLivePhotoPath())))
                .visitCount(appointments.size())
                .lastVisitedAt(lastVisitedAt)
                .schemes(schemeApplications.stream()
                        .sorted(Comparator.comparing(this::schemeSortTime, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(this::toSchemeHistory)
                        .toList())
                .appointments(appointments.stream()
                        .sorted(Comparator.comparing(this::latestAppointmentTime, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(appointment -> toAppointmentHistory(appointment, citizenId))
                        .toList())
                .build();
    }

    private PublicIdentificationHistoryDto.SchemeHistoryItem toSchemeHistory(SchemeApplication application) {
        return PublicIdentificationHistoryDto.SchemeHistoryItem.builder()
                .id(application.getId())
                .schemeName(formatSchemeType(application.getSchemeType()))
                .projectName(application.getProjectName())
                .appliedDate(application.getCreatedAt())
                .status(firstNonBlank(application.getStatus(), application.getHcmDecision() != null ? application.getHcmDecision().name() : null, "SUBMITTED"))
                .amount(application.getHcmApprovedCost() != null ? application.getHcmApprovedCost() : application.getEstimatedCost())
                .remarks(firstNonBlank(application.getHcmRemarks(), application.getJustification()))
                .build();
    }

    private PublicIdentificationHistoryDto.AppointmentHistoryItem toAppointmentHistory(Appointment appointment, Long citizenId) {
        boolean primary = appointment.getApplicant() != null && appointment.getApplicant().getId().equals(citizenId);
        Visitor primaryVisitor = appointment.getApplicant();
        return PublicIdentificationHistoryDto.AppointmentHistoryItem.builder()
                .appointmentId(appointment.getId())
                .applicationId(appointment.getApplicationId())
                .dateTime(latestAppointmentTime(appointment))
                .department(firstNonBlank(appointment.getDepartment(), appointment.getReferredOffice()))
                .officerName(firstNonBlank(appointment.getApprovedBy(), appointment.getSelectedForPublicDarbarBy(), appointment.getRejectedBy(), appointment.getReferredByName()))
                .purpose(firstNonBlank(appointment.getSubject(), appointment.getAgendaBrief(), appointment.getAgendaType(), appointment.getReasonForAppointment()))
                .status(appointment.getStatus() != null ? appointment.getStatus().name() : "")
                .remarks(firstNonBlank(appointment.getHcmRemarks(), appointment.getApproverRemarks(), appointment.getCmoRemarks(), appointment.getShortNotes(), appointment.getRejectionReason()))
                .role(primary ? "PRIMARY" : "ASSOCIATE")
                .primaryVisitorName(primaryVisitor != null ? primaryVisitor.getFullName() : "")
                .groupMembers(appointmentService.toAssociateDtos(appointment))
                .build();
    }

    private List<Appointment> mergeAppointments(List<Appointment> primaryAppointments, List<AssociateMapping> associateMappings) {
        Map<Long, Appointment> byId = new LinkedHashMap<>();
        for (Appointment appointment : primaryAppointments) {
            if (appointment != null && appointment.getId() != null) {
                byId.put(appointment.getId(), appointment);
            }
        }
        for (AssociateMapping mapping : associateMappings) {
            Appointment appointment = mapping.getAppointment();
            if (appointment != null && appointment.getId() != null) {
                byId.putIfAbsent(appointment.getId(), appointment);
            }
        }
        return new ArrayList<>(byId.values());
    }

    private LocalDateTime latestAppointmentTime(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        return appointment.getScheduledDateTime() != null
                ? appointment.getScheduledDateTime()
                : firstNonNull(appointment.getUpdatedAt(), appointment.getCreatedAt());
    }

    private boolean isPastVisit(Appointment appointment) {
        LocalDateTime appointmentTime = latestAppointmentTime(appointment);
        if (appointmentTime == null || appointmentTime.isAfter(LocalDateTime.now())) {
            return false;
        }
        if (appointment.getStatus() == null) {
            return true;
        }
        String status = appointment.getStatus().name();
        return List.of("COMPLETED", "VISITED", "CLOSED", "EXITED", "RESOLVED").contains(status);
    }

    private String toUploadUrl(String path) {
        String clean = firstNonBlank(path);
        if (clean.isEmpty()) {
            return "";
        }
        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("/uploads/")) {
            return clean;
        }
        return "/uploads/" + clean.replaceFirst("^/+", "");
    }

    private LocalDateTime schemeSortTime(SchemeApplication application) {
        return application == null ? null : firstNonNull(application.getUpdatedAt(), application.getCreatedAt());
    }

    private LocalDateTime firstNonNull(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String formatSchemeType(SchemeApplication.SchemeType schemeType) {
        if (schemeType == null) {
            return "Scheme";
        }
        return schemeType.name().replace('_', ' ');
    }
}
