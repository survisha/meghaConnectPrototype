package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.PublicIdentificationHistoryDto;
import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.SchemeApplication;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.exception.ResourceNotFoundException;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.SchemeApplicationRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicIdentificationService {

    private final VisitorRepository visitorRepository;
    private final SchemeApplicationRepository schemeApplicationRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public PublicIdentificationHistoryDto getCitizenFullHistory(Long citizenId, String actor) {
        Visitor citizen = visitorRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found"));

        List<SchemeApplication> schemeApplications = schemeApplicationRepository.findByApplicant_IdOrderByCreatedAtDesc(citizenId);
        List<Appointment> appointments = appointmentRepository.findByApplicant_IdOrderByCreatedAtDesc(citizenId);

        LocalDateTime lastVisitedAt = appointments.stream()
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
                .photoUrl(firstNonBlank(citizen.getLivePhotoPath(), citizen.getPhotoStoragePath(), citizen.getPhotoPath()))
                .visitCount(appointments.size())
                .lastVisitedAt(lastVisitedAt)
                .schemes(schemeApplications.stream()
                        .sorted(Comparator.comparing(this::schemeSortTime, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(this::toSchemeHistory)
                        .toList())
                .appointments(appointments.stream()
                        .sorted(Comparator.comparing(this::latestAppointmentTime, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(this::toAppointmentHistory)
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

    private PublicIdentificationHistoryDto.AppointmentHistoryItem toAppointmentHistory(Appointment appointment) {
        return PublicIdentificationHistoryDto.AppointmentHistoryItem.builder()
                .appointmentId(appointment.getId())
                .applicationId(appointment.getApplicationId())
                .dateTime(latestAppointmentTime(appointment))
                .department(firstNonBlank(appointment.getDepartment(), appointment.getReferredOffice()))
                .officerName(firstNonBlank(appointment.getApprovedBy(), appointment.getSelectedForPublicDarbarBy(), appointment.getRejectedBy(), appointment.getReferredByName()))
                .purpose(firstNonBlank(appointment.getSubject(), appointment.getAgendaBrief(), appointment.getAgendaType(), appointment.getReasonForAppointment()))
                .status(appointment.getStatus() != null ? appointment.getStatus().name() : "")
                .remarks(firstNonBlank(appointment.getHcmRemarks(), appointment.getApproverRemarks(), appointment.getCmoRemarks(), appointment.getShortNotes(), appointment.getRejectionReason()))
                .build();
    }

    private LocalDateTime latestAppointmentTime(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        return appointment.getScheduledDateTime() != null
                ? appointment.getScheduledDateTime()
                : firstNonNull(appointment.getUpdatedAt(), appointment.getCreatedAt());
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
