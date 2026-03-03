package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.entity.Appointment;
import com.survisha.meghaconnect.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI Document Summary endpoint.
 * In production this would call an LLM/NLP microservice.
 * For now it generates a concise summary from the appointment fields.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiSummaryController {

    private final AppointmentRepository appointmentRepository;

    @PostMapping("/generate-summary")
    public ResponseEntity<Map<String, Object>> generateSummary(@RequestBody Map<String, Object> request) {
        Long appointmentId = null;
        if (request.get("appointmentId") != null) {
            try {
                appointmentId = Long.parseLong(request.get("appointmentId").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid appointmentId: must be a valid numeric identifier."
                ));
            }
        }
        String agendaBrief   = String.valueOf(request.getOrDefault("agendaBrief", ""));
        String agendaType    = String.valueOf(request.getOrDefault("agendaType", ""));
        String applicantName = String.valueOf(request.getOrDefault("applicantName", ""));
        String district      = String.valueOf(request.getOrDefault("district", ""));

        // Build short summary (placeholder for actual AI inference)
        String brief = agendaBrief.length() > 120 ? agendaBrief.substring(0, 120) + "…" : agendaBrief;
        String shortNotes = String.format("%s (%s) – %s: %s", applicantName, district, agendaType, brief);

        // Persist shortNotes if appointmentId provided
        if (appointmentId != null) {
            appointmentRepository.findById(appointmentId).ifPresent(appt -> {
                appt.setShortNotes(shortNotes);
                appointmentRepository.save(appt);
            });
        }

        return ResponseEntity.ok(Map.of(
                "appointmentId", appointmentId != null ? appointmentId : 0,
                "shortNotes", shortNotes
        ));
    }
}
