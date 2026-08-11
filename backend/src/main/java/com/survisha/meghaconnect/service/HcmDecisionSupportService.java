package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.*;
import com.survisha.meghaconnect.entity.*;
import com.survisha.meghaconnect.exception.ResourceNotFoundException;
import com.survisha.meghaconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HcmDecisionSupportService {
    private final AppointmentRepository appointmentRepository;
    private final WalkInRepository walkInRepository;
    private final DirectionFollowUpRepository followUpRepository;
    private final DirectionFollowUpService followUpService;
    private final VisitorRepository visitorRepository;
    private final PublicIdentificationService historyService;

    public Map<String, Long> dashboardCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("pendingScheduled", countAppointments(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.PENDING));
        counts.put("scheduledUpcoming", countAppointments(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.SCHEDULED));
        counts.put("liveWalkIns", walkInRepository.countByStatus(WalkIn.WalkInStatus.PENDING));
        counts.put("completedAppointments", countAppointments(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.HCM_MET_COMPLETED)
                + walkInRepository.countByStatus(WalkIn.WalkInStatus.COMPLETED));
        counts.put("routedToOfficial", countAppointments(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.ROUTED_TO_OFFICIAL));
        counts.put("rejected", countAppointments(Appointment.AppointmentCategory.SCHEDULED, Appointment.AppointmentStatus.REJECTED));
        counts.put("openFollowUps", followUpRepository.count((root, query, cb) -> cb.notEqual(root.get("status"), DirectionFollowUp.FollowUpStatus.COMPLETED)));
        counts.put("overdueFollowUps", followUpRepository.count((root, query, cb) -> cb.and(
                cb.notEqual(root.get("status"), DirectionFollowUp.FollowUpStatus.COMPLETED),
                cb.lessThan(root.get("dueDate"), LocalDate.now()))));
        return counts;
    }

    public Map<String, Object> citizenIntelligence(Long visitorId, String actor) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor not found"));
        PublicIdentificationHistoryDto history = historyService.getCitizenFullHistory(visitorId, actor);
        List<DirectionFollowUpDto> followUps = followUpRepository.findByVisitor_IdOrderByCreatedAtDesc(visitorId)
                .stream().map(followUpService::toDto).toList();
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", visitor.getId()); profile.put("name", visitor.getFullName());
        profile.put("mobile", visitor.getPhoneNumber()); profile.put("epic", mask(visitor.getEpicNumber()));
        profile.put("constituency", visitor.getConstituency()); profile.put("district", visitor.getDistrict());
        profile.put("address", first(visitor.getFullAddress(), visitor.getAddress(), visitor.getAddressLine()));
        profile.put("gender", visitor.getGender()); profile.put("dateOfBirth", visitor.getDateOfBirth());
        profile.put("photoUrl", history.getPhotoUrl());

        long open = followUps.stream().filter(item -> "PENDING".equals(item.getStatus()) || "IN_PROGRESS".equals(item.getStatus())).count();
        long overdue = followUps.stream().filter(item -> "OVERDUE".equals(item.getStatus())).count();
        long completed = followUps.stream().filter(item -> "COMPLETED".equals(item.getStatus())).count();
        String summary = history.getVisitCount() == 0
                ? "No previous visit data is available for this citizen."
                : "Citizen has " + history.getVisitCount() + " recorded visit(s), " + open
                    + " open follow-up(s), " + overdue + " overdue follow-up(s), and " + completed
                    + " completed follow-up(s). This briefing uses stored MeghaConnect data only.";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", profile); result.put("visitHistory", history.getAppointments());
        result.put("schemeBenefits", history.getSchemes()); result.put("directionsAndFollowUps", followUps);
        result.put("followUpSummary", Map.of("open", open, "overdue", overdue, "completed", completed));
        result.put("departmentInteractions", followUps); result.put("aiSummary", summary);
        result.put("aiGenerated", false); result.put("aiProvider", "stored-data-fallback");
        return result;
    }

    private long countAppointments(Appointment.AppointmentCategory category, Appointment.AppointmentStatus status) {
        Specification<Appointment> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("appointmentCategory"), category), cb.equal(root.get("status"), status));
        return appointmentRepository.count(spec);
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() <= 4 ? normalized : "XXXX-" + normalized.substring(normalized.length() - 4);
    }
    private String first(String... values) { return Arrays.stream(values).filter(v -> v != null && !v.isBlank()).findFirst().orElse(null); }
}
