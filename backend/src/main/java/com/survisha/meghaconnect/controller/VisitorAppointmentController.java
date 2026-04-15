package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.entity.Visitor;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Public-facing appointment booking endpoint for logged-in visitors (citizens).
 *
 * All paths under /api/v1/visitor/** are permitted in SecurityConfig.
 *
 * Flow:
 *   POST /api/v1/visitor/appointments  – submit a new appointment request
 *
 * The visitor must be registered and their applicantId (person.id) must be sent.
 * If no applicantId is provided (e.g. anonymous submission), a lightweight Person
 * record is created from the form data so that the appointment can be linked.
 */
@RestController
@RequestMapping("/api/v1/visitor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VisitorAppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final VisitorRepository      visitorRepository;
    private final AuditLogService       auditLogService;

    /**
     * Submit a new appointment / scheme application request from a citizen.
     *
     * Request body (JSON):
     * {
     *   "applicantId":         123,          // optional – registered person ID
     *   "applicantName":       "John Doe",
     *   "applicantPhone":      "9876543210",
     *   "epicNumber":          "ABC1234567",
     *   "agendaType":          "Scheme availment (CM)",
     *   "agendaBrief":         "Request for CMSDF road project",
     *   "requestedLocation":   "SHILLONG",
     *   "mlaMdcApproved":      true,
     *   "applicationType":     "NEW_APPLICATION",   // or "REMINDER"
     *   "schemeType":          "CMSDF",
     *   "projectName":         "Village road improvement",
     *   "projectCategory":     "Road",
     *   "beneficiaryType":     "Community/Society",
     *   "beneficiaryCount":    "101 to 500",
     *   "estimatedCost":       250000.00,
     *   "communityContribution": 25000.00,
     *   "justification":       "...",
     *   "schemeHistoryList":   ["CMSG"],
     *   "associates":          [{ "name": "Jane Doe", "phoneNumber": "...", ... }]
     * }
     */
    @PostMapping("/appointments")
    public ResponseEntity<Map<String, Object>> submitAppointment(@RequestBody Map<String, Object> body) {

        // ── Resolve applicant ───────────────────────────────────────────────
        Visitor applicant = null;

        Object applicantIdObj = body.get("applicantId");
        if (applicantIdObj != null) {
            try {
                long applicantId = Long.parseLong(applicantIdObj.toString());
                applicant = visitorRepository.findById(applicantId).orElse(null);
            } catch (NumberFormatException ignored) { }
        }

        // If no registered person found, create a lightweight one from form data
        if (applicant == null) {
            String name  = getString(body, "applicantName");
            String phone = getString(body, "applicantPhone");
            if (name == null || name.trim().isEmpty()) {
                return badRequest("applicantName is required when applicantId is not provided");
            }
            if (phone == null || !phone.matches("^[0-9]{10}$")) {
                return badRequest("applicantPhone must be a valid 10-digit number");
            }
            // Reuse existing person by phone to avoid duplicates
            applicant = visitorRepository.findByPhoneNumber(phone.trim()).orElseGet(() -> {
                String epic = getString(body, "epicNumber");
                // Validate EPIC format if provided
                if (epic != null && !epic.trim().isEmpty() && !epic.matches("^[A-Z]{3}[0-9]{7}$")) {
                    epic = null; // Ignore invalid EPIC rather than rejecting
                }
                // Check EPIC uniqueness if provided
                final String epicFinal = epic;
                if (epicFinal != null && visitorRepository.findByEpicNumber(epicFinal).isPresent()) {
                    return visitorRepository.findByEpicNumber(epicFinal).get();
                }
                Visitor p = Visitor.builder()
                        .fullName(name.trim())
                        .phoneNumber(phone.trim())
                        .epicNumber(epicFinal)
                        .kycType("NONE")
                        .kycVerified(false)
                        .kycStatus("PENDING")
                        .build();
                return visitorRepository.save(p);
            });
        }

        // ── Validate agenda ─────────────────────────────────────────────────
        String agendaType = getString(body, "agendaType");
        if (agendaType == null || agendaType.trim().isEmpty()) {
            return badRequest("agendaType is required");
        }

        // ── Resolve requestedLocation enum ──────────────────────────────────
        Appointment.MeetingLocation location;
        try {
            String locStr = getString(body, "requestedLocation");
            location = locStr != null
                    ? Appointment.MeetingLocation.valueOf(locStr.toUpperCase())
                    : Appointment.MeetingLocation.OTHERS;
        } catch (IllegalArgumentException e) {
            location = Appointment.MeetingLocation.OTHERS;
        }

        // ── Generate application ID ─────────────────────────────────────────
        String appId = "MC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // ── Build and save Appointment ──────────────────────────────────────
        int meetingCount = appointmentRepository.countMeetingsLast6Months(
                applicant.getId(), LocalDateTime.now().minusMonths(6));

        Appointment appt = Appointment.builder()
                .applicationId(appId)
                .applicant(applicant)
                .eventType(Appointment.EventType.A1)
                .agendaType(agendaType.trim())
                .agendaBrief(getString(body, "agendaBrief"))
                .status(Appointment.AppointmentStatus.SUBMITTED)
                .requestedLocation(location)
                .mlaMdcApproved(getBoolean(body, "mlaMdcApproved"))
                .isWalkIn(false)
                .meetingCountLast6Months(meetingCount)
                .build();

        // Persist AI-generated fields if included in the submission (R004–R007)
        String aiSummary = getString(body, "aiSummary");
        if (aiSummary != null && !aiSummary.trim().isEmpty()) {
            appt.setAiSummary(aiSummary.trim());
        }
        String aiPriorityLevel = getString(body, "aiPriorityLevel");
        if (aiPriorityLevel != null && !aiPriorityLevel.trim().isEmpty()) {
            appt.setAiPriorityLevel(aiPriorityLevel.trim());
        }

        Appointment saved = appointmentRepository.save(appt);
        auditLogService.log("Appointment", saved.getId(), "SUBMITTED_BY_VISITOR",
                "Visitor appointment submitted: " + appId, "visitor_" + applicant.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", saved.getId());
        response.put("applicationId", appId);
        response.put("status", "SUBMITTED");
        response.put("message", "Appointment request submitted successfully.");
        return ResponseEntity.ok(response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private static boolean getBoolean(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String)  return Boolean.parseBoolean((String) v);
        return false;
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return ResponseEntity.badRequest().body(error);
    }
}
