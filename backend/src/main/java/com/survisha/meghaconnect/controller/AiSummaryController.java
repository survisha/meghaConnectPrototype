package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.repository.AppointmentRepository;
import com.survisha.meghaconnect.service.AISummaryService;
import com.survisha.meghaconnect.service.RequestValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI Document Summary endpoint.
 * Delegates to AISummaryService which is pluggable for local LLM integration.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Summary", description = "AI-powered document summarization endpoints")
public class AiSummaryController {

    private final AppointmentRepository appointmentRepository;
    private final AISummaryService aiSummaryService;
    private final RequestValidationService validationService;

    @PostMapping("/generate-summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OSD','APPROVER','CMO','CMO_OFFICER','HCM','DATA_ENTRY_OPERATOR')")
    public ResponseEntity<Map<String, Object>> generateSummary(@RequestBody Map<String, Object> request) {
        Long appointmentId = validationService.optionalLong(
                request != null ? request.get("appointmentId") : null,
                "appointmentId"
        );
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
