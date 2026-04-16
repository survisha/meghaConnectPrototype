package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.service.AISummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI Document Summary endpoint.
 * Delegates to AISummaryService which is pluggable for LLM/OpenAI integration.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Summary", description = "AI-powered document summarization endpoints")
public class AiSummaryController {

    private final AppointmentRepository appointmentRepository;
    private final AISummaryService aiSummaryService;

    @PostMapping("/generate-summary")
    public ResponseEntity<Map<String, Object>> generateSummary(@RequestBody Map<String, Object> request) {
        Long appointmentId = null;
        if (request.get("appointmentId") != null) {
            try {
                appointmentId = Long.parseLong(request.get("appointmentId").toString());
            } catch (NumberFormatException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid appointmentId: must be a valid numeric identifier.");
                return ResponseEntity.badRequest().body(error);
            }
        }
        String agendaBrief   = String.valueOf(request.getOrDefault("agendaBrief", ""));
        String agendaType    = String.valueOf(request.getOrDefault("agendaType", ""));
        String applicantName = String.valueOf(request.getOrDefault("applicantName", ""));
        String district      = String.valueOf(request.getOrDefault("district", ""));

        String shortNotes = aiSummaryService.generateSummaryFromText(applicantName, district, agendaType, agendaBrief);

        // Persist shortNotes if appointmentId provided
        final Long apptId = appointmentId;
        if (apptId != null) {
            appointmentRepository.findById(apptId).ifPresent(appt -> {
                appt.setShortNotes(shortNotes);
                appointmentRepository.save(appt);
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("appointmentId", appointmentId != null ? appointmentId : 0);
        response.put("shortNotes", shortNotes);
        return ResponseEntity.ok(response);
    }
}
